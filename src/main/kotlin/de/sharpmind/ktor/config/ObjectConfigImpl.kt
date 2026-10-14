package de.sharpmind.ktor.config

import ObjectConfig
import de.sharpmind.ktor.exceptions.KeyNotFoundException
import de.sharpmind.ktor.exceptions.PropertyMissingException
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.primaryConstructor

// Implementation of the object config functionality: binds a config block to a data class
class ObjectConfigImpl(private val configCore: ConfigCoreImpl) : ObjectConfig {
    private val stringConfig = StringConfigImpl(configCore)
    private val booleanConfig = BooleanConfigImpl(configCore)
    private val intConfig = IntConfigImpl(configCore)
    private val longConfig = LongConfigImpl(configCore)
    private val doubleConfig = DoubleConfigImpl(configCore)
    private val listConfig = ListConfigImpl(configCore)

    override fun <T : Any> getObject(propertyKey: String, clazz: KClass<T>): T {
        val ctor = clazz.primaryConstructor
            ?: throw KeyNotFoundException("Class ${clazz.simpleName} has no primary constructor")

        val args = mutableMapOf<KParameter, Any>()
        for (param in ctor.parameters) {
            val leafPath = "$propertyKey.${param.name}"
            val value: Any? = when (param.type.classifier) {
                String::class -> stringConfig.getStringOrNull(leafPath)
                Boolean::class -> booleanConfig.getBooleanOrNull(leafPath)
                Int::class -> intConfig.getIntOrNull(leafPath)
                Long::class -> longConfig.getLongOrNull(leafPath)
                Double::class -> doubleConfig.getDoubleOrNull(leafPath)
                Float::class -> stringConfig.getStringOrNull(leafPath)?.toFloatOrNull()
                List::class -> listConfig.getListOrNull(leafPath)
                else -> null
            }
            args[param] = value
                ?: throw PropertyMissingException("Property $leafPath does not exist and has no default value")
        }

        return ctor.callBy(args)
    }
}
