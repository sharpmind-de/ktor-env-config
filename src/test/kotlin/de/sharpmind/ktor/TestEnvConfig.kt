package de.sharpmind.ktor

import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import kotlin.test.*

/**
 * Test env config
 *
 * These test methods have to run in a specific order.
 * Therefore NAME_ASCENDING is configured for method sorting.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class TestEnvConfig {

    /**
     * Make sure exception is thrown if envconfig is not yet initialized
     */
    @Test
    fun _TestUninitializedConfig_getA() {
        println("Running test: " + object {}.javaClass.enclosingMethod.name)

        assertFails { EnvConfig.getBoolean("a") }
        assertFails { EnvConfig.getInt("a") }
        assertFails { EnvConfig.getList("a") }
        assertFails { EnvConfig.getString("a") }
    }


    @Test
    fun testConfigA_fallback() {
        println("Running test: " + object {}.javaClass.enclosingMethod.name)

        assertEquals(3, EnvConfig.initConfig(testConfigA).getList("b").size)
        assertEquals(5, EnvConfig.initConfig(testConfigA).getInt("c"))
    }

    @Test
    fun testConfigB_fallback() {
        println("Running test: " + object {}.javaClass.enclosingMethod.name)

        assertTrue(EnvConfig.initConfig(testConfigB).getBoolean("b"))
        assertFails { EnvConfig.initConfig(testConfigB).getInt("b") }
        assertFails { EnvConfig.initConfig(testConfigB).getList("b") }
        assertEquals("True", EnvConfig.initConfig(testConfigB).getString("b"))
    }

    @Test
    fun testConfig_withDefaultValues() {
        println("Running test: " + object {}.javaClass.enclosingMethod.name)

        assertEquals(3, EnvConfig.initConfig(testConfigC).getIntOrDefault("a", 3))
        assertEquals(true, EnvConfig.initConfig(testConfigB).getBooleanOrDefault("c", true))
        assertNotEquals("3", EnvConfig.initConfig(testConfigA).getStringOrDefault("a", "3"))
        assertEquals(emptyList(), EnvConfig.initConfig(testConfigA).getListOrDefault("f", emptyList()))
    }

    @Test
    fun testConfigA_getA() {
        println("Running test: " + object {}.javaClass.enclosingMethod.name)

        assertFalse(EnvConfig.initConfig(testConfigA).getBoolean("a"))
        assertEquals(2, EnvConfig.initConfig(testConfigA).getInt("a"))
        assertFails { EnvConfig.initConfig(testConfigA).getList("a") }
        assertEquals("2", EnvConfig.initConfig(testConfigA).getString("a"))
    }

    @Test
    fun testConfigA_getB() {
        println("Running test: " + object {}.javaClass.enclosingMethod.name)

        assertFails { EnvConfig.initConfig(testConfigA).getBoolean("b") }
        assertFails { EnvConfig.initConfig(testConfigA).getInt("b") }
        assertEquals(3, EnvConfig.initConfig(testConfigA).getList("b").size)
        assertEquals("baz", EnvConfig.initConfig(testConfigA).getList("b").get(2))
        assertFails { EnvConfig.initConfig(testConfigA).getString("b") }
    }

    @Test
    fun testConfigA_getC() {
        println("Running test: " + object {}.javaClass.enclosingMethod.name)

        assertFalse(EnvConfig.initConfig(testConfigA).getBoolean("c"))
        assertEquals(5, EnvConfig.initConfig(testConfigA).getInt("c"))
        assertFails { EnvConfig.initConfig(testConfigA).getList("c") }
        assertEquals("5", EnvConfig.initConfig(testConfigA).getString("c"))
    }

    @Test
    fun testConfigB_getA() {
        println("Running test: " + object {}.javaClass.enclosingMethod.name)

        assertFalse(EnvConfig.initConfig(testConfigB).getBoolean("a"))
        assertEquals(1, EnvConfig.initConfig(testConfigB).getInt("a"))
        assertFails { EnvConfig.initConfig(testConfigB).getList("a") }
        assertEquals("1", EnvConfig.initConfig(testConfigB).getString("a"))
    }

    @Test
    fun testConfigB_getB() {
        println("Running test: " + object {}.javaClass.enclosingMethod.name)

        assertTrue(EnvConfig.initConfig(testConfigB).getBoolean("b"))
        assertFails { EnvConfig.initConfig(testConfigB).getInt("b") }
        assertFails { EnvConfig.initConfig(testConfigB).getList("b") }
        assertEquals("True", EnvConfig.initConfig(testConfigB).getString("b"))
    }

    @Test
    fun testConfigC_getA() {
        println("Running test: " + object {}.javaClass.enclosingMethod.name)

        assertFails { EnvConfig.initConfig(testConfigC).getBoolean("a") }
        assertFails { EnvConfig.initConfig(testConfigC).getInt("a") }
        assertFails { EnvConfig.initConfig(testConfigC).getList("a") }
        assertFails { EnvConfig.initConfig(testConfigC).getString("a") }
    }

    @Test
    fun testConfigA_getAOrNull() {
        println("Running test: " + object {}.javaClass.enclosingMethod.name)

        assertFalse(EnvConfig.initConfig(testConfigA).getBooleanOrNull("a")!!)
        assertEquals(2, EnvConfig.initConfig(testConfigA).getIntOrNull("a"))
        assertFails { EnvConfig.initConfig(testConfigA).getListOrNull("a") }
        assertEquals("2", EnvConfig.initConfig(testConfigA).getStringOrNull("a"))
    }

    @Test
    fun testConfigA_getBOrNull() {
        println("Running test: " + object {}.javaClass.enclosingMethod.name)

        assertFails { EnvConfig.initConfig(testConfigA).getBooleanOrNull("b") }
        assertFails { EnvConfig.initConfig(testConfigA).getIntOrNull("b") }
        assertEquals(3, EnvConfig.initConfig(testConfigA).getListOrNull("b")!!.size)
        assertEquals("baz", EnvConfig.initConfig(testConfigA).getListOrNull("b")!!.get(2))
        assertFails { EnvConfig.initConfig(testConfigA).getStringOrNull("b") }
    }

    @Test
    fun testConfigA_getCOrNull() {
        println("Running test: " + object {}.javaClass.enclosingMethod.name)

        assertFalse(EnvConfig.initConfig(testConfigA).getBooleanOrNull("c")!!)
        assertEquals(5, EnvConfig.initConfig(testConfigA).getIntOrNull("c"))
        assertFails { EnvConfig.initConfig(testConfigA).getListOrNull("c") }
        assertEquals("5", EnvConfig.initConfig(testConfigA).getStringOrNull("c"))
    }

    @Test
    fun testConfigB_getAOrNull() {
        println("Running test: " + object {}.javaClass.enclosingMethod.name)

        assertFalse(EnvConfig.initConfig(testConfigB).getBooleanOrNull("a")!!)
        assertEquals(1, EnvConfig.initConfig(testConfigB).getIntOrNull("a"))
        assertFails { EnvConfig.initConfig(testConfigB).getListOrNull("a") }
        assertEquals("1", EnvConfig.initConfig(testConfigB).getStringOrNull("a"))
    }

    @Test
    fun testConfigB_getBOrNull() {
        println("Running test: " + object {}.javaClass.enclosingMethod.name)

        assertTrue(EnvConfig.initConfig(testConfigB).getBooleanOrNull("b")!!)
        assertFails { EnvConfig.initConfig(testConfigB).getIntOrNull("b") }
        assertFails { EnvConfig.initConfig(testConfigB).getListOrNull("b") }
        assertEquals("True", EnvConfig.initConfig(testConfigB).getStringOrNull("b"))
    }

    @Test
    fun testConfigC_getAOrNull() {
        println("Running test: " + object {}.javaClass.enclosingMethod.name)

        assertNull(EnvConfig.initConfig(testConfigC).getBooleanOrNull("a"))
        assertNull(EnvConfig.initConfig(testConfigC).getIntOrNull("a"))
        assertNull(EnvConfig.initConfig(testConfigC).getListOrNull("a"))
        assertNull(EnvConfig.initConfig(testConfigC).getStringOrNull("a"))
    }

    @Test
    fun testConfigC_getD() {
        println("Running test: " + object {}.javaClass.enclosingMethod.name)
        assertEquals(EnvConfig.initConfig(testConfigD).getEnvironment(), "testEnv")
        assertEquals(EnvConfig.initConfig(testConfigD).getString("configFile"), "path_to_the_external_file.conf")
    }
}
