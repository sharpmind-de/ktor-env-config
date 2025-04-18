package de.sharpmind.ktor.config

import ConfigCore
import de.sharpmind.ktor.EnvConfig
import de.sharpmind.ktor.exceptions.NotInitializedException
import io.ktor.server.config.*
import org.slf4j.LoggerFactory

// Implementation of the core config functionality that other classes will depend on
class ConfigCoreImpl : ConfigCore {
    companion object {
        private const val ROOT_NODE = "envConfig"
        private const val ENVIRONMENT_NODE = "env"
        private const val EXT_CONFIG_NODE = "externalConfigFile"
        private const val DEFAULT_ENVIRONMENT = "default"
    }

    private lateinit var config: ApplicationConfig
    private var extConfig: ApplicationConfig? = null
    private val logger = LoggerFactory.getLogger(EnvConfig.loggerName)
    private var environment: String = DEFAULT_ENVIRONMENT

    override fun initConfig(config: ApplicationConfig, verbose: Boolean): ConfigCore =
        apply {
            this.config = config

            // set environment if configured
            this.environment = DEFAULT_ENVIRONMENT
            config.propertyOrNull("$ROOT_NODE.$ENVIRONMENT_NODE")?.getString()?.let { customEnv ->
                environment = customEnv
            }

            logger.info("Initializing EnvConfig with environment: $environment")

            // set external config file if configured and existent
            this.extConfig = null
            config.propertyOrNull("$ROOT_NODE.$EXT_CONFIG_NODE")?.getString()?.let { externalFilePath ->
                HoconConfigLoader().load(externalFilePath)?.let { externalConfig ->
                    extConfig = externalConfig
                } ?: {
                    logger.warn("External config file $externalFilePath not found or not supported")
                    throw NotInitializedException("External config file $externalFilePath not found or not supported")
                }
            }

            if (verbose) dumpConfig()
        }

    override fun getEnvironment(): String = environment

    override fun getExternalConfigFile(): String? = getPropertyValue(ROOT_NODE, EXT_CONFIG_NODE)?.getString()

    override fun dumpConfig() {
        if (!::config.isInitialized) {
            logger.warn("EnvConfig not initialized yet")
            throw NotInitializedException("Not initialized yet")
        }

        logger.info("--- Dumping configuration for environment: $environment ---")

        val mergedConfig = mutableMapOf<String, String>()

        config.config(ROOT_NODE).config(DEFAULT_ENVIRONMENT).toMap().forEach { (k, v) ->
            mergedConfig[k] = v.toString()
        }

        // merge the default config with the environment config
        if (environment != DEFAULT_ENVIRONMENT) {
            try {
                config.config(ROOT_NODE).config(environment).toMap().forEach { (k, v) ->
                    mergedConfig[k] = v.toString()
                }
            } catch (e: Exception) {
                logger.warn("No config found for environment $environment. Using default config.")
            }
        }

        logger.info("Environment: $environment")
        logger.info("External config file: ${getExternalConfigFile() ?: "none"}")
        logger.info("Number of config entries: ${mergedConfig.size}")

        mergedConfig.forEach { (key, value) ->
            logger.info("$key = $value")
        }
        logger.info("--- End of config dump ---")
    }

    /**
     * Get raw property value from config
     */
    fun getPropertyValue(propertyKey: String): ApplicationConfigValue? {
        // do we have a config?
        if (!::config.isInitialized) {
            logger.warn("EnvConfig not initialized yet")
            throw NotInitializedException("Not initialized yet")
        }

        val pathExternal = propertyKey
        val pathEnvironment = "${ROOT_NODE}.${environment}.$propertyKey"
        val pathDefault = "${ROOT_NODE}.${DEFAULT_ENVIRONMENT}.$propertyKey"

        // try external config file first, then environment block, then default
        return extConfig?.propertyOrNull(pathExternal)
            ?: config.propertyOrNull(pathEnvironment)
            ?: config.propertyOrNull(pathDefault)
    }

    /**
     * Helper method to get property value when passing separate components
     */
    fun getPropertyValue(vararg paths: String): ApplicationConfigValue? {
        return getPropertyValue(paths.joinToString("."))
    }
}