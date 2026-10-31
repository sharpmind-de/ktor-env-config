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
        private const val SECRET_KEYS_NODE = "secretKeys"
        private const val DEFAULT_ENVIRONMENT = "default"
        private const val SECRET_MASK = "****"

        /**
         * Normalized key names (lowercase, without '_' or '-').
         * `api_key` / `apiKey` and `private_key` / `privateKey` collapse to `apikey` and `privatekey`.
         */
        private val DEFAULT_SECRET_KEY_NAMES = setOf(
            "password",
            "passwd",
            "secret",
            "token",
            "apikey",
            "credential",
            "privatekey",
        )

        private val CAMEL_BOUNDARY = Regex("(?<=[a-z0-9])(?=[A-Z])")

        /**
         * A key is secret when its name, or a `.` / `_` / `-` / camelCase part of it,
         * matches a built-in secret name or [extraSecretKeys]. Matching is case-insensitive.
         */
        internal fun isSecretKey(key: String, extraSecretKeys: Set<String> = emptySet()): Boolean {
            val needles = HashSet<String>(DEFAULT_SECRET_KEY_NAMES.size + extraSecretKeys.size)
            needles.addAll(DEFAULT_SECRET_KEY_NAMES)
            for (name in extraSecretKeys) {
                val trimmed = name.trim()
                if (trimmed.isEmpty()) continue
                addNeedle(needles, trimmed)
                addNeedle(needles, trimmed.substringAfterLast('.'))
            }
            return secretNameCandidates(key).any { normalizeSecretName(it) in needles }
        }

        /**
         * Returns the value as it should appear in a config dump.
         * Nested maps and lists of maps are walked so `db.password` is masked
         * even when [io.ktor.server.config.ApplicationConfig.toMap] keeps the nesting.
         */
        internal fun maskConfigValue(key: String, value: Any?, extraSecretKeys: Set<String> = emptySet()): Any? {
            if (isSecretKey(key, extraSecretKeys)) return SECRET_MASK
            return when (value) {
                is Map<*, *> -> value.entries.associate { (childKey, childValue) ->
                    val childName = childKey.toString()
                    childName to maskConfigValue(childName, childValue, extraSecretKeys)
                }
                is List<*> -> value.map { element ->
                    if (element is Map<*, *>) maskConfigValue(key, element, extraSecretKeys) else element
                }
                else -> value
            }
        }

        private fun addNeedle(needles: MutableSet<String>, name: String) {
            val normalized = normalizeSecretName(name)
            if (normalized.isNotEmpty()) needles.add(normalized)
        }

        private fun normalizeSecretName(name: String): String =
            buildString(name.length) {
                for (ch in name) {
                    when (ch) {
                        '_', '-', ' ', '\t' -> Unit
                        else -> append(ch.lowercaseChar())
                    }
                }
            }

        private fun secretNameCandidates(key: String): List<String> {
            val candidates = ArrayList<String>()
            for (segment in key.split('.')) {
                if (segment.isEmpty()) continue
                candidates.add(segment)
                for (word in segment.split('_', '-')) {
                    if (word.isEmpty()) continue
                    candidates.add(word)
                    val camelParts = CAMEL_BOUNDARY.split(word)
                    if (camelParts.size > 1) candidates.addAll(camelParts)
                }
            }
            return candidates
        }
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

        val mergedConfig = mutableMapOf<String, Any?>()

        config.config(ROOT_NODE).config(DEFAULT_ENVIRONMENT).toMap().forEach { (k, v) ->
            mergedConfig[k] = v
        }

        // merge the default config with the environment config
        if (environment != DEFAULT_ENVIRONMENT) {
            try {
                config.config(ROOT_NODE).config(environment).toMap().forEach { (k, v) ->
                    mergedConfig[k] = v
                }
            } catch (e: Exception) {
                logger.warn("No config found for environment $environment. Using default config.")
            }
        }

        val extraSecretKeys = readExtraSecretKeys()

        logger.info("Environment: $environment")
        logger.info("External config file: ${getExternalConfigFile() ?: "none"}")
        logger.info("Number of config entries: ${mergedConfig.size}")

        mergedConfig.forEach { (key, value) ->
            logger.info("$key = ${maskConfigValue(key, value, extraSecretKeys)}")
        }
        logger.info("--- End of config dump ---")
    }

    /**
     * Names from `envConfig.secretKeys`, as a HOCON list or a comma-separated string.
     * This key sits next to `env` and `externalConfigFile`, not inside an environment block.
     */
    private fun readExtraSecretKeys(): Set<String> {
        val value = config.propertyOrNull("$ROOT_NODE.$SECRET_KEYS_NODE") ?: return emptySet()
        val raw = try {
            value.getList()
        } catch (_: Exception) {
            try {
                value.getString().split(',')
            } catch (_: Exception) {
                emptyList()
            }
        }
        return raw.map { it.trim() }.filter { it.isNotEmpty() }.toSet()
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