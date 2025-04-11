package de.sharpmind.ktor.config

import StringConfig
import de.sharpmind.ktor.exceptions.KeyNotFoundException

class StringConfigImpl(private val configCore: ConfigCoreImpl) : StringConfig {
    override fun getString(propertyKey: String): String =
        configCore.getPropertyValue(propertyKey)?.getString()
            ?: throw KeyNotFoundException("Property $propertyKey does not exist")

    override fun getStringOrNull(propertyKey: String): String? =
        configCore.getPropertyValue(propertyKey)?.getString()

    override fun getStringOrDefault(propertyKey: String, defaultVal: String): String =
        getStringOrNull(propertyKey) ?: defaultVal

    override fun getStringOrBlank(propertyKey: String): String =
        getStringOrNull(propertyKey) ?: ""
}