package de.sharpmind.ktor

import com.typesafe.config.ConfigFactory
import de.sharpmind.ktor.config.ConfigCoreImpl
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.config.MapApplicationConfig
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Secret values must not show up in dumpConfig() output.
 * Getters keep returning the real value.
 */
class TestSecretMasking {

    @Test
    fun test_secret_key_matching() {
        println("Running test: " + object {}.javaClass.enclosingMethod.name)

        listOf(
            "password",
            "PASSWORD",
            "db.password",
            "passwd",
            "secret",
            "client_secret",
            "clientSecret",
            "token",
            "access_token",
            "accessToken",
            "api_key",
            "api-key",
            "apiKey",
            "APIKey",
            "credential",
            "private_key",
            "privateKey",
            "private-key",
        ).forEach { key ->
            assertTrue(ConfigCoreImpl.isSecretKey(key), "expected secret key: $key")
        }

        listOf(
            "secretary",
            "tokenizer",
            "passwordless",
            "tokens",
            "username",
            "host",
            "db",
        ).forEach { key ->
            assertFalse(ConfigCoreImpl.isSecretKey(key), "expected non-secret key: $key")
        }

        assertTrue(ConfigCoreImpl.isSecretKey("pin", setOf("pin")))
        assertTrue(ConfigCoreImpl.isSecretKey("user.pin", setOf("pin")))
        assertTrue(ConfigCoreImpl.isSecretKey("db.password", setOf("db.password")))
        assertFalse(ConfigCoreImpl.isSecretKey("pinning", setOf("pin")))
        assertFalse(ConfigCoreImpl.isSecretKey("city", setOf("pin")))
        assertFalse(ConfigCoreImpl.isSecretKey("db", setOf("db.password")))
    }

    @Test
    fun test_nested_value_is_masked_without_flattening() {
        println("Running test: " + object {}.javaClass.enclosingMethod.name)

        val masked = ConfigCoreImpl.maskConfigValue(
            "db",
            mapOf(
                "host" to "localhost",
                "password" to "s3cr3t-db-value-9f3a",
            ),
        )
        val rendered = masked.toString()
        assertFalse(rendered.contains("s3cr3t-db-value-9f3a"), rendered)
        assertTrue(rendered.contains("localhost"), rendered)
        assertTrue(rendered.contains("password=****"), rendered)
    }

    @Test
    fun test_dump_masks_secret_values() {
        println("Running test: " + object {}.javaClass.enclosingMethod.name)

        val output = captureErr {
            EnvConfig.initConfig(secretConfig)
            EnvConfig.dumpConfig()
        }

        // The issue's example value "x" also occurs in words such as "External",
        // so the dump is checked for a distinctive secret marker instead.
        assertFalse(output.contains(SECRET_MARKER), output)
        assertMasked(output, "password")
        assertMasked(output, "api_key")
        assertMasked(output, "apiKey")
        assertMasked(output, "private_key")
        assertMasked(output, "privateKey")
        assertMasked(output, "client_secret")
        assertMasked(output, "clientSecret")
        assertMasked(output, "access_token")
        assertMasked(output, "accessToken")
        assertMasked(output, "passwd")
        assertMasked(output, "credential")
        assertMasked(output, "token")
        assertMasked(output, "PASSWORD")
        assertMasked(output, "pin")
        assertMasked(output, "ssn")

        assertTrue(output.contains("localhost"), output)
        assertTrue(output.contains("visible-secretary"), output)
        assertTrue(output.contains("visible-tokenizer"), output)
        assertTrue(output.contains("visible-passwordless"), output)
        assertTrue(output.contains("visible-tokens"), output)
        assertTrue(output.contains("visible-user"), output)
        assertTrue(output.contains("visible-pinning"), output)
        assertTrue(output.contains("visible-plain"), output)
        assertTrue(output.contains("ada"), output)

        // Getters are unchanged: callers still receive the real secret.
        assertEquals("$SECRET_MARKER-default-password", EnvConfig.getString("db.password"))
        assertEquals("$SECRET_MARKER-env-api-key", EnvConfig.getString("api_key"))
        assertEquals("visible-secretary", EnvConfig.getString("secretary"))
        assertEquals("$SECRET_MARKER-custom-pin", EnvConfig.getString("pin"))
    }

