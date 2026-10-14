package de.sharpmind.ktor

import de.sharpmind.ktor.exceptions.PropertyMissingException
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import kotlin.test.*

/**
 * Tests for EnvConfig.getObject()
 *
 * These test methods have to run in a specific order.
 * Therefore NAME_ASCENDING is configured for method sorting.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class TestEnvConfigObject {

    @Test
    fun test_getObject_fillsClassFromBlock() {
        println("Running test: " + object {}.javaClass.enclosingMethod.name)
        val obj = EnvConfig.initConfig(testObjectConfig).getObject("poll", PlainPolling::class)
        assertEquals(300, obj.statusFreqSecs)   // env block overrides default (600)
        assertEquals("poll", obj.label)         // default block value
    }

    @Test
    fun test_getObject_fullPrecedenceAndSnakeCaseNaming() {
        println("Running test: " + object {}.javaClass.enclosingMethod.name)
        val obj = EnvConfig.initConfig(testConfigK).getObject("polling", PollingConfig::class)
        assertEquals(999, obj.statusFreqSecs)     // external file beats env (300) beats default (600)
        assertTrue(obj.statusEnabled)             // default block
        assertEquals(1200, obj.staleAfterSecs)    // default block
    }

    @Test
    fun test_getObject_serialNameOverride() {
        println("Running test: " + object {}.javaClass.enclosingMethod.name)
        val obj = EnvConfig.initConfig(testConfigK).getObject("odd", OddConfig::class)
        assertEquals(7, obj.weirdKey)
    }

    @Test
    fun test_getObject_exactKeyWinsOverSnakeCase() {
        println("Running test: " + object {}.javaClass.enclosingMethod.name)
        val obj = EnvConfig.initConfig(testConfigExact).getObject("poll", PlainPolling::class)
        assertEquals(111, obj.statusFreqSecs)     // exact parameter-name key beats snake_case candidate (222)
    }

    @Test
    fun test_getObject_missingProperty_defaultOrNullOrThrow() {
        println("Running test: " + object {}.javaClass.enclosingMethod.name)
        val cfg = EnvConfig.initConfig(testConfigL)

        // present -> bound; absent with default -> default; absent nullable -> null; unparsable -> default
        val obj = cfg.getObject("defaults", DefaultsConfig::class)
        assertEquals(1, obj.present)
        assertEquals(42, obj.absent)
        assertNull(obj.maybe)
        assertEquals(42, obj.bad)                    // testConfigL sets bad = "abc"

        // absent, no default, non-nullable -> PropertyMissingException naming the leaf path
        // (fallback error key is the snake_case form: keyCandidates.last())
        val ex = assertFailsWith<PropertyMissingException> { cfg.getObject("required", RequiredConfig::class) }
        assertTrue(ex.message!!.contains("required.required_value"), "message was: ${ex.message}")

        // getObject never returns null: absent block with all defaults -> defaults object
        assertEquals(5, cfg.getObject("noSuchBlock", AllDefaultsConfig::class).x)
    }
}
