package dev.relaylyrics.compat;

import okhttp3.OkHttpClient;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Minecraft 1.8.9 usually runs on the launcher's Java 8u51, whose trust store predates
 * Let's Encrypt (ISRG Root X1/X2), Google Trust Services and other current roots.
 * This trusts the JVM's own roots plus a modern root bundle generated at build time
 * ({@code /assets/lyrica/certs/cacerts.jks}), without touching JVM-wide defaults.
 */
public final class LegacyTls {
    private static final String BUNDLE = "/assets/lyrica/certs/cacerts.jks";
    private static volatile Holder holder;

    private LegacyTls() { }

    /** Applies the combined trust store to an OkHttp builder. */
    public static OkHttpClient.Builder apply(OkHttpClient.Builder builder) {
        Holder h = holder();
        if (h != null) builder.sslSocketFactory(h.factory, h.trustManager);
        return builder;
    }

    /**
     * Makes the combined trust store the JVM default, so HttpsURLConnection and the
     * java.net.http stubs used by the ported client code also accept current CAs. This only
     * adds trust anchors on top of the JVM's own.
     */
    public static synchronized boolean installJvmDefault() {
        Holder h = holder();
        if (h == null) return false;
        SSLContext.setDefault(h.context);
        javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(h.factory);
        return true;
    }

    public static OkHttpClient.Builder newBuilder() {
        return apply(new OkHttpClient.Builder());
    }

    private static Holder holder() {
        Holder h = holder;
        if (h == null) {
            synchronized (LegacyTls.class) {
                h = holder;
                if (h == null) holder = h = create();
            }
        }
        return h == Holder.NONE ? null : h;
    }

    private static Holder create() {
        try {
            // Modern bundle first; the JVM's own store (possibly old or empty) second.
            List<X509TrustManager> managers = new ArrayList<>();
            try (InputStream in = LegacyTls.class.getResourceAsStream(BUNDLE)) {
                if (in != null) managers.add(trustManager(load(readAll(in))));
            }
            try { managers.add(trustManager(null)); }
            catch (Exception unusableJvmStore) { /* e.g. empty trust store: rely on the bundle */ }
            if (managers.isEmpty()) return Holder.NONE;
            X509TrustManager combined = new CompositeTrustManager(managers);
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new TrustManager[]{combined}, null);
            return new Holder(context, context.getSocketFactory(), combined);
        } catch (Exception failure) {
            System.err.println("[Lyrica] Could not build modern trust store, using JVM defaults: " + failure);
            return Holder.NONE;
        }
    }

    private static KeyStore load(byte[] bytes) throws Exception {
        Exception failure = null;
        for (String type : new String[]{"JKS", "PKCS12"}) {
            try {
                KeyStore store = KeyStore.getInstance(type);
                store.load(new java.io.ByteArrayInputStream(bytes), "changeit".toCharArray());
                return store;
            } catch (Exception attempt) { failure = attempt; }
        }
        throw failure;
    }

    private static byte[] readAll(InputStream in) throws java.io.IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        for (int read; (read = in.read(buffer)) > 0; ) out.write(buffer, 0, read);
        return out.toByteArray();
    }

    private static X509TrustManager trustManager(KeyStore store) throws Exception {
        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init(store);
        for (TrustManager manager : factory.getTrustManagers())
            if (manager instanceof X509TrustManager x509) return x509;
        throw new IllegalStateException("No X509TrustManager");
    }

    private static final class Holder {
        static final Holder NONE = new Holder(null, null, null);
        final SSLContext context;
        final SSLSocketFactory factory;
        final X509TrustManager trustManager;
        Holder(SSLContext context, SSLSocketFactory factory, X509TrustManager trustManager) {
            this.context = context;
            this.factory = factory;
            this.trustManager = trustManager;
        }
    }

    private static final class CompositeTrustManager implements X509TrustManager {
        private final List<X509TrustManager> delegates;
        CompositeTrustManager(List<X509TrustManager> delegates) { this.delegates = delegates; }

        @Override public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            CertificateException last = null;
            for (X509TrustManager delegate : delegates) {
                try { delegate.checkClientTrusted(chain, authType); return; }
                catch (CertificateException failure) { last = failure; }
                catch (RuntimeException failure) { last = new CertificateException(failure); }
            }
            throw last != null ? last : new CertificateException("No trust managers");
        }

        @Override public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            CertificateException last = null;
            for (X509TrustManager delegate : delegates) {
                try { delegate.checkServerTrusted(chain, authType); return; }
                catch (CertificateException failure) { last = failure; }
                catch (RuntimeException failure) { last = new CertificateException(failure); }
            }
            throw last != null ? last : new CertificateException("No trust managers");
        }

        @Override public X509Certificate[] getAcceptedIssuers() {
            List<X509Certificate> all = new ArrayList<>();
            for (X509TrustManager delegate : delegates) all.addAll(Arrays.asList(delegate.getAcceptedIssuers()));
            return all.toArray(new X509Certificate[0]);
        }
    }
}