    @Test
    fun test_verbose_init_masks_secret_values() {
        println("Running test: " + object {}.javaClass.enclosingMethod.name)

        val output = captureErr {
            EnvConfig.initConfig(secretConfig, verbose = true)
        }

        assertFalse(output.contains(SECRET_MARKER), output)
        assertMasked(output, "password")
        assertTrue(output.contains("localhost"), output)
    }

    @Test
    fun test_dump_secret_keys_from_comma_separated_string() {
        println("Running test: " + object {}.javaClass.enclosingMethod.name)

        val config = HoconApplicationConfig(
            ConfigFactory.parseString(
                """
                envConfig {
                    env = default
                    secretKeys = "pin, ssn"
                    default {
                        pin = "$SECRET_MARKER-pin"
                        ssn = "$SECRET_MARKER-ssn"
                        city = "Berlin"
                    }
                }
                """.trimIndent()
            )
        )

        val output = captureErr {
            EnvConfig.initConfig(config)
            EnvConfig.dumpConfig()
        }

        assertFalse(output.contains(SECRET_MARKER), output)
        assertMasked(output, "pin")
        assertMasked(output, "ssn")
        assertTrue(output.contains("Berlin"), output)
        assertEquals("$SECRET_MARKER-pin", EnvConfig.getString("pin"))
    }

    @Test
    fun test_dump_map_application_config() {
        println("Running test: " + object {}.javaClass.enclosingMethod.name)

        val config = MapApplicationConfig(
            "envConfig.env" to "default",
            "envConfig.default.db.password" to "$SECRET_MARKER-map-password",
            "envConfig.default.db.host" to "localhost",
            "envConfig.default.username" to "visible-user",
        )

        val output = captureErr {
            EnvConfig.initConfig(config)
            EnvConfig.dumpConfig()
        }

        assertFalse(output.contains(SECRET_MARKER), output)
        assertMasked(output, "password")
        assertTrue(output.contains("localhost"), output)
        assertTrue(output.contains("visible-user"), output)
        assertEquals("$SECRET_MARKER-map-password", EnvConfig.getString("db.password"))
    }

    private fun assertMasked(output: String, key: String) {
        // Require a boundary before the key so `access_token = ****` does not satisfy `token`.
        val masked = Regex("""(?:^|[{\s,])${Regex.escape(key)}\s*=\s*\*\*\*\*""")
        assertTrue(masked.containsMatchIn(output), "Expected $key to be masked as ****.\n$output")
    }

    private fun captureErr(block: () -> Unit): String {
        val previous = System.err
        val bytes = ByteArrayOutputStream()
        val stream = PrintStream(bytes, true, Charsets.UTF_8)
        System.setErr(stream)
        return try {
            block()
            stream.flush()
            bytes.toString(Charsets.UTF_8)
        } finally {
            System.setErr(previous)
        }
    }

    private companion object {
        const val SECRET_MARKER = "s3cr3t"

        val secretConfig = HoconApplicationConfig(
            ConfigFactory.parseString(
                """
                envConfig {
                    env = testEnv
                    secretKeys = ["pin", "ssn"]

                    default {
                        db {
                            host = "localhost"
                            password = "$SECRET_MARKER-default-password"
                            user = "app-user"
                        }
                        api_key = "$SECRET_MARKER-default-api-key"
                        apiKey = "$SECRET_MARKER-apikey-camel"
                        private_key = "$SECRET_MARKER-private-key"
                        privateKey = "$SECRET_MARKER-privatekey-camel"
                        client_secret = "$SECRET_MARKER-client-secret"
                        clientSecret = "$SECRET_MARKER-clientsecret-camel"
                        access_token = "$SECRET_MARKER-access-token"
                        accessToken = "$SECRET_MARKER-accesstoken-camel"
                        passwd = "$SECRET_MARKER-passwd"
                        credential = "$SECRET_MARKER-credential"
                        token = "$SECRET_MARKER-token"
                        PASSWORD = "$SECRET_MARKER-upper-password"
                        secretary = "visible-secretary"
                        tokenizer = "visible-tokenizer"
                        passwordless = "visible-passwordless"
                        tokens = "visible-tokens"
                        username = "visible-user"
                        pinning = "visible-pinning"
                        pin = "$SECRET_MARKER-custom-pin"
                        ssn = "$SECRET_MARKER-custom-ssn"
                        users = [
                            { name = "ada", password = "$SECRET_MARKER-user-password" }
                        ]
                    }

                    testEnv {
                        api_key = "$SECRET_MARKER-env-api-key"
                        plain = "visible-plain"
                    }
                }
                """.trimIndent()
            )
        )
    }
}
