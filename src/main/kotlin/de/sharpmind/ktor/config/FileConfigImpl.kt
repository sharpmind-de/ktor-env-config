package de.sharpmind.ktor.config

import FileConfig
import StringConfig
import java.io.File

class FileConfigImpl(private val configCore: ConfigCoreImpl, private val stringConfig: StringConfig) : FileConfig {
    override fun getFile(propertyKey: String): File =
        File(stringConfig.getString(propertyKey))

    override fun getFileOrNull(propertyKey: String): File? =
        stringConfig.getStringOrNull(propertyKey)?.let { File(it) }

    override fun getFileOrDefault(propertyKey: String, defaultVal: File): File =
        getFileOrNull(propertyKey) ?: defaultVal
}