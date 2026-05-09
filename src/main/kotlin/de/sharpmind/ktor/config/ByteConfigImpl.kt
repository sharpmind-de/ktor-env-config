package de.sharpmind.ktor.config

import ByteConfig
import de.sharpmind.ktor.exceptions.KeyNotFoundException
import java.lang.NumberFormatException

class ByteConfigImpl(private val configCore: ConfigCoreImpl) : ByteConfig {
    override fun getByte(propertyKey: String): Byte =
        parseByte(configCore.getPropertyValue(propertyKey)?.getString())
            ?: throw KeyNotFoundException("Property $propertyKey does not exist")

    override fun getByteOrNull(propertyKey: String): Byte? =
        parseByte(configCore.getPropertyValue(propertyKey)?.getString())

    override fun getByteOrDefault(propertyKey: String, defaultVal: Byte): Byte =
        getByteOrNull(propertyKey) ?: defaultVal

    private fun parseByte(value: String?): Byte? {
        if (value == null) return null
        val longValue = if (value.startsWith("0x", ignoreCase = true)) {
            value.substring(2).toULongOrNull(16)?.toLong()
        } else {
            value.toLongOrNull()
        }
        if (longValue == null) return null
        if (longValue < Byte.MIN_VALUE || longValue > Byte.MAX_VALUE) {
            throw NumberFormatException(
                "Value $longValue is outside valid Byte range (${Byte.MIN_VALUE} to ${Byte.MAX_VALUE})"
            )
        }
        return longValue.toByte()
    }
}
