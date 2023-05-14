package de.sharpmind.ktor.exceptions

/**
 * Exception for not initialized EnvConfig
 */
class NotInitializedException(message: String?) : Throwable(message)

/**
 * Exception for not found keys in EnvConfig
 */
class KeyNotFoundException(message: String?) : Throwable(message)
