package de.sharpmind.ktor

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
}
