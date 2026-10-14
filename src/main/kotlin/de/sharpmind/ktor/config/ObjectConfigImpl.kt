package de.sharpmind.ktor.config

import ObjectConfig
import de.sharpmind.ktor.exceptions.KeyNotFoundException
import de.sharpmind.ktor.exceptions.PropertyMissingException
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

// Implementation of the object config functionality: binds a config block to a data class
class ObjectConfigImpl(private val configCore: ConfigCoreImpl) : ObjectConfig {
    companion object {
        // Detected by qualified name only so production code does not depend on kotlinx-serialization
        private const val SERIAL_NAME_ANNOTATION = "kotlinx.serialization.SerialName"
    }

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
        val properties = clazz.memberProperties.associateBy { it.name }
        for (param in ctor.parameters) {
            val keyCandidates = keyCandidates(param, properties[param.name])
            // If no candidate resolves, the error points at the snake_case form
            val errorPath = "$propertyKey.${keyCandidates.last()}"
            val value = keyCandidates.firstNotNullOfOrNull { candidate ->
                readValue(param, "$propertyKey.$candidate")?.let { it to candidate }
            }?.first
            args[param] = value
                ?: throw PropertyMissingException("Property $errorPath does not exist and has no default value")
        }

        return ctor.callBy(args)
    }

    // Key candidates for a constructor parameter: an explicit @SerialName value is the
    // only candidate, otherwise the exact parameter name first, then its snake_case form
    private fun keyCandidates(param: KParameter, property: KProperty<*>?): List<String> {
        val paramName = param.name
            ?: throw PropertyMissingException("Constructor parameter has no name")
        serialNameValue(param, property)?.let { return listOf(it) }
        return listOf(paramName, snakeCase(paramName))
    }

    // Read the @SerialName annotation by qualified name via reflection, avoiding a
    // direct dependency on kotlinx-serialization in production code. Its Kotlin targets
    // are [PROPERTY, CLASS], so for constructor `val` parameters it lives on the backing
    // property; the parameter itself is also checked for explicit use-site targets.
    private fun serialNameValue(param: KParameter, property: KProperty<*>?): String? {
        val annotation = param.annotations.firstOrNull { it.isSerialName() }
            ?: property?.annotations?.firstOrNull { it.isSerialName() }
            ?: return null
        return annotation.javaClass.getMethod("value").invoke(annotation) as String
    }

    private fun Annotation.isSerialName(): Boolean = annotationClass.qualifiedName == SERIAL_NAME_ANNOTATION

    private fun snakeCase(name: String): String = name.replace(Regex("([a-z0-9])([A-Z])"), "$1_$2").lowercase()

    private fun readValue(param: KParameter, leafPath: String): Any? = when (param.type.classifier) {
        String::class -> stringConfig.getStringOrNull(leafPath)
        Boolean::class -> booleanConfig.getBooleanOrNull(leafPath)
        Int::class -> intConfig.getIntOrNull(leafPath)
        Long::class -> longConfig.getLongOrNull(leafPath)
        Double::class -> doubleConfig.getDoubleOrNull(leafPath)
        Float::class -> stringConfig.getStringOrNull(leafPath)?.toFloatOrNull()
        List::class -> listConfig.getListOrNull(leafPath)
        else -> null
    }
}
