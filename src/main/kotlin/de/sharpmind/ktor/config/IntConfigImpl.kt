package de.sharpmind.ktor.config

import IntConfig
import de.sharpmind.ktor.exceptions.KeyNotFoundException

class IntConfigImpl(private val configCore: ConfigCoreImpl) : IntConfig {
    override fun getInt(propertyKey: String): Int =
        configCore.getPropertyValue(propertyKey)?.getString()?.toInt()
            ?: throw KeyNotFoundException("Property $propertyKey does not exist")

    override fun getIntOrNull(propertyKey: String): Int? =
        configCore.getPropertyValue(propertyKey)?.getString()?.toInt()

    override fun getIntOrDefault(propertyKey: String, defaultVal: Int): Int =
        getIntOrNull(propertyKey) ?: defaultVal
}