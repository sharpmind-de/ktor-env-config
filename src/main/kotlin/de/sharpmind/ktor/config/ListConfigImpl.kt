package de.sharpmind.ktor.config

import ListConfig
import de.sharpmind.ktor.exceptions.KeyNotFoundException

class ListConfigImpl(private val configCore: ConfigCoreImpl) : ListConfig {
    override fun getList(propertyKey: String): List<String> =
        configCore.getPropertyValue(propertyKey)?.getList()
            ?: throw KeyNotFoundException("Property $propertyKey does not exist")

    override fun getListOrNull(propertyKey: String): List<String>? =
        configCore.getPropertyValue(propertyKey)?.getList()

    override fun getListOrDefault(propertyKey: String, defaultVal: List<String>): List<String> =
        getListOrNull(propertyKey) ?: defaultVal

    override fun getListOrEmpty(propertyKey: String): List<String> {
        return getListOrNull(propertyKey) ?: emptyList()
    }
}