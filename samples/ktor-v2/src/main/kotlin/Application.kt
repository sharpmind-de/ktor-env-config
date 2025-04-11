package de.sharpmind.ktor

import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    // initialize EnvConfig with the environment config
    EnvConfig.initConfig(environment.config)

    println("EnvConfig: ${EnvConfig.getEnvironment()}")

    configureRouting()
}
