package de.sharpmind.ktor.config

import LongConfig
import de.sharpmind.ktor.exceptions.KeyNotFoundException

class LongConfigImpl(private val configCore: ConfigCoreImpl) : LongConfig {
    override fun getLong(propertyKey: String): Long =
        parseLong(configCore.getPropertyValue(propertyKey)?.getString())
            ?: throw KeyNotFoundException("Property $propertyKey does not exist")

    override fun getLongOrNull(propertyKey: String): Long? =
        parseLong(configCore.getPropertyValue(propertyKey)?.getString())

    override fun getLongOrDefault(propertyKey: String, defaultVal: Long): Long =
        getLongOrNull(propertyKey) ?: defaultVal

    private fun parseLong(value: String?): Long? {
        if (value == null) return null
        return if (value.startsWith("0x", ignoreCase = true)) {
            value.substring(2).toULongOrNull(16)?.toLong()
        } else {
            value.toLongOrNull()
        }
    }
}