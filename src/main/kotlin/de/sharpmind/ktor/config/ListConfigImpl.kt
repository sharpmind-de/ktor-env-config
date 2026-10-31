package de.sharpmind.ktor.config

import ListConfig
import de.sharpmind.ktor.exceptions.KeyNotFoundException
import io.ktor.server.config.*

class ListConfigImpl(private val configCore: ConfigCoreImpl) : ListConfig {

    companion object {
        /** Config key for list delimiter when parsing list from string (e.g. env var). Default is comma. */
        private const val LIST_DELIMITER_KEY = "listDelimiter"
    }

    private fun getListDelimiter(): String =
        configCore.getPropertyValue(LIST_DELIMITER_KEY)?.getString()?.takeIf { it.isNotEmpty() } ?: ","

    /**
     * Branch on [ApplicationConfigValue.type] instead of catching [com.typesafe.config.ConfigException].
     * Ktor 3.2 and 3.4 throw that exception from [ApplicationConfigValue.getList] for a plain string.
     * Ktor 3.6 wraps it in [ApplicationConfigurationException], so the string would never be split.
     * `type` has been part of ApplicationConfigValue since Ktor 3.2.
     */
    private fun parseListValue(value: ApplicationConfigValue): List<String>? =
        when (value.type) {
            ApplicationConfigValue.Type.LIST -> value.getList()
            else -> value.getString().split(getListDelimiter()).map { it.trim() }
        }

    override fun getList(propertyKey: String): List<String> {
        return configCore.getPropertyValue(propertyKey)
            ?.let { parseListValue(it) }
            ?: throw KeyNotFoundException("Property $propertyKey does not exist")
    }

    override fun getListOrNull(propertyKey: String): List<String>? =
        configCore.getPropertyValue(propertyKey)?.let { parseListValue(it) }

    override fun getListOrDefault(propertyKey: String, defaultVal: List<String>): List<String> =
        getListOrNull(propertyKey) ?: defaultVal

    override fun getListOrEmpty(propertyKey: String): List<String> =
        getListOrNull(propertyKey) ?: emptyList()
}