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
}
