package de.sharpmind.ktor

import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import java.io.File
import java.net.URL
import kotlin.test.*

/**
 * Test env config
 *
 * These test methods have to run in a specific order.
 * Therefore NAME_ASCENDING is configured for method sorting.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class TestEnvConfigDump {

    @Test
    fun test_dump() {
        println("Running test: " + object {}.javaClass.enclosingMethod.name)

        EnvConfig.initConfig(testConfigE)

        EnvConfig.dumpConfig()
        assertEquals(1, EnvConfig.getInt("a"))

    }

    @Test
    fun test_init_verbose() {
        println("Running test: " + object {}.javaClass.enclosingMethod.name)

        EnvConfig.initConfig(testConfigE, true)
    }
}
