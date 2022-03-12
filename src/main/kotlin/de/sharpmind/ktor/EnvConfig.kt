package de.sharpmind.ktor

import de.sharpmind.ktor.exceptions.KeyNotFoundException
import de.sharpmind.ktor.exceptions.NotInitializedException
import io.ktor.server.config.*
import org.slf4j.LoggerFactory

/**
 * Environment config main class
 *
 * todo description
 */
object EnvConfig {
    private const val ROOT_NODE = "envConfig"
    private const val ENVIRONMENT_NODE = "env"
    private const val DEFAULT_ENVIRONMENT = "default"

    private lateinit var config: ApplicationConfig
    private val logger = LoggerFactory.getLogger(this::class.java)
    private var environment: String = DEFAULT_ENVIRONMENT

    /**
     * Initialized env config with the ktor config object
     *
     * @param config the application config (hacoon format)
     * @return EnvConfig object itself for call chaining
     */
    fun initConfig(config: ApplicationConfig): EnvConfig =
        apply {
            this.config = config

            config.propertyOrNull("$ROOT_NODE.$ENVIRONMENT_NODE")?.getString()?.let { customEnv ->
                environment = customEnv
            }
        }

    /**
     * Gets boolean property from config with default fallback
     *
     * @return the default property for provided key or throws an exception if key does not exist
     */
    fun getBoolean(propertyKey: String): Boolean =
        getBooleanInternal(environment, propertyKey)
            ?: throw KeyNotFoundException("Property $propertyKey does not exist")

    /**
     * Gets int property from config with default fallback
     *
     * @return the int property for provided key or throws an exception if key does not exist
     */
    fun getInt(propertyKey: String): Int =
        getIntInternal(environment, propertyKey) ?: throw KeyNotFoundException("Property $propertyKey does not exist")

    /**
     * Gets list property from config with default fallback
     *
     * @return the list property for provided key or throws an exception if key does not exist
     */
    fun getList(propertyKey: String): List<String> =
        getListInternal(environment, propertyKey) ?: throw KeyNotFoundException("Property $propertyKey does not exist")

    /**
     * Gets string property from config with default fallback
     *
     * @return the string property for provided key or throws an exception if key does not exist
     */
    fun getString(propertyKey: String): String =
        getStringInternal(environment, propertyKey)
            ?: throw KeyNotFoundException("Property $propertyKey does not exist")


    /**
     * Gets boolean property from config with default fallback
     *
     * @return the default property for provided key or null if it doesn't exist
     */
    fun getBooleanOrNull(propertyKey: String): Boolean? = getBooleanInternal(environment, propertyKey)

    /**
     * Gets int property from config with default fallback
     *
     * @return the int property for provided key or null if it doesn't exist
     */
    fun getIntOrNull(propertyKey: String): Int? = getIntInternal(environment, propertyKey)

    /**
     * Gets list property from config with default fallback
     *
     * @return the list property for provided key or null if it doesn't exist
     */
    fun getListOrNull(propertyKey: String): List<String>? = getListInternal(environment, propertyKey)

    /**
     * Gets string property from config with default fallback
     *
     * @return the string property for provided key or null if it doesn't exist
     */
    fun getStringOrNull(propertyKey: String): String? = getStringInternal(environment, propertyKey)


    /**
     * Gets boolean property from config with default fallback
     *
     * @param defaultVal the returned value, in case of null
     * @return the default property for provided key or defaultVal if it doesn't exist
     */
    fun getBooleanOrDefault(propertyKey: String, defaultVal: Boolean): Boolean =
        getBooleanOrNull(propertyKey) ?: defaultVal

    /**
     * Gets int property from config with default fallback
     *
     * @param defaultVal the returned value, in case of null
     * @return the int property for provided key or defaultVal if it doesn't exist
     */
    fun getIntOrDefault(propertyKey: String, defaultVal: Int): Int = getIntOrNull(propertyKey) ?: defaultVal

    /**
     * Gets list property from config with default fallback
     *
     * @param defaultVal the returned value, in case of null
     * @return the list property for provided key or defaultVal if it doesn't exist
     */
    fun getListOrDefault(propertyKey: String, defaultVal: List<String>): List<String> =
        getListOrNull(propertyKey) ?: defaultVal

    /**
     * Gets string property from config with default fallback
     *
     * @param defaultVal the returned value, in case of null
     * @return the string property for provided key or defaultVal if it doesn't exist
     */
    fun getStringOrDefault(propertyKey: String, defaultVal: String): String = getStringOrNull(propertyKey) ?: defaultVal

    private fun getBooleanInternal(environment: String, propertyKey: String): Boolean? =
        getStringInternal(environment, propertyKey)?.toBoolean()

    private fun getIntInternal(environment: String, propertyKey: String): Int? =
        getStringInternal(environment, propertyKey)?.toInt()

    private fun getListInternal(environment: String, propertyKey: String): List<String>? =
        getPropertyInternal(environment, propertyKey)?.getList()

    private fun getStringInternal(environment: String, propertyKey: String): String? =
        getPropertyInternal(environment, propertyKey)?.getString()

    /**
     * Get property from config for specified environment, with fallback to default environment
     *
     * @return the application config value for the provided environment and key -
     * or from default environment or null if key is not found
     * @throws Exception if EnvConfig is not initialized yet
     */
    private fun getPropertyInternal(environment: String, propertyKey: String): ApplicationConfigValue? {
        // do we have a config?
        if (!::config.isInitialized) {
            logger.warn("EnvConfig not initialized yet")
            throw NotInitializedException("Not initialized yet")
        }

        val path = "${ROOT_NODE}.${environment}.$propertyKey"
        val pathDefault = "${ROOT_NODE}.${DEFAULT_ENVIRONMENT}.$propertyKey"

        return config.propertyOrNull(path) ?: config.propertyOrNull(pathDefault)
    }
}
