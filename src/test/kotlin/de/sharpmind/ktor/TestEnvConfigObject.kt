package de.sharpmind.ktor

import de.sharpmind.ktor.exceptions.NotInitializedException
import de.sharpmind.ktor.exceptions.PropertyMissingException
import de.sharpmind.ktor.exceptions.UnsupportedTypeException
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

    // Runs first within the class (NAME_ASCENDING), while the singleton is still uninitialized
    @Test
    fun _TestUninitialized_getObject() {
        println("Running test: " + object {}.javaClass.enclosingMethod.name)
        assertFailsWith<NotInitializedException> { EnvConfig.getObject("polling", PollingConfig::class) }
    }

    @Test
    fun test_getObject_unsupportedTypeThrows() {
        println("Running test: " + object {}.javaClass.enclosingMethod.name)
        val ex = assertFailsWith<UnsupportedTypeException> {
            EnvConfig.initConfig(testConfigL).getObject("weirdtype", WeirdTypeConfig::class)
        }
        assertTrue(ex.message!!.contains("weirdtype"), "message was: ${ex.message}")
    }

    @Test
    fun test_getObject_unsupportedListElementTypeThrows() {
        println("Running test: " + object {}.javaClass.enclosingMethod.name)
        assertFailsWith<UnsupportedTypeException> {
            EnvConfig.initConfig(testConfigN).getObject("intlist", IntListBlock::class)
        }
    }

    @Test
    fun test_getObject_reifiedConvenience() {
        println("Running test: " + object {}.javaClass.enclosingMethod.name)
        val viaReified = EnvConfig.initConfig(testConfigK).getObject<PollingConfig>("polling")
        assertEquals(999, viaReified.statusFreqSecs)
        assertNull(EnvConfig.getObjectOrNull<AllDefaultsConfig>("noSuchBlock"))
    }

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
    fun test_getObjectOrNull_blockExistence() {
        println("Running test: " + object {}.javaClass.enclosingMethod.name)
        val cfg = EnvConfig.initConfig(testConfigL)

        // absent block -> null
        assertNull(cfg.getObjectOrNull("noSuchBlock", AllDefaultsConfig::class))
        // present block -> filled object
        assertEquals(1, cfg.getObjectOrNull("defaults", DefaultsConfig::class)!!.present)
        // present block (has leaves) with a required property missing -> still throws;
        // null means "block absent", not "block invalid"
        assertFailsWith<PropertyMissingException> { cfg.getObjectOrNull("partial", RequiredConfig::class) }
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

    @Test
    fun test_getObject_listAndNestedBlock() {
        println("Running test: " + object {}.javaClass.enclosingMethod.name)
        val obj = EnvConfig.initConfig(testConfigM).getObject("app", AppBlock::class)

        // list: env block's comma-separated string wins over default's HOCON list
        assertEquals(listOf("x", "y", "z"), obj.tags)
        // nested block: host from default, port overridden by env block (deep chain)
        assertEquals("db.internal", obj.db.host)
        assertEquals(1234, obj.db.port)
    }

    @Test
    fun test_getObject_nestedMissingRequiredThrows() {
        println("Running test: " + object {}.javaClass.enclosingMethod.name)
        // nested block present (has leaves) but a required nested property is missing
        val ex = assertFailsWith<PropertyMissingException> {
            EnvConfig.initConfig(testConfigM).getObject("nest", NestBlock::class)
        }
        assertTrue(ex.message!!.contains("nest.db.host"), "message was: ${ex.message}")
    }

    @Test
    fun test_getObject_nestedMissingUsesDefaults() {
        println("Running test: " + object {}.javaClass.enclosingMethod.name)
        // nested block absent entirely + nullable nested param -> null
        val obj = EnvConfig.initConfig(testConfigM).getObject("bare", BareBlock::class)
        assertNull(obj.db)
    }
}
