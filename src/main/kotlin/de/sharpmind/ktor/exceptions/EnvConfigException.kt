package de.sharpmind.ktor.exceptions

/**
 * Exception for not initialized EnvConfig
 */
class NotInitializedException(message: String?) : Throwable(message)

/**
 * Exception for not found keys in EnvConfig
 */
class KeyNotFoundException(message: String?) : Throwable(message)

/**
 * Exception for properties missing when binding a config block to an object
 */
class PropertyMissingException(message: String?) : Throwable(message)

/**
 * Exception for property types not supported when binding a config block to an object
 */
class UnsupportedTypeException(message: String?) : Throwable(message)
