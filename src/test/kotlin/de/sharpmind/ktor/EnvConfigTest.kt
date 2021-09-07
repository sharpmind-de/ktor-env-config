package de.sharpmind.ktor

import com.typesafe.config.ConfigFactory
import io.ktor.config.HoconApplicationConfig
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
class EnvConfigTest {

    /**
     * Uninitialized tests have to come first
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
    fun testConfigA_getA() {
        println("Running test: " + object {}.javaClass.enclosingMethod.name)

        assertFalse(EnvConfig.initConfig(testConfigA).getBoolean("a")!!)
        assertEquals(2, EnvConfig.initConfig(testConfigA).getInt("a"))
        assertFails { EnvConfig.initConfig(testConfigA).getList("a") }
        assertEquals("2", EnvConfig.initConfig(testConfigA).getString("a"))
    }

    @Test
    fun testConfigA_getB() {
        println("Running test: " + object {}.javaClass.enclosingMethod.name)

        assertFails { EnvConfig.initConfig(testConfigA).getBoolean("b") }
        assertFails { EnvConfig.initConfig(testConfigA).getInt("b") }
        assertEquals(3, EnvConfig.initConfig(testConfigA).getList("b")!!.size)
        assertEquals("baz", EnvConfig.initConfig(testConfigA).getList("b")!!.get(2))
        assertFails { EnvConfig.initConfig(testConfigA).getString("b") }
    }

    @Test
    fun testConfigA_getC() {
        println("Running test: " + object {}.javaClass.enclosingMethod.name)

        assertFalse(EnvConfig.initConfig(testConfigA).getBoolean("c")!!)
        assertEquals(5, EnvConfig.initConfig(testConfigA).getInt("c"))
        assertFails { EnvConfig.initConfig(testConfigA).getList("c") }
        assertEquals("5", EnvConfig.initConfig(testConfigA).getString("c"))
    }

    @Test
    fun testConfigB_getA() {
        println("Running test: " + object {}.javaClass.enclosingMethod.name)

        assertFalse(EnvConfig.initConfig(testConfigB).getBoolean("a")!!)
        assertEquals(1, EnvConfig.initConfig(testConfigB).getInt("a"))
        assertFails { EnvConfig.initConfig(testConfigB).getList("a") }
        assertEquals("1", EnvConfig.initConfig(testConfigB).getString("a"))
    }

    @Test
    fun testConfigB_getB() {
        println("Running test: " + object {}.javaClass.enclosingMethod.name)

        assertTrue(EnvConfig.initConfig(testConfigB).getBoolean("b")!!)
        assertFails { EnvConfig.initConfig(testConfigB).getInt("b")!! }
        assertFails { EnvConfig.initConfig(testConfigB).getList("b")!! }
        assertEquals("True", EnvConfig.initConfig(testConfigB).getString("b")!!)
    }

    @Test
    fun testConfigC_getA() {
        println("Running test: " + object {}.javaClass.enclosingMethod.name)

        assertNull(EnvConfig.initConfig(testConfigC).getBoolean("a"))
        assertNull(EnvConfig.initConfig(testConfigC).getInt("a"))
        assertNull(EnvConfig.initConfig(testConfigC).getList("a"))
        assertNull(EnvConfig.initConfig(testConfigC).getString("a"))
    }

    @Test
    fun testConfig_withDefaultValues() {
        println("Running test: " + object {}.javaClass.enclosingMethod.name)

        assertNull(EnvConfig.initConfig(testConfigA).getInt("av"))
        assertEquals(3, EnvConfig.initConfig(testConfigC).getInt("a", 3))
        assertEquals(true, EnvConfig.initConfig(testConfigB).getBoolean("c", true))
        assertNotEquals("3", EnvConfig.initConfig(testConfigA).getString("a", "3"))
        assertEquals(emptyList(), EnvConfig.initConfig(testConfigA).getList("f", emptyList()))
    }

    companion object {
        // TEST CONFIG A
        private val testConfigA = HoconApplicationConfig(
            ConfigFactory.parseString(
                """
        envConfig {
            env = testEnv
    
            default {
                a = 1
                b = ["foo", "bar", "baz"]
                c = 5
            }
    
            testEnv {
                a = 2
            }
        }
        """.trimIndent()
            )
        )

        // TEST CONFIG B
        private val testConfigB = HoconApplicationConfig(
            ConfigFactory.parseString(
                """
        envConfig {
            env = doesNotExist
    
            default {
                a = 1
                b = True
            }
    
            testEnv {
                a = 2
                b = false
            }
        }
        """.trimIndent()
            )
        )

        // TEST CONFIG C
        private val testConfigC = HoconApplicationConfig(ConfigFactory.parseString(""))
    }
}
