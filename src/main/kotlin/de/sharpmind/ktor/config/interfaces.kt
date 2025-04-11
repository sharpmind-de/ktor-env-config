import io.ktor.server.config.*
import java.io.File
import java.net.URL

// Core configuration interfaces

/**
 * Core configuration functionality shared across types
 */
interface ConfigCore {
    fun initConfig(config: ApplicationConfig, verbose: Boolean = false): ConfigCore
    fun getEnvironment(): String
    fun getExternalConfigFile(): String?
    fun dumpConfig()
}

/**
 * String configuration access
 */
interface StringConfig {
    fun getString(propertyKey: String): String
    fun getStringOrNull(propertyKey: String): String?
    fun getStringOrDefault(propertyKey: String, defaultVal: String): String
    fun getStringOrBlank(propertyKey: String): String
}

/**
 * Boolean configuration access
 */
interface BooleanConfig {
    fun getBoolean(propertyKey: String): Boolean
    fun getBooleanOrNull(propertyKey: String): Boolean?
    fun getBooleanOrDefault(propertyKey: String, defaultVal: Boolean): Boolean
    fun getBooleanOrTrue(propertyKey: String): Boolean
    fun getBooleanOrFalse(propertyKey: String): Boolean
}

/**
 * Integer configuration access
 */
interface IntConfig {
    fun getInt(propertyKey: String): Int
    fun getIntOrNull(propertyKey: String): Int?
    fun getIntOrDefault(propertyKey: String, defaultVal: Int): Int
}

/**
 * List configuration access
 */
interface ListConfig {
    fun getList(propertyKey: String): List<String>
    fun getListOrNull(propertyKey: String): List<String>?
    fun getListOrDefault(propertyKey: String, defaultVal: List<String>): List<String>
    fun getListOrEmpty(propertyKey: String): List<String>
}

/**
 * File configuration access
 */
interface FileConfig {
    fun getFile(propertyKey: String): File
    fun getFileOrNull(propertyKey: String): File?
    fun getFileOrDefault(propertyKey: String, defaultVal: File): File
}

/**
 * URL configuration access
 */
interface UrlConfig {
    fun getUrl(propertyKey: String): URL
    fun getUrlOrNull(propertyKey: String): URL?
    fun getUrlOrDefault(propertyKey: String, defaultVal: URL): URL
}

