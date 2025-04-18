package de.sharpmind.ktor

import BooleanConfig
import ConfigCore
import FileConfig
import IntConfig
import ListConfig
import StringConfig
import UrlConfig
import de.sharpmind.ktor.config.*
import io.ktor.server.config.*
import java.io.File
import java.net.URL

// The main EnvConfig class that uses delegation to implement all interfaces
object EnvConfig : ConfigCore, StringConfig, BooleanConfig, IntConfig, ListConfig, FileConfig, UrlConfig {
    val loggerName = "de.sharpmind.ktor.EnvConfig"

    private val configCore = ConfigCoreImpl()
    private val stringConfig by lazy { StringConfigImpl(configCore) }
    private val booleanConfig by lazy { BooleanConfigImpl(configCore) }
    private val intConfig by lazy { IntConfigImpl(configCore) }
    private val listConfig by lazy { ListConfigImpl(configCore) }
    private val fileConfig by lazy { FileConfigImpl(configCore, stringConfig) }
    private val urlConfig by lazy { UrlConfigImpl(configCore, stringConfig) }

    // Delegate ConfigCore methods
    override fun initConfig(config: ApplicationConfig, verbose: Boolean): EnvConfig =
        apply { configCore.initConfig(config, verbose) }

    override fun getEnvironment(): String = configCore.getEnvironment()
    override fun getExternalConfigFile(): String? = configCore.getExternalConfigFile()
    override fun dumpConfig() = configCore.dumpConfig()

    // Delegate StringConfig methods
    override fun getString(propertyKey: String): String = stringConfig.getString(propertyKey)
    override fun getStringOrNull(propertyKey: String): String? = stringConfig.getStringOrNull(propertyKey)
    override fun getStringOrDefault(propertyKey: String, defaultVal: String): String =
        stringConfig.getStringOrDefault(propertyKey, defaultVal)

    override fun getStringOrBlank(propertyKey: String): String = stringConfig.getStringOrBlank(propertyKey)

    // Delegate BooleanConfig methods
    override fun getBoolean(propertyKey: String): Boolean = booleanConfig.getBoolean(propertyKey)
    override fun getBooleanOrNull(propertyKey: String): Boolean? = booleanConfig.getBooleanOrNull(propertyKey)
    override fun getBooleanOrDefault(propertyKey: String, defaultVal: Boolean): Boolean =
        booleanConfig.getBooleanOrDefault(propertyKey, defaultVal)

    override fun getBooleanOrTrue(propertyKey: String): Boolean = booleanConfig.getBooleanOrTrue(propertyKey)
    override fun getBooleanOrFalse(propertyKey: String): Boolean = booleanConfig.getBooleanOrFalse(propertyKey)

    // Delegate IntConfig methods
    override fun getInt(propertyKey: String): Int = intConfig.getInt(propertyKey)
    override fun getIntOrNull(propertyKey: String): Int? = intConfig.getIntOrNull(propertyKey)
    override fun getIntOrDefault(propertyKey: String, defaultVal: Int): Int =
        intConfig.getIntOrDefault(propertyKey, defaultVal)

    // Delegate ListConfig methods
    override fun getList(propertyKey: String): List<String> = listConfig.getList(propertyKey)
    override fun getListOrNull(propertyKey: String): List<String>? = listConfig.getListOrNull(propertyKey)
    override fun getListOrDefault(propertyKey: String, defaultVal: List<String>): List<String> =
        listConfig.getListOrDefault(propertyKey, defaultVal)

    override fun getListOrEmpty(propertyKey: String): List<String> = listConfig.getListOrEmpty(propertyKey)

    // Delegate FileConfig methods
    override fun getFile(propertyKey: String): File = fileConfig.getFile(propertyKey)
    override fun getFileOrNull(propertyKey: String): File? = fileConfig.getFileOrNull(propertyKey)
    override fun getFileOrDefault(propertyKey: String, defaultVal: File): File =
        fileConfig.getFileOrDefault(propertyKey, defaultVal)

    // Delegate UrlConfig methods
    override fun getUrl(propertyKey: String): URL = urlConfig.getUrl(propertyKey)
    override fun getUrlOrNull(propertyKey: String): URL? = urlConfig.getUrlOrNull(propertyKey)
    override fun getUrlOrDefault(propertyKey: String, defaultVal: URL): URL =
        urlConfig.getUrlOrDefault(propertyKey, defaultVal)
}