package de.sharpmind.ktor.config

import BooleanConfig
import de.sharpmind.ktor.exceptions.KeyNotFoundException

class BooleanConfigImpl(private val configCore: ConfigCoreImpl) : BooleanConfig {
    override fun getBoolean(propertyKey: String): Boolean =
        configCore.getPropertyValue(propertyKey)?.getString()?.toBoolean()
            ?: throw KeyNotFoundException("Property $propertyKey does not exist")

    override fun getBooleanOrNull(propertyKey: String): Boolean? =
        configCore.getPropertyValue(propertyKey)?.getString()?.toBoolean()

    override fun getBooleanOrDefault(propertyKey: String, defaultVal: Boolean): Boolean =
        getBooleanOrNull(propertyKey) ?: defaultVal

    override fun getBooleanOrTrue(propertyKey: String): Boolean =
        getBooleanOrDefault(propertyKey, true)

    override fun getBooleanOrFalse(propertyKey: String): Boolean =
        getBooleanOrDefault(propertyKey, false)
}