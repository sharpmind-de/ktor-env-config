package de.sharpmind.ktor

import io.ktor.config.ApplicationConfig
import io.ktor.config.ApplicationConfigValue
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

    /* todo  in case of an override use ConfigFactory.load("conf/url_short").withFallback(ConfigFactory.defaultApplication()).resolve()
    private val urlShortConfig: Config =
        ConfigFactory.load("conf/url_short").withFallback(ConfigFactory.defaultApplication()).resolve()*/
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

            // if the environment property is available, we set it, if not use default
            this.environment = config.propertyOrNull("$ROOT_NODE.$ENVIRONMENT_NODE")?.getString() ?: DEFAULT_ENVIRONMENT
            logger.info("use environment: $environment")
        }

    /**
     * Gets boolean property from config with default fallback
     *
     * @return the default property for provided key or null if it doesn't exist
     */
    fun getBoolean(propertyKey: String): Boolean? =
        getBooleanInternal(environment, propertyKey) ?: getBooleanInternal(DEFAULT_ENVIRONMENT, propertyKey)

    /**
     * Gets int property from config with default fallback
     *
     * @return the int property for provided key or null if it doesn't exist
     */
    fun getInt(propertyKey: String): Int? =
        getIntInternal(environment, propertyKey) ?: getIntInternal(DEFAULT_ENVIRONMENT, propertyKey)

    /**
     * Gets list property from config with default fallback
     *
     * @return the list property for provided key or null if it doesn't exist
     */
    fun getList(propertyKey: String): List<String>? =
        getListInternal(environment, propertyKey) ?: getListInternal(DEFAULT_ENVIRONMENT, propertyKey)

    /**
     * Gets string property from config with default fallback
     *
     * @return the string property for provided key or null if it doesn't exist
     */
    fun getString(propertyKey: String): String? =
        getStringInternal(environment, propertyKey) ?: getStringInternal(DEFAULT_ENVIRONMENT, propertyKey)

    /**
     * Gets boolean property from config with default fallback
     *
     * @param defaultVal the returned value, in case of null
     * @return the default property for provided key or defaultVal if it doesn't exist
     */
    fun getBoolean(propertyKey: String, defaultVal: Boolean): Boolean = getBoolean(propertyKey) ?: defaultVal

    /**
     * Gets int property from config with default fallback
     *
     * @param defaultVal the returned value, in case of null
     * @return the int property for provided key or defaultVal if it doesn't exist
     */
    fun getInt(propertyKey: String, defaultVal: Int): Int = getInt(propertyKey) ?: defaultVal

    /**
     * Gets list property from config with default fallback
     *
     * @param defaultVal the returned value, in case of null
     * @return the list property for provided key or defaultVal if it doesn't exist
     */
    fun getList(propertyKey: String, defaultVal: List<String>): List<String> = getList(propertyKey) ?: defaultVal

    /**
     * Gets string property from config with default fallback
     *
     * @param defaultVal the returned value, in case of null
     * @return the string property for provided key or defaultVal if it doesn't exist
     */
    fun getString(propertyKey: String, defaultVal : String): String = getString(propertyKey) ?: defaultVal

    private fun getBooleanInternal(environment: String, propertyKey: String): Boolean? =
        getStringInternal(environment, propertyKey)?.toBoolean()

    private fun getIntInternal(environment: String, propertyKey: String): Int? =
        getStringInternal(environment, propertyKey)?.toInt()

    private fun getListInternal(environment: String, propertyKey: String): List<String>? =
        getPropertyInternal(environment, propertyKey)?.getList()

    private fun getStringInternal(environment: String, propertyKey: String): String? =
        getPropertyInternal(environment, propertyKey)?.getString()

    /**
     * Get property from config for specified environment
     *
     * @return the application config value for the provided environment and key - or null if key is not found
     * @throws Exception if EnvConfig is not initialized yet
     */
    private fun getPropertyInternal(environment: String, propertyKey: String): ApplicationConfigValue? {
        // do we have a config?
        if (!::config.isInitialized) {
            logger.warn("EnvConfig not initialized yet")
            throw Exception("Not initialized yet")
        }

        val path = "${ROOT_NODE}.${environment}.$propertyKey"
        return config.propertyOrNull(path)
    }
}
