package com.blipblipcode.query.retrofit.r8

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Call
import retrofit2.Retrofit
import java.io.File
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * Regression tests for the R8 obfuscation bug reported by consumers:
 *
 * ```
 * @QueryMap keys must be of type String: K (parameter #2)
 * for method IHivetireApi.getTireList
 * ```
 *
 * They run the real R8 shrinker/obfuscator (as a subprocess) over
 * [com.blipblipcode.query.retrofit.RepeatedQueryParameters] with the very rules
 * the AAR ships to its consumers, and then inspect and load the produced
 * bytecode through an isolated class loader.
 */
class R8ObfuscationTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `R8 keeps the String key signature with the shipped consumer rules`() {
        // Given: The library as a minified consumer sees it
        val r8Output = R8TestRunner.run(consumerRules(), "shipped")

        // When: R8 obfuscates the class used as @QueryMap
        val queryMapClass = queryMapClassFrom(r8Output)

        // Then: The generic superclass is still LinkedHashMap<String, ...>,
        // which is exactly what Retrofit validates to accept the parameter
        assertEquals(
            "R8 must keep the generic superclass of RepeatedQueryParameters",
            String::class.java,
            keyTypeOf(queryMapClass)
        )
    }

    @Test
    fun `R8 drops the String key signature without the keepattributes rules`() {
        // Given: The same program processed with the keep rules but WITHOUT
        // -keepattributes Signature, i.e. the state that reached consumers
        val r8Output = R8TestRunner.run(consumerRulesWithoutAttributes(), "no-attributes")

        // When: R8 obfuscates the class used as @QueryMap
        val queryMapClass = queryMapClassFrom(r8Output)

        // Then: The String key binding is gone, which is what made Retrofit
        // report "@QueryMap keys must be of type String: K"
        assertNotEquals(
            "This test only proves something while R8 breaks the signature without -keepattributes Signature",
            String::class.java,
            keyTypeOf(queryMapClass)
        )
    }

    @Test
    fun `Retrofit accepts the obfuscated class as a QueryMap`() {
        // Given: A Retrofit service compiled together with the query map class
        val r8Output = R8TestRunner.run(consumerRules(), "retrofit")
        val loader = R8TestRunner.classLoaderFor(r8Output, javaClass.classLoader!!)
        val apiClass = loader.loadClass(R8TestRunner.SAMPLE_API_CLASS_NAME)
        val queryMapClass = apiClass.methods.first { it.name == "search" }
            .genericParameterTypes.first() as Class<*>
        val api = Retrofit.Builder().baseUrl(server.url("/")).build().create(apiClass)
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))

        // When: Calling it with repeated parameters built by the obfuscated class
        val call = apiClass.getMethod("search", queryMapClass)
            .invoke(api, obfuscatedQueryMap(queryMapClass)) as Call<*>
        val response = call.execute()

        // Then: The call goes through and the list value is expanded into
        // repeated query parameters (the entries set does not preserve order)
        assertEquals(200, response.code())
        val path = server.takeRequest().path.orEmpty()
        assertEquals("/search", path.substringBefore("?"))
        assertEquals(setOf("status=1", "status=2"), path.substringAfter("?").split("&").toSet())
    }

    /** Type of the `@QueryMap` parameter as seen by Retrofit after obfuscation. */
    private fun queryMapClassFrom(r8Output: File): Class<*> {
        val loader = R8TestRunner.classLoaderFor(r8Output, javaClass.classLoader!!)
        val apiClass = loader.loadClass(R8TestRunner.SAMPLE_API_CLASS_NAME)
        return apiClass.methods.first { it.name == "search" }.genericParameterTypes.first() as Class<*>
    }

    /**
     * Key type bound to the map, or `null` when the generic signature was lost.
     * Mirrors what Retrofit resolves before validating `@QueryMap`.
     */
    private fun keyTypeOf(queryMapClass: Class<*>): Type? {
        val superclass = queryMapClass.genericSuperclass as? ParameterizedType ?: return null
        return superclass.actualTypeArguments.firstOrNull()
    }

    /**
     * Builds a `RepeatedQueryParameters` through the obfuscated class. The
     * factory is looked up by shape and not by name because the consumer rules
     * allow R8 to rename the members of the class.
     */
    @Suppress("UNCHECKED_CAST")
    private fun obfuscatedQueryMap(queryMapClass: Class<*>): Any {
        val pairArray = java.lang.reflect.Array.newInstance(Pair::class.java, 1) as Array<Any>
        pairArray[0] = Pair("status", listOf(1, 2))

        val factory = queryMapClass.declaredMethods.firstOrNull {
            Modifier.isStatic(it.modifiers) && it.parameterTypes.contentEquals(arrayOf(pairArray::class.java))
        } ?: error("R8 dropped the vararg factory of $queryMapClass")
        return factory.invoke(null, pairArray as Any)
    }

    /** Rules the AAR ships inside `proguard.txt`, plus what keeps the sample service. */
    private fun consumerRules(): String = """
        ${readConsumerRules()}

        -keep class com.blipblipcode.query.retrofit.r8.R8SampleApi { *; }
    """.trimIndent()

    /** Same rules, minus every `-keepattributes` line: the state before the fix. */
    private fun consumerRulesWithoutAttributes(): String = consumerRules()
        .lineSequence()
        .filterNot { it.trimStart().startsWith("-keepattributes") }
        .joinToString("\n")

    private fun readConsumerRules(): String {
        val path = System.getProperty(CONSUMER_RULES_PROPERTY)
            ?: File("consumer-rules.pro").takeIf { it.isFile }?.absolutePath
        return path
            ?.let(::File)
            ?.takeIf { it.isFile }
            ?.readText()
            ?: error(
                "consumer-rules.pro not found. Run the tests through Gradle or set " +
                    "-D$CONSUMER_RULES_PROPERTY=<path to query/consumer-rules.pro>"
            )
    }

    private companion object {
        const val CONSUMER_RULES_PROPERTY = "query.consumerRules"
    }
}
