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

            // A missing or unsupported external config file is not an error:
            // log a warning and continue without external overrides
            this.extConfig = null
            config.propertyOrNull("$ROOT_NODE.$EXT_CONFIG_NODE")?.getString()?.let { externalFilePath ->
                val externalConfig = HoconConfigLoader().load(externalFilePath)
                if (externalConfig != null) {
                    extConfig = externalConfig
                } else {
                    logger.warn("External config file $externalFilePath not found or not supported - continuing without external overrides")
                }
            }

            if (verbose) dumpConfig()
        }

    override fun getEnvironment(): String = environment

    override fun getExternalConfigFile(): String? = getPropertyValue(ROOT_NODE, EXT_CONFIG_NODE)?.getString()

    override fun getMergedConfig(): Map<String, String> = getMergedConfig("")

    override fun getMergedConfig(blockPath: String): Map<String, String> {
        if (!::config.isInitialized) {
            logger.warn("EnvConfig not initialized yet")
            throw NotInitializedException("Not initialized yet")
        }

        var merged: Map<String, Any?> = emptyMap()
        val rootConfig = config.config(ROOT_NODE)
        if (rootConfig.propertyOrNull(DEFAULT_ENVIRONMENT) != null) {
            merged = deepMerge(merged, rootConfig.config(DEFAULT_ENVIRONMENT).toMap())
        }

        if (environment != DEFAULT_ENVIRONMENT) {
            try {
                merged = deepMerge(merged, config.config(ROOT_NODE).config(environment).toMap())
            } catch (e: Exception) {
                logger.warn("No config found for environment $environment. Using default config.")
            }
        }

        // external config file has the highest priority
        extConfig?.let { merged = deepMerge(merged, it.toMap()) }

        val flattened = flatten(merged)
        if (blockPath.isEmpty()) return flattened

        val prefix = "$blockPath."
        return flattened
            .filterKeys { it.startsWith(prefix) }
            .mapKeys { (key, _) -> key.removePrefix(prefix) }
    }

    private fun deepMerge(base: Map<String, Any?>, override: Map<String, Any?>): Map<String, Any?> {
        val result = base.toMutableMap()
        override.forEach { (key, value) ->
            val baseValue = result[key]
            result[key] = if (baseValue is Map<*, *> && value is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                deepMerge(baseValue as Map<String, Any?>, value as Map<String, Any?>)
            } else {
                value
            }
        }
        return result
    }

    private fun flatten(
        values: Map<String, Any?>,
        prefix: String = "",
        result: MutableMap<String, String> = mutableMapOf()
    ): Map<String, String> {
        values.forEach { (key, value) ->
            val path = if (prefix.isEmpty()) key else "$prefix.$key"
            if (value is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                flatten(value as Map<String, Any?>, path, result)
            } else if (value != null) {
                result[path] = value.toString()
            }
        }
        return result
    }

    override fun dumpConfig() {
        if (!::config.isInitialized) {
            logger.warn("EnvConfig not initialized yet")
            throw NotInitializedException("Not initialized yet")
        }

        logger.info("--- Dumping configuration for environment: $environment ---")

        val mergedConfig = getMergedConfig()

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