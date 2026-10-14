package de.sharpmind.ktor

import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import kotlin.test.*

/**
 * Tests for EnvConfig.getMergedConfig()
 *
 * These test methods have to run in a specific order.
 * Therefore NAME_ASCENDING is configured for method sorting.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class TestEnvConfigMerged {

    @Test
    fun test_merged_default_and_environment() {
        println("Running test: " + object {}.javaClass.enclosingMethod.name)

        val merged = EnvConfig.initConfig(testConfigA).getMergedConfig()

        // environment block overrides default
        assertEquals("2", merged["a"])
        // default-only key is present
        assertEquals("5", merged["c"])
        // list key from default block is present
        assertTrue(merged.containsKey("b"))
        // control keys are not part of the merged properties
        assertNull(merged["env"])
        assertNull(merged["externalConfigFile"])
    }

    @Test
    fun test_merged_external_config_wins() {
        println("Running test: " + object {}.javaClass.enclosingMethod.name)

        val merged = EnvConfig.initConfig(testConfigD).getMergedConfig()

        // external config file has highest priority (over env=2 and default=1)
        assertEquals("3", merged["a"])
        // external config file has highest priority (over env/default=false)
        assertEquals("true", merged["b"])
    }

    @Test
    fun test_merged_nonexistent_environment_uses_default() {
        println("Running test: " + object {}.javaClass.enclosingMethod.name)

        val merged = EnvConfig.initConfig(testConfigB).getMergedConfig()

        assertEquals("1", merged["a"])
        assertEquals("True", merged["b"])
    }

    @Test
    fun test_merged_deepMergesNestedBlock() {
        println("Running test: " + object {}.javaClass.enclosingMethod.name)

        val merged = EnvConfig.initConfig(testConfigJ).getMergedConfig()

        assertEquals("true", merged["polling.status_enabled"])
        assertEquals("300", merged["polling.status_freq_secs"])
        assertEquals("1200", merged["polling.stale_after_secs"])
        assertEquals("900", merged["polling.version_freq_secs"])
        assertFalse(merged.containsKey("polling"))
    }

    @Test
    fun test_merged_blockPathReturnsRelativeKeys() {
        println("Running test: " + object {}.javaClass.enclosingMethod.name)

        val merged = EnvConfig.initConfig(testConfigJ).getMergedConfig("polling")

        assertEquals("true", merged["status_enabled"])
        assertEquals("300", merged["status_freq_secs"])
        assertEquals("1200", merged["stale_after_secs"])
        assertEquals("900", merged["version_freq_secs"])
        assertTrue(merged.keys.none { it.startsWith("polling.") })
        assertTrue(EnvConfig.getMergedConfig("missing").isEmpty())
    }

}
