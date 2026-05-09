package de.sharpmind.ktor.config

import DoubleConfig
import de.sharpmind.ktor.exceptions.KeyNotFoundException

class DoubleConfigImpl(private val configCore: ConfigCoreImpl) : DoubleConfig {
    override fun getDouble(propertyKey: String): Double =
        configCore.getPropertyValue(propertyKey)?.getString()?.toDoubleOrNull()
            ?: throw KeyNotFoundException("Property $propertyKey does not exist")

    override fun getDoubleOrNull(propertyKey: String): Double? =
        configCore.getPropertyValue(propertyKey)?.getString()?.toDoubleOrNull()

    override fun getDoubleOrDefault(propertyKey: String, defaultVal: Double): Double =
        getDoubleOrNull(propertyKey) ?: defaultVal
}
