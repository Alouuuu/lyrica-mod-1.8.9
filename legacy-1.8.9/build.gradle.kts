import org.apache.commons.lang3.SystemUtils
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// Lyrica for Forge 1.8.9 — standalone build, independent from the Fabric multi-project build
// in the repository root (see PORTING.md).

plugins {
    idea
    java
    id("gg.essential.loom") version "0.10.0.+"
    id("dev.architectury.architectury-pack200") version "0.1.3"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("org.jetbrains.kotlin.jvm") version "2.4.20"
    // Upstream Java is written for Java 21; JvmDowngrader rewrites the bytecode to Java 8 and
    // shades stubs for the Java 9-21 APIs it uses (records, List.of, String.strip, ...).
    id("xyz.wagyourtail.jvmdowngrader") version "1.3.6"
}

val baseGroup: String by project
val mcVersion: String by project
val version: String by project
val modid: String by project
val transformerFile = file("src/main/resources/accesstransformer.cfg")

// Minecraft 1.8.9 runs on Java 8. Loom launches the client with this toolchain.
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

loom {
    log4jConfigs.from(file("log4j2.xml"))
    launchConfigs {
        "client" {
            property("mixin.debug", "true")
            arg("--tweakClass", "org.spongepowered.asm.launch.MixinTweaker")
        }
    }
    runConfigs {
        "client" {
            if (SystemUtils.IS_OS_MAC_OSX) vmArgs.remove("-XstartOnFirstThread")
        }
        remove(getByName("server"))
    }
    forge {
        pack200Provider.set(dev.architectury.pack200.java.Pack200Adapter())
        mixinConfig("mixins.$modid.json")
        if (transformerFile.exists()) accessTransformer(transformerFile)
    }
    mixin {
        defaultRefmapName.set("mixins.$modid.refmap.json")
    }
}

// Modern root CAs for Java 8u51 (see LegacyTls): the JDK 17 trust store, packaged as a resource.
val modernCaCerts by tasks.registering(Copy::class) {
    from(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(17)) }
        .map { it.metadata.installationPath.file("lib/security/cacerts") })
    into(layout.buildDirectory.dir("generated/certs/assets/lyrica/certs"))
    rename { "cacerts.jks" }
}

// `-PengineOnly`: build without the Minecraft client layer, for headless Java 8 probes.
if (project.hasProperty("engineOnly")) sourceSets.main {
    java.exclude("dev/relaylyrics/client/**", "dev/relaylyrics/platform/**")
}

// Upstream lang files are 1.21 JSON (en_us.json); 1.8.9 reads key=value .lang files (en_US.lang).
val legacyLang by tasks.registering {
    val sources = rootProject.file("../lyrics-popup/src/main/resources/assets/relaylyrics/lang")
    val extra = file("src/main/lang-extra")
    val output = layout.buildDirectory.dir("generated/lang/assets/relaylyrics/lang")
    inputs.dir(sources); inputs.dir(extra); outputs.dir(output)
    doLast {
        val out = output.get().asFile.also { it.deleteRecursively(); it.mkdirs() }
        sources.listFiles { f -> f.name.endsWith(".json") }!!.forEach { json ->
            @Suppress("UNCHECKED_CAST")
            val entries = groovy.json.JsonSlurper().parse(json, "UTF-8") as Map<String, Any?>
            val base = json.name.removeSuffix(".json")                     // en_us
            val locale = base.substringBefore('_') + "_" + base.substringAfter('_').uppercase()  // en_US
            // .lang values are single-line; keep embedded line breaks as the literal "\n" escape.
            val lines = entries.map { (k, v) -> k + "=" + v.toString().replace("\r", "").replace("\n", "\\n") }.toMutableList()
            val extraFile = File(extra, "$locale.lang")
            if (extraFile.exists()) lines += extraFile.readLines(Charsets.UTF_8)
            File(out, "$locale.lang").writeText(lines.joinToString("\n") + "\n", Charsets.UTF_8)
        }
    }
}

sourceSets.main {
    resources.srcDir(files(layout.buildDirectory.dir("generated/lang")).builtBy(legacyLang))
    output.setResourcesDir(sourceSets.main.flatMap { it.java.classesDirectory })
    resources.srcDir(files(layout.buildDirectory.dir("generated/certs")).builtBy(modernCaCerts))
}

repositories {
    mavenCentral()
    maven("https://repo.spongepowered.org/maven/")
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
}

val shadowImpl: Configuration by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}

