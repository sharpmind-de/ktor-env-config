package de.sharpmind.ktor.config

import ObjectConfig
import de.sharpmind.ktor.exceptions.KeyNotFoundException
import de.sharpmind.ktor.exceptions.PropertyMissingException
import de.sharpmind.ktor.exceptions.UnsupportedTypeException
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

    override fun <T : Any> getObjectOrNull(propertyKey: String, clazz: KClass<T>): T? =
        if (configCore.getMergedConfig(propertyKey).isEmpty()) null else getObject(propertyKey, clazz)

    override fun <T : Any> getObject(propertyKey: String, clazz: KClass<T>): T {
        val ctor = clazz.primaryConstructor
            ?: throw KeyNotFoundException("Class ${clazz.simpleName} has no primary constructor")

        val args = mutableMapOf<KParameter, Any?>()
        val properties = clazz.memberProperties.associateBy { it.name }
        for (param in ctor.parameters) {
            val keyCandidates = keyCandidates(param, properties[param.name])
            // If no candidate resolves, the error points at the snake_case form
            val errorPath = "$propertyKey.${keyCandidates.last()}"
            val value = keyCandidates.firstNotNullOfOrNull { candidate ->
                // An unparsable value counts as missing. NotInitializedException and
                // UnsupportedTypeException extend Throwable, not Exception, so they
                // still propagate out of here.
                try {
                    readValue(param, "$propertyKey.$candidate")
                } catch (e: Exception) {
                    null
                }
            }
            when {
                value != null -> args[param] = value
                // Omit the parameter from args so callBy applies the constructor default
                param.isOptional -> Unit
                param.type.isMarkedNullable -> args[param] = null
                else -> throw PropertyMissingException("Property $errorPath does not exist and has no default value")
            }
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

    private fun readValue(param: KParameter, leafPath: String): Any? {
        val classifier = param.type.classifier
        return when {
            classifier == String::class -> stringConfig.getStringOrNull(leafPath)
            classifier == Boolean::class -> booleanConfig.getBooleanOrNull(leafPath)
            classifier == Int::class -> intConfig.getIntOrNull(leafPath)
            classifier == Long::class -> longConfig.getLongOrNull(leafPath)
            classifier == Double::class -> doubleConfig.getDoubleOrNull(leafPath)
            classifier == Float::class -> stringConfig.getStringOrNull(leafPath)?.toFloatOrNull()
            // Only List<String> is supported: any other element type (e.g. List<Int>)
            // must not bind a List<String> value and falls through to the final else
            // branch, which throws UnsupportedTypeException
            classifier == List::class && param.type.arguments.singleOrNull()?.type?.classifier == String::class ->
                listConfig.getListOrNull(leafPath)
            // Nested data class: an absent nested block reads as missing, so the
            // default/null/throw rule applies; a nonempty block binds recursively
            // with the nested block path as the new prefix
            classifier is KClass<*> && classifier.primaryConstructor != null ->
                if (configCore.getMergedConfig(leafPath).isEmpty()) null
                else getObject(leafPath, classifier)
            // Every other classifier is an explicit error instead of falling through
            // to the missing-value rule; the message names the offending leaf path
            else -> throw UnsupportedTypeException(
                "Property type ${param.type} of $leafPath is not supported by getObject"
            )
        }
    }
}
