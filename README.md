# ktor-env-config

![GitHub](https://img.shields.io/github/license/sharpmind-de/ktor-env-config)
![GitHub commit activity](https://img.shields.io/github/commit-activity/m/sharpmind-de/ktor-env-config)
![GitHub top language](https://img.shields.io/github/languages/top/sharpmind-de/ktor-env-config)

The ktor-env-config library is designed to provide environment-aware configuration capabilities for [KTOR](https://ktor.io/) projects. With this library, you can easily access configuration properties from anywhere in your code.

## Branches

* main
  * Current active branch
  * built for Ktor-2.x, Ktor-3.x
  * jar file versions 2.x
* ktor-1.x
  * ktor-env-config for KTOR-1.x
  * this branch is not maintained anymore
  * jar file versions 1.x

## Description

In a typical KTOR project, different environments such as Test, Staging, and Production are used during development and deployment workflows. These environments require different configuration properties, such as database connections and external services.

To manage these configuration properties, you can use the ktor-env-config library. This library allows you to specify default configuration parameters and possible overrides in environment-specific blocks. To use this library, simply set the current environment as an environment variable, and you're all set.
## Installation

In your `build.gradle.kts`, add:

    // https://github.com/sharpmind-de/ktor-env-config
    implementation("de.sharpmind.ktor:ktor-env-config:<version>")

Replace `<version>` by one [release version](https://github.com/sharpmind-de/ktor-env-config/releases) of the project.

Current Release:

[![Maven Central](https://img.shields.io/maven-central/v/de.sharpmind.ktor/ktor-env-config)](https://search.maven.org/artifact/de.sharpmind.ktor/ktor-env-config)

    implementation("de.sharpmind.ktor:ktor-env-config:2.1.1")



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

    test {
        enableTestAccounts = true
    }

    staging {
        db_url = "https://com.company.superapp.staging-123456.firebaseio.com"
    }

    production {
        db_url = "https://com.company.superapp.prod-123456.firebaseio.com"
    }
}
```

To set the environment for ktor-env-config, use the environment variable `ENVIRONMENT` in this example. ENVIRONMENT is the default variable name for ktor-env-config.

To use a different variable, modify the line `env = ${?ENVIRONMENT}` with your preferred variable name.

The default configuration for the service can be found in the `envConfig.default` block. For other environments, override the default configuration by using a block with the same name as the environment.

If a property is not specified in a specific environment, it will use the default value. Only overwrite properties in specific environments if they differ from the default value.

### External Configuration File
In addition to defining configuration properties directly within your application.conf, you can also specify an external configuration file. This is particularly useful if you want to separate sensitive or environment-specific values from the main configuration or keep different files for local development, CI, staging, or production environments.

To use an external config file, define the file path in the envConfig.externalConfigFile key. The file should follow the same format as application.conf and can contain any supported configuration values, including environment-specific blocks. Example:

```
envConfig.externalConfigFile = "/etc/configs/my-app-config.conf"
```

This path can also be specified using environment variable substitution:

```
envConfig.externalConfigFile = ${?MY_APP_CONFIG}
```
When specified, the external config file will be parsed and merged into the existing configuration.

### Configuration Override Strategy
The ktor-env-config library uses a prioritized override strategy to determine the final configuration values. This allows you to set base defaults while also allowing environment-specific and external overrides. The order of precedence (from lowest to highest priority) is as follows:

1. Default block (envConfig.default): Base configuration that applies to all environments unless explicitly overridden.

2. Environment-specific block (envConfig.test, envConfig.staging, envConfig.production, etc.): Overrides values in the default block for a specific environment.

3. External config file (envConfig.externalConfigFile): Has the highest priority and overrides both the default and environment-specific values if present.

This means that if a value is defined in all three places, the value from the external configuration file will take effect.

This approach provides a clean and flexible way to manage configuration in different contexts without hardcoding or duplicating properties.


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

### manual initialization of EnvConfig

In certain situations, such as during unit tests, it may be necessary to manually initialize the EnvConfig object.
To do this, call `EnvConfig.initConfig()` with a configuration object that implements `ApplicationConfig`.
You can conveniently use `MapApplicationConfig` to create a map of configuration properties.

To specify the environment to be used, the config key paths should start with `envConfig.<environment>`. The environment name can be set using the ENVIRONMENT environment variable.

Here's an example code snippet to manually initialize EnvConfig:

```
// Prepare test config
val appConfig = MapApplicationConfig()
appConfig.put("envConfig.default.db.host", "dev-db-14")
appConfig.put("envConfig.default.db.dbname", "appdb")
appConfig.put("envConfig.default.db.user", "myDBUser")
appConfig.put("envConfig.default.db.password", "89qh8hh78dasf")
appConfig.put("envConfig.default.feature1.enabled", "false")

// Initialize EnvConfig
EnvConfig.initConfig(appConfig)

```

Afterward the configuration properties can be accessed via the EnvConfig object.

### get configuration properties

From anywhere in the code configuration properties can be accessed through the EnvConfig object.

The following methods are available:

#### Return value is not null, throws exception if key does not exist or if requested type can't be instantiated from specified config value.
```
EnvConfig.getBoolean(key: String): Boolean
EnvConfig.getInt(key: String): Int
EnvConfig.getList(key: String): List<String>
EnvConfig.getString(key: String): String
EnvConfig.getFile(key: String): File
EnvConfig.getUrl(key: String): URL
```
#### Return value is nullable, returns null if key does not exist or if requested type can't be instantiated from specified config value..
```
EnvConfig.getBooleanOrNull(key: String): Boolean?
EnvConfig.getIntOrNull(key: String): Int?
EnvConfig.getListOrNull(key: String): List<String>?
EnvConfig.getStringOrNull(key: String): String?
EnvConfig.getFileOrNull(key: String): File?
EnvConfig.getUrlOrNull(key: String): URL?
```
#### Return value is not null, returns default value if key does not exist or if requested type can't be instantiated from specified config value.
```
EnvConfig.getBooleanOrDefault(key: String, default: Boolean): Boolean
EnvConfig.getIntOrDefault(key: String, default: Int): Int
EnvConfig.getListOrDefault(key: String, default: List<String>): List<String>
EnvConfig.getStringOrDefault(key: String, default: String): String
EnvConfig.getFileOrDefault(key: String, default: String): File
EnvConfig.getUrlOrDefault(key: String, default: String): URL
```

### type evaluation

#### boolean

If `getBoolean*()` methods are used, the configured property value is evaluated according to the `Boolean.parseBoolean(String s)` method logic.

```
public static boolean parseBoolean(String s) {
    return ((s != null) && s.equalsIgnoreCase("true"));
}
```

So everything is considered to be false, except for the string `"true"`, evaluated case-insensitive.

### internal information ###

EnvConfig offers methods to get it's internal state. This is useful for debugging purposes.

```
# return the configured environment string
# do not base configuration decisions on this value, use the individual configuration properties and environment blocks instead
EnvConfig.getEnvironment(): String

# returns a configured external config file, or null if none is configured
EnvConfig.getExternalConfigFile(): String?
```

### set ENVIRONMENT environment variable

To set the environment for your KTOR service using the ktor-env-config library, you need to set an environment variable. The way you set this variable may vary depending on how you run your service.

The default name that ktor-env-config is looking for is `ENVIRONMENT`. You can change this by modifying the line `env = ${?ENVIRONMENT}` in the configuration file.

Here are some guidelines and pointers to help you set the environment variable correctly:


#### Command line

If you're running your service from the command line, you can set the environment variable using the export command in Unix-based systems or the set command in Windows systems. For example:

```
export ENVIRONMENT=prod
```

or

```
set ENVIRONMENT=prod
```

#### java -jar

If you're running your service as a standalone JAR file using the java -jar command, you can set the environment variable using the -D flag. For example:

```
java -DENVIRONMENT=staging -jar yourapp.jar
```

#### gradle

During development, you certainly run the project via gradle. In this case set the environment variable before running the gradle command. For example:

```
# Unix-based systems
ENVIRONMENT=test ./gradlew run
```

or

```
# Windows systems
set ENVIRONMENT=test
gradlew run

```
#### IntelliJ IDEA
If you're using IntelliJ IDEA, you can set the environment variable in the Run/Debug Configurations. Go to Run > Edit Configurations, select your run configuration, and add the environment variable in the Environment Variables field. For example:

``` 
ENVIRONMENT=staging
```

#### docker run

If you're running your service in a Docker container, you can set the environment variable using the -e flag when running the docker run command. For example:

```
docker run -e ENVIRONMENT=staging yourimage
```

For more information on the docker run command, see the official Docker documentation: [docker run](https://docs.docker.com/engine/reference/commandline/run/)  

#### docker-compose

If you're using Docker Compose to manage your Docker containers, you can set the environment variable in your docker-compose.yml file. For example:

```
...
environment:
  - ENVIRONMENT=staging
...
```

see also: [Docker Compose Environment Variables](https://docs.docker.com/compose/environment-variables/)

#### Kubernetes

To set the ENVIRONMENT variable in a Kubernetes environment, you can use Kubernetes ConfigMaps. ConfigMaps allow you to manage configuration data as key-value pairs and separate them from the container image.
Here's an example of how to create a ConfigMap with the ENVIRONMENT variable set to production:

```
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
data:
  ENVIRONMENT: production
```


## License

[MIT License](http://en.wikipedia.org/wiki/MIT_License)