dependencies {
    minecraft("com.mojang:minecraft:1.8.9")
    mappings("de.oceanlabs.mcp:mcp_stable:22-1.8.9")
    forge("net.minecraftforge:forge:1.8.9-11.15.1.2318-1.8.9")

    shadowImpl("org.spongepowered:mixin:0.7.11-SNAPSHOT") { isTransitive = false }
    annotationProcessor("org.spongepowered:mixin:0.8.5-SNAPSHOT")

    // Lyrics engine runtime (shaded + relocated; 1.8.9 ships Gson 2.2.4 / Guava 17 on the classpath).
    shadowImpl("org.jetbrains.kotlin:kotlin-stdlib:2.4.20")
    shadowImpl("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    shadowImpl("com.google.code.gson:gson:2.14.0")
    shadowImpl("com.squareup.okhttp3:okhttp-jvm:5.5.0")
    shadowImpl("com.squareup.retrofit2:retrofit:3.0.0") { exclude(group = "com.squareup.okhttp3") }
    shadowImpl("com.squareup.retrofit2:converter-gson:3.0.0") { exclude(group = "com.google.code.gson") }
    shadowImpl("javax.inject:javax.inject:1")
    shadowImpl("com.atilika.kuromoji:kuromoji-ipadic:0.9.0")
    shadowImpl("net.sf.kxml:kxml2:2.3.0")
    shadowImpl("com.fasterxml.jackson.core:jackson-databind:2.13.5")
    shadowImpl("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.13.5")

    testImplementation("junit:junit:4.13.2")

    runtimeOnly("me.djtheredstoner:DevAuth-forge-legacy:1.2.1")
}

// Java: compiled as Java 21 (like upstream); downgraded to Java 8 bytecode after shading.
tasks.named<JavaCompile>("compileJava") {
    javaCompiler.set(javaToolchains.compilerFor { languageVersion.set(JavaLanguageVersion.of(21)) })
    sourceCompatibility = "21"
    options.compilerArgs.addAll(listOf("-Xmaxerrs", "5000"))
    targetCompatibility = "21"
}
tasks.withType<JavaCompile>().configureEach { options.encoding = "UTF-8" }

// Kotlin: compiler runs on JDK 17, emits Java 8 bytecode and links against the Java 8 API.
tasks.withType<KotlinCompile>().configureEach {
    kotlinJavaToolchain.toolchain.use(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(17)) })
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
        freeCompilerArgs.addAll("-Xjdk-release=1.8", "-Xjvm-default=all")
    }
}

tasks.withType(org.gradle.jvm.tasks.Jar::class) {
    archiveBaseName.set(modid)
    manifest.attributes.run {
        this["FMLCorePluginContainsFMLMod"] = "true"
        this["ForceLoadAsMod"] = "true"
        this["TweakClass"] = "org.spongepowered.asm.launch.MixinTweaker"
        this["MixinConfigs"] = "mixins.$modid.json"
        if (transformerFile.exists()) this["FMLAT"] = "${modid}_at.cfg"
    }
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("mcversion", mcVersion)
    inputs.property("modid", modid)
    inputs.property("basePackage", baseGroup)
    filesMatching(listOf("mcmod.info", "mixins.$modid.json")) { expand(inputs.properties) }
    rename("accesstransformer.cfg", "META-INF/${modid}_at.cfg")
    from(rootProject.file("../LICENSE")) { rename { "LICENSE_lyrica" } }
    // Upstream assets (same layout as lyrics-popup's processResources). 1.21-only JSON font
    // providers, post-effect shaders and the Relay phone textures are not used on 1.8.9.
    from(rootProject.file("../lyrics-popup/src/main/resources/assets")) {
        into("assets")
        exclude("**/lang/**", "relaylyrics/font/*.json")
    }
    from(rootProject.file("../mod/src/main/resources/assets/lyrica/textures/gui/lyrics")) {
        into("assets/lyrica/textures/gui/lyrics")
    }
    from(rootProject.file("../lyrics-popup/src/main/resources/icon.png"))
    from(rootProject.file("../THIRD_PARTY_NOTICES.md"))
}

jvmdg {
    downgradeTo = JavaVersion.VERSION_1_8
}

// Only our own classes need downgrading (all bundled libraries are already Java 8):
// jar (Java 21) -> downgradeJar (Java 8) -> shadeDowngradedApi (+ API stubs) -> lyricaShadowJar (+ deps, relocated) -> remapJar
val downgradeJar = tasks.named<xyz.wagyourtail.jvmdg.gradle.task.DowngradeJar>("downgradeJar") {
    inputFile.set(tasks.jar.flatMap { it.archiveFile })
    destinationDirectory.set(layout.buildDirectory.dir("intermediates"))
    archiveClassifier.set("downgraded")
}
val shadeDowngradedApi = tasks.named<xyz.wagyourtail.jvmdg.gradle.task.ShadeJar>("shadeDowngradedApi") {
    inputFile.set(downgradeJar.flatMap { it.archiveFile })
    destinationDirectory.set(layout.buildDirectory.dir("intermediates"))
    archiveClassifier.set("downgraded-shaded")
    shadePath = { "$baseGroup.deps.jvmdg".replace('.', '/') + "/" }
}

