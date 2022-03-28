package de.sharpmind.ktor

import com.typesafe.config.ConfigFactory
import io.ktor.config.*

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
            
            default {
                a = 1
                b = True
            }
    
            testEnv {
                a = 2
                b = false
                configFile = "path_to_the_external_file.conf"
            }
        }
        """.trimIndent()
    )
)
