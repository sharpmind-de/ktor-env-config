package de.sharpmind.ktor

import com.typesafe.config.ConfigFactory
import io.ktor.server.config.*
import kotlinx.serialization.SerialName

// TEST CONFIG A
val testConfigA = HoconApplicationConfig(
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
val testConfigB = HoconApplicationConfig(
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
val testConfigC = HoconApplicationConfig(ConfigFactory.parseString(""))

// TEST CONFIG D
val testConfigD = HoconApplicationConfig(
    ConfigFactory.parseString(
        """
        envConfig {
            env = testEnv
            externalConfigFile = "src/test/resources/test.conf"
    
            default {
                a = 1
                b = false
            }
    
            testEnv {
                a = 2
                b = false
            }
        }
        """.trimIndent()
    )
)

// TEST CONFIG E
val testConfigE = HoconApplicationConfig(
    ConfigFactory.parseString(
        """
        envConfig {
            env = testEnv
    
            default {
                a = 1
                b = false
            }
    
            testEnv {
                url1 = "http://localhost:8080"
                url2 = "https://www.google.com/foobar?foo=bar&baz=qux"
                file1 = "c:/foo/bar/baz.txt"
                file2 = "/root/bin/test.sh"
            }
        }
        """.trimIndent()
    )
)

// TEST CONFIG F: list from comma-separated string (e.g. env var)
val testConfigF = HoconApplicationConfig(
    ConfigFactory.parseString(
        """
        envConfig {
            env = testEnv

            default {
                listFromString = "foo,bar,baz"
                listWithSpaces = "one , two , three"
                listDelimiterAtStart = ",foo,bar"
                listDelimiterAtEnd = "foo,bar,"
            }

            testEnv {
            }
        }
        """.trimIndent()
    )
)

// TEST CONFIG G: custom list delimiter
val testConfigG = HoconApplicationConfig(
    ConfigFactory.parseString(
        """
        envConfig {
            env = testEnv

            default {
                listDelimiter = ";"
                listFromString = "alpha;beta;gamma"
            }

            testEnv {
            }
        }
        """.trimIndent()
    )
)

// TEST CONFIG H: Numeric types including hex
val testConfigH = HoconApplicationConfig(
    ConfigFactory.parseString(
        """
        envConfig {
            env = testEnv

            default {
                byteHex = "0x30"
                byteDecimal = "127"
                byteOverflow = "0x1000"
                byte127plus1 = "128"
                longHex = "0xFFFFFFFFFFFFFFFF"
                longDecimal = "1234567890123456"
                doubleValue = "3.14159"
                doubleWhole = "42"
            }

            testEnv {
            }
        }
        """.trimIndent()
    )
)

// TEST CONFIG J: nested config with default, environment, and external overrides
val testConfigJ = HoconApplicationConfig(
    ConfigFactory.parseString(
        """
        envConfig {
            env = testEnv
            externalConfigFile = "src/test/resources/test-merge.conf"

            default {
                a = 1
                polling {
                    status_enabled = true
                    status_freq_secs = 600
                    version_freq_secs = 3600
                    stale_after_secs = 1200
                }
            }

            testEnv {
                polling {
                    status_freq_secs = 300
                    version_freq_secs = 1800
                }
            }
        }
        """.trimIndent()
    )
)

// TEST CONFIG I: external config file that does not exist
val testConfigI = HoconApplicationConfig(
    ConfigFactory.parseString(
        """
        envConfig {
            env = testEnv
            externalConfigFile = "src/test/resources/does-not-exist.conf"

            default {
                a = 1
                b = false
            }

            testEnv {
                a = 2
            }
        }
        """.trimIndent()
    )
)

// TEST CONFIG for getObject: config block bound to a plain data class
data class PlainPolling(val statusFreqSecs: Int, val label: String)

val testObjectConfig = HoconApplicationConfig(
    ConfigFactory.parseString(
        """
        envConfig {
            env = testEnv

            default {
                poll {
                    statusFreqSecs = 600
                    label = "poll"
                }
            }

            testEnv {
                poll {
                    statusFreqSecs = 300
                }
            }
        }
        """.trimIndent()
    )
)

// TEST CONFIG K: snake_case config keys, external > env > default precedence, @SerialName override
data class PollingConfig(
    val statusEnabled: Boolean = false,
    val versionEnabled: Boolean = false,
    val statusFreqSecs: Int = 30,
    val versionFreqSecs: Int = 60,
    val statusInitialDelaySecs: Int = 0,
    val versionInitialDelaySecs: Int = 0,
    val staleAfterSecs: Int = 300
)

data class OddConfig(@SerialName("odd_name") val weirdKey: Int)

val testConfigK = HoconApplicationConfig(ConfigFactory.parseString("""
    envConfig {
        env = testEnv
        externalConfigFile = "src/test/resources/test-object.conf"
        default {
            polling {
                status_enabled    = true
                version_enabled   = true
                status_freq_secs  = 600
                version_freq_secs = 3600
                status_initial_delay_secs = 60
                version_initial_delay_secs = 120
                stale_after_secs  = 1200
            }
            odd { odd_name = 7 }
        }
        testEnv {
            polling { status_freq_secs = 300 }
        }
    }
""".trimIndent()))

// TEST CONFIG K2: exact camelCase parameter-name key must win over the snake_case candidate
val testConfigExact = HoconApplicationConfig(
    ConfigFactory.parseString(
        """
        envConfig {
            env = testEnv

            default {
                poll {
                    statusFreqSecs = 111
                    status_freq_secs = 222
                    label = "poll"
                }
            }

            testEnv {
            }
        }
        """.trimIndent()
    )
)

// TEST CONFIG L: missing-property rule — constructor defaults, nullable null, unparsable-as-missing
data class DefaultsConfig(val present: Int, val absent: Int = 42, val maybe: String?, val bad: Int = 42)
data class RequiredConfig(val requiredValue: Int)
data class AllDefaultsConfig(val x: Int = 5)

val testConfigL = HoconApplicationConfig(ConfigFactory.parseString("""
    envConfig {
        env = testEnv
        default {
            defaults { present = 1, bad = "abc" }
            required { }
            partial { other = 1 }     # block with leaves but without requiredValue — used by Task 4
        }
        testEnv { }
    }
""".trimIndent()))
