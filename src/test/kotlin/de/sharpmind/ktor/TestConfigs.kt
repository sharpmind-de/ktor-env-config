package de.sharpmind.ktor

import com.typesafe.config.ConfigFactory
import io.ktor.server.config.*

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
