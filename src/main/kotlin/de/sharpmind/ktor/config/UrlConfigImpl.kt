package de.sharpmind.ktor.config

import StringConfig
import UrlConfig
import org.slf4j.LoggerFactory
import java.net.MalformedURLException
import java.net.URL

class UrlConfigImpl(private val configCore: ConfigCoreImpl, private val stringConfig: StringConfig) : UrlConfig {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun getUrl(propertyKey: String): URL {
        val url = stringConfig.getString(propertyKey)
        return try {
            URL(url)
        } catch (e: MalformedURLException) {
            logger.error("Malformed URL: $url")
            throw e
        }
    }

    override fun getUrlOrNull(propertyKey: String): URL? =
        try {
            stringConfig.getStringOrNull(propertyKey)?.let { URL(it) }
        } catch (e: MalformedURLException) {
            null
        }

    override fun getUrlOrDefault(propertyKey: String, defaultVal: URL): URL =
        getUrlOrNull(propertyKey) ?: defaultVal
}