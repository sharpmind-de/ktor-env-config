# ktor-env-config

![GitHub](https://img.shields.io/github/license/sharpmind-de/ktor-env-config)
![GitHub](https://img.shields.io/github/workflow/status/sharpmind-de/ktor-env-config/CI)

A library for environment aware configuration in [KTOR](https://ktor.io/).

ktor-env-config allows access to configuration properties from everywhere in the code.

## Branches

* main branch - ktor-env-config for KTOR-2.x (Versions 2.x)
* ktor-1.x branch - ktor-env-config for KTOR-1.x (Versions 1.x)
   * this branch is not maintained anymore

## Description

Usually a KTOR project runs in different environments during it's development and deployment workflow. Test, Staging and Production environments are often used.

The same application has to use different properties in different environments. Database connections and similar external services are examples. 

ktor-env-config solves the issue of different configuration properties per environment. You can specify the default configuration parameters and possible overwrites in env specific blocks.
From outside you just need to set the current environment as a environment variable and you are all set.

## Installation

In your `build.gradle.kts`, add:

    // https://github.com/sharpmind-de/ktor-env-config
    implementation("de.sharpmind.ktor:ktor-env-config:<version>")

Replace `<version>` by one [release version](https://github.com/sharpmind-de/ktor-env-config/releases) of the project.

## Usage

The following example service has to configure different firebase URLs for the environments
* test
* staging
* production

### sample application.conf

```
ktor {
    deployment {
        port = 8080
        port = ${?PORT}
    }
    application {
        modules = [com.company.superapp.ApplicationKt.module]
    }
}

envConfig{
    env = default
    env = ${?ENVIRONMENT}

    default {                                               
        db_url = "https://com.company.superapp.test-123456.firebaseio.com"
        enableTestAccounts = false
    }

    dev {
        enableTestAccounts = true
    }

    staging {
        db_url = "https://com.company.superapp.staging-123456.firebaseio.com"
    }

    prod {
        db_url = "https://com.company.superapp.prod-123456.firebaseio.com"
    }
}
```

In this example the variable that sets the environment is called "ENVIRONMENT". If you want to use a different variable you have to change the ```env = ${?ENVIRONMENT}``` line accordingly.

The default configuration for the service goes to the envConfig.default block.
Any overrides for other environments go to blocks with the same name as the environment.

Properties that are not specified in specific environments are using the default values. You just overwrite properties in specific environments if they differ from default.

### initialize EnvConfig

The EnvConfig object has to be initialized with the ktor configuration. A good place is the ```Application.module()``` extension function.

in Application.kt:

```
fun Application.module(testing: Boolean = false) {
    ...
    EnvConfig.initConfig(environment.config)
    ...
}
```

### get configuration properties

From anywhere in the code configuration properties can be accessed through the EnvConfig object.

The following methods are available:

#### Return value is not null, throws exception if key does not exist.
```
EnvConfig.getBoolean(key: String): Boolean
EnvConfig.getInt(key: String): Int
EnvConfig.getList(key: String): List<String>
EnvConfig.getString(key: String): String
```
#### Return value is nullable, returns null if key does not exist.
```
EnvConfig.getBooleanOrNull(key: String): Boolean?
EnvConfig.getIntOrNull(key: String): Int?
EnvConfig.getListOrNull(key: String): List<String>?
EnvConfig.getStringOrNull(key: String): String?
```
#### Return value is not null, returns default value if key does not exist
```
EnvConfig.getBooleanOrDefault(key: String, default: Boolean): Boolean
EnvConfig.getIntOrDefault(key: String, default: Int): Int
EnvConfig.getListOrDefault(key: String, default: List<String>): List<String>
EnvConfig.getStringOrDefault(key: String, default: String): String
```

### type evaluation

#### boolean
if getBoolean*() methods are used the configured property value is evaluated according to the Boolean.parseBoolean(String s) method logic

```
public static boolean parseBoolean(String s) {
    return ((s != null) && s.equalsIgnoreCase("true"));
}
```

So everything is considered to be false, except for the string "true", evaluated case insensitive.

### set environment

The environment has to be set via environment variables. Depending on how you run your service you have to set it in a different way.

Here are some guidelines and pointers:

#### java -jar

java -DENVIRONMENT=staging -jar yourapp.jar

#### docker run

see also https://docs.docker.com/engine/reference/commandline/run/

#### docker-compose

see also https://docs.docker.com/compose/environment-variables/

## License

[MIT License](http://en.wikipedia.org/wiki/MIT_License)
