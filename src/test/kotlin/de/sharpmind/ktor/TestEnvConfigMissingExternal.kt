package de.sharpmind.ktor

import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.*

/**
 * Tests for a missing external config file.
 *
 * A configured external config file that cannot be loaded is NOT an error:
 * EnvConfig must log a warning and continue without external overrides.
 * This test class runs in its own JVM (forkEvery = 1), so the EnvConfig
 * logger is initialized only after System.err has been swapped.
 */
class TestEnvConfigMissingExternal {

    @Test
    fun test_missingExternalFile_warnsAndContinues() {
        println("Running test: " + object {}.javaClass.enclosingMethod.name)

        val originalErr = System.err
        val captured = ByteArrayOutputStream()
        val capturingStream = PrintStream(captured, true)
        System.setErr(capturingStream)

        val config = try {
            EnvConfig.initConfig(testConfigI)
        } finally {
            System.setErr(originalErr)
            capturingStream.flush()
        }

        // initialization continues and falls back to environment/default values
        assertEquals(2, config.getInt("a"))
        assertFalse(config.getBoolean("b"))

        // a warning names the missing file
        val logged = captured.toString(Charsets.UTF_8.name())
        assertTrue(logged.contains("WARN"), "expected a WARN level log, got: $logged")
        assertTrue(
            logged.contains("src/test/resources/does-not-exist.conf"),
            "expected warning to name the missing file, got: $logged"
        )
    }
}