tasks.shadowJar { enabled = false }
val lyricaShadowJar by tasks.registering(com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
    dependsOn(shadeDowngradedApi)
    destinationDirectory.set(layout.buildDirectory.dir("intermediates"))
    archiveBaseName.set(modid)
    archiveClassifier.set("non-obfuscated-with-deps")
    from(shadeDowngradedApi.flatMap { it.archiveFile }.map { zipTree(it) })
    configurations = listOf(shadowImpl)
    manifest.inheritFrom(tasks.jar.get().manifest)
    exclude("META-INF/*.SF", "META-INF/*.RSA", "META-INF/*.DSA", "META-INF/versions/**", "module-info.class")
    mergeServiceFiles()

    fun relocate(name: String) = relocate(name, "$baseGroup.deps.$name")
    relocate("kotlin")
    relocate("kotlinx")
    relocate("_COROUTINE", "dev.relaylyrics.deps._COROUTINE")
    relocate("org.intellij.lang.annotations")
    relocate("org.jetbrains.annotations")
    relocate("okhttp3")
    relocate("okio")
    relocate("retrofit2")
    relocate("com.google.gson")
    relocate("com.fasterxml.jackson")
    relocate("com.atilika")
    relocate("org.kxml2")
    relocate("org.xmlpull")
    relocate("javax.inject")
    relocate("com.google.errorprone")
    relocate("org.codehaus.mojo.animal_sniffer")
}

val remapJar by tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar") {
    archiveClassifier.set("")
    dependsOn(lyricaShadowJar)
    input.set(lyricaShadowJar.flatMap { it.archiveFile })
}

// Dev runs: build/classes is Java 21 bytecode, but the 1.8.9 client runs on Java 8. `runLegacyClient`
// copies Loom's client run configuration but runs from the downgraded + relocated jar instead of the
// raw class/resource dirs and unshaded libraries. (Loom's RunGameTask itself cannot be reconfigured
// under Gradle 8 validation.)
// FML only discovers @Mod on classpath *directories* (tweaker jars on the classpath are skipped),
// so the dev client runs from the downgraded+shaded jar unpacked into a folder, like loom's output dirs.
val devModDir by tasks.registering(Sync::class) {
    from(lyricaShadowJar.map { zipTree(it.archiveFile) })
    into(layout.buildDirectory.dir("devmod"))
}

val runLegacyClient by tasks.registering(JavaExec::class) {
    group = "fabric"
    dependsOn(devModDir, "downloadAssets")
    val modJar = layout.buildDirectory.dir("devmod")
    val mainOutput = sourceSets.main.get().output
    val runtime = sourceSets.main.get().runtimeClasspath
    val launchConfig = file(".gradle/loom-cache/launch.cfg")
    val runDir = file("run")
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(8)) })
    mainClass.set("net.fabricmc.devlaunchinjector.Main")
    workingDir = runDir
    jvmArgs("-Xmx3G",
        "-Dfabric.dli.config=" + launchConfig.absolutePath,
        "-Dfabric.dli.env=client",
        "-Dfabric.dli.main=net.minecraft.launchwrapper.Launch",
        "-Dlyrica.smoke=" + (project.findProperty("smoke") ?: "false"))
    // -PsoftGL: allow LWJGL's software OpenGL (headless/remote sessions without an accelerated pixel format).
    if (project.hasProperty("softGL")) jvmArgs("-Dorg.lwjgl.opengl.Display.allowSoftwareOpenGL=true")
    doFirst {
        runDir.mkdirs()
        // Our libraries are inside modJar (relocated). Keep the ones Minecraft itself needs:
        // Gradle unified MC's Gson 2.2.4 with ours into a single artifact.
        val keepGroups = setOf("com.google.code.gson")
        val ours = shadowImpl.resolvedConfiguration.resolvedArtifacts
            .filter { it.moduleVersion.id.group !in keepGroups }.map { it.file }.toSet()
        val excluded = mainOutput.files + ours
        classpath = files(modJar) + runtime.filter { it !in excluded }
    }
}

tasks.jar {
    archiveClassifier.set("without-deps")
    destinationDirectory.set(layout.buildDirectory.dir("intermediates"))
}

tasks.assemble.get().dependsOn(tasks.remapJar)
