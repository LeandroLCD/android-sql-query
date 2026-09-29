package com.blipblipcode.query.retrofit.r8

import com.android.tools.r8.R8
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import java.util.jar.Attributes
import java.util.jar.JarFile
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Runs the real R8 shrinker/obfuscator over a minimal program made of
 * [com.blipblipcode.query.retrofit.RepeatedQueryParameters] plus a Retrofit
 * service that consumes it, and exposes the produced class files through an
 * isolated class loader.
 *
 * This is what turns "the consumer reported an @QueryMap error" into a test
 * that fails on a plain `./gradlew :query:testDebugUnitTest`, with no device and
 * no emulator involved.
 */
internal object R8TestRunner {

    const val SAMPLE_API_CLASS_NAME = "com.blipblipcode.query.retrofit.r8.R8SampleApi"

    private const val PARAMETERS_CLASS_PATH =
        "com/blipblipcode/query/retrofit/RepeatedQueryParameters.class"

    private const val PARAMETERS_PACKAGE_PATH = "com/blipblipcode/query/retrofit/"

    private const val SAMPLE_API_CLASS_PATH =
        "com/blipblipcode/query/retrofit/r8/R8SampleApi.class"

    private const val TIMEOUT_SECONDS = 300L

    private val mainClasses: ClasspathEntry by lazy { classpathEntryOf(PARAMETERS_CLASS_PATH) }

    private val testClasses: ClasspathEntry by lazy { classpathEntryOf(SAMPLE_API_CLASS_PATH) }

    /**
     * Runs R8 in release mode with [proguardRules] and returns the directory
     * holding the obfuscated class files.
     */
    fun run(proguardRules: String, outputName: String): File {
        val outputDir = Files.createTempDirectory("r8-output-$outputName").toFile()
        val rulesFile = Files.createTempFile("r8-rules-$outputName", ".pro").toFile()
        rulesFile.writeText(proguardRules)

        val command = buildList {
            add(File(File(System.getProperty("java.home"), "bin"), "java").absolutePath)
            add("-cp")
            add(r8Jar().absolutePath)
            add("com.android.tools.r8.R8")
            add("--release")
            add("--classfile")
            add("--output")
            add(outputDir.absolutePath)
            add("--lib")
            add(System.getProperty("java.home"))
            libraries().forEach {
                add("--lib")
                add(it.absolutePath)
            }
            add("--pg-conf")
            add(rulesFile.absolutePath)
            add(programJar().absolutePath)
        }

        val process = ProcessBuilder(command).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            error("R8 did not finish within $TIMEOUT_SECONDS seconds")
        }
        check(process.exitValue() == 0) { "R8 failed (exit ${process.exitValue()}):\n$output" }

        return outputDir
    }

    /**
     * Class loader that serves the R8 output and delegates everything else
     * (Retrofit, OkHttp, Kotlin stdlib, ...) to [parent].
     */
    fun classLoaderFor(outputDir: File, parent: ClassLoader): ClassLoader =
        object : ClassLoader(parent) {
            override fun loadClass(name: String, resolve: Boolean): Class<*> {
                val classFile = File(outputDir, name.replace('.', '/') + ".class")
                if (!classFile.isFile) return super.loadClass(name, resolve)
                return synchronized(this) {
                    findLoadedClass(name) ?: classFile.readBytes().let { bytes ->
                        defineClass(name, bytes, 0, bytes.size)
                    }.also { if (resolve) resolveClass(it) }
                }
            }
        }

    private fun r8Jar(): File = File(
        checkNotNull(R8::class.java.protectionDomain?.codeSource).location.toURI()
    )

    /** Jar with the classes handed to R8 as program input. */
    private fun programJar(): File {
        val entries = mainClasses
            .list(PARAMETERS_PACKAGE_PATH) { it.startsWith("RepeatedQueryParameters") }
            .associateWith { mainClasses.read(PARAMETERS_PACKAGE_PATH + it) }
            .toMutableMap()
        entries[SAMPLE_API_CLASS_PATH] = testClasses.read(SAMPLE_API_CLASS_PATH)

        val jar = Files.createTempFile("r8-program", ".jar").toFile()
        ZipOutputStream(jar.outputStream()).use { zip ->
            entries.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return jar
    }

    /**
     * Library class path given to R8: everything the unit test runs with, minus
     * the module classes that are part of the program input (R8 rejects a class
     * declared both as program and as library).
     */
    private fun libraries(): List<File> {
        val excluded = listOf(mainClasses.file.absolutePath, testClasses.file.absolutePath)
        return classpath()
            .filter { it.absolutePath !in excluded }
            .filter { it.exists() }
            .distinctBy { it.absolutePath }
    }

    private fun classpath(): List<File> =
        System.getProperty("java.class.path")
            .split(File.pathSeparator)
            .filter { it.isNotBlank() }
            .map(::File)
            .flatMap { entry -> if (entry.isFile) expandManifestClassPath(entry) else listOf(entry) }

    /** Gradle may hand a single manifest jar over to the JVM; expand its Class-Path. */
    private fun expandManifestClassPath(jar: File): List<File> {
        val classPath = runCatching {
            JarFile(jar).use { it.manifest?.mainAttributes?.getValue(Attributes.Name.CLASS_PATH) }
        }.getOrNull() ?: return listOf(jar)

        return listOf(jar) + classPath
            .split(" ")
            .filter { it.isNotBlank() }
            .map { File(URI(it)) }
    }

    private fun classpathEntryOf(relativeClassPath: String): ClasspathEntry {
        val resource = checkNotNull(R8TestRunner::class.java.classLoader?.getResource(relativeClassPath)) {
            "$relativeClassPath not found on the test classpath"
        }
        val entries = classpath()
        val file = when (resource.protocol) {
            "file" -> {
                var candidate: File? = File(resource.toURI())
                while (candidate != null && entries.none { it.absolutePath == candidate?.absolutePath }) {
                    candidate = candidate.parentFile
                }
                checkNotNull(candidate) { "Could not resolve the classpath entry of $resource" }
            }
            "jar" -> File(URI(resource.toString().removePrefix("jar:").substringBefore("!/")))
            else -> error("Unsupported classpath entry: $resource")
        }
        return ClasspathEntry(file)
    }

    /** A jar or a directory of the test class path, both readable the same way. */
    private class ClasspathEntry(val file: File) {

        fun read(relativePath: String): ByteArray = if (file.isDirectory) {
            File(file, relativePath).readBytes()
        } else {
            JarFile(file).use { jar ->
                checkNotNull(jar.getEntry(relativePath)) { "$relativePath not found in ${file.name}" }
                jar.getInputStream(jar.getEntry(relativePath)).use { it.readBytes() }
            }
        }

        fun list(relativeDirectory: String, filter: (String) -> Boolean): List<String> =
            if (file.isDirectory) {
                File(file, relativeDirectory).listFiles().orEmpty().map { it.name }.filter(filter)
            } else {
                JarFile(file).use { jar ->
                    jar.entries().asSequence()
                        .map { it.name }
                        .filter { it.startsWith(relativeDirectory) && !it.endsWith("/") }
                        .map { it.removePrefix(relativeDirectory) }
                        .filter(filter)
                        .toList()
                }
            }
    }
}
