package dev.relaylyrics.compat;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedConstructor;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.AnnotatedParameter;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 1.8.9 port: JvmDowngrader turns {@code record}s into plain classes extending its {@code J_L_Record}
 * stub, and Jackson 2.13 on Java 8 cannot see them as records ({@code Class.isRecord} does not exist).
 * This introspector restores record semantics: the canonical constructor becomes a properties-based
 * creator (names = component fields) and the {@code x()} accessors become the serialized properties.
 * Explicit Jackson annotations still win.
 */
public final class LegacyRecords extends JacksonAnnotationIntrospector {
    private static final long serialVersionUID = 1L;
    private static final ConcurrentHashMap<Class<?>, Info> CACHE = new ConcurrentHashMap<>();
    private static final Info NONE = new Info(new String[0], null, false);

    private static final class Info {
        final String[] names;
        final Constructor<?> canonical;
        final boolean explicitCreator;
        Info(String[] names, Constructor<?> canonical, boolean explicitCreator) {
            this.names = names; this.canonical = canonical; this.explicitCreator = explicitCreator;
        }
    }

    public static boolean isRecord(Class<?> type) {
        Class<?> parent = type == null ? null : type.getSuperclass();
        if (parent == null) return false;
        String name = parent.getName();
        return name.equals("java.lang.Record") || name.endsWith(".J_L_Record");
    }

    private static Info info(Class<?> type) {
        if (!isRecord(type)) return NONE;
        return CACHE.computeIfAbsent(type, LegacyRecords::inspect);
    }

    private static Info inspect(Class<?> type) {
        List<Field> components = new ArrayList<>();
        for (Field field : type.getDeclaredFields())
            if (!Modifier.isStatic(field.getModifiers())) components.add(field);
        Class<?>[] types = new Class<?>[components.size()];
        String[] names = new String[components.size()];
        for (int i = 0; i < types.length; i++) { types[i] = components.get(i).getType(); names[i] = components.get(i).getName(); }
        Constructor<?> canonical = null;
        boolean explicit = false;
        for (Constructor<?> constructor : type.getDeclaredConstructors()) {
            if (constructor.isAnnotationPresent(JsonCreator.class)) explicit = true;
            if (Arrays.equals(constructor.getParameterTypes(), types)) canonical = constructor;
        }
        for (Method method : type.getDeclaredMethods())
            if (Modifier.isStatic(method.getModifiers()) && method.isAnnotationPresent(JsonCreator.class)) explicit = true;
        return new Info(names, canonical, explicit);
    }

    @Override
    public JsonCreator.Mode findCreatorAnnotation(MapperConfig<?> config, Annotated annotated) {
        JsonCreator.Mode explicit = super.findCreatorAnnotation(config, annotated);
        if (explicit != null || !(annotated instanceof AnnotatedConstructor)) return explicit;
        AnnotatedConstructor constructor = (AnnotatedConstructor) annotated;
        Info info = info(constructor.getDeclaringClass());
        if (info.explicitCreator || info.canonical == null) return null;
        return info.canonical.equals(constructor.getAnnotated()) ? JsonCreator.Mode.PROPERTIES : null;
    }

    @Override
    public String findImplicitPropertyName(AnnotatedMember member) {
        String name = super.findImplicitPropertyName(member);
        if (name != null) return name;
        Info info = info(member.getDeclaringClass());
        if (info == NONE) return null;
        if (member instanceof AnnotatedParameter) {
            AnnotatedParameter parameter = (AnnotatedParameter) member;
            if (info.canonical != null && info.canonical.equals(parameter.getOwner().getMember())
                    && parameter.getIndex() < info.names.length)
                return info.names[parameter.getIndex()];
            return null;
        }
        if (member instanceof AnnotatedMethod) {
            AnnotatedMethod method = (AnnotatedMethod) member;
            if (method.getParameterCount() == 0 && !Modifier.isStatic(method.getModifiers()))
                for (String component : info.names) if (component.equals(method.getName())) return component;
        }
        return null;
    }
}
