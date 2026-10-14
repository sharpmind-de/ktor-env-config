# getObject Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let callers ask `EnvConfig` for an entire config block as a caller-supplied data class, e.g. `EnvConfig.getObject<PollingConfig>("polling")`, filled from default block, environment block, and external config file.

**Architecture:** A new `ObjectConfig` interface (root package, alongside the existing typed interfaces) is delegated from the `EnvConfig` object to a new `ObjectConfigImpl`, following the library's existing per-type-impl pattern. Binding iterates the data class's primary constructor parameters (`kotlin-reflect`), resolves each parameter's leaf key through the existing per-key precedence chain (reusing `StringConfigImpl`/`IntConfigImpl`/… conversion logic), and instantiates via `callBy`, omitting parameters that have constructor defaults.

**Tech Stack:** Kotlin 2.2.20 / JVM 17, Ktor 3.3.0 `ApplicationConfig`, kotlin-reflect (new dependency), JUnit 4 + kotlin-test.

**Spec:** https://github.com/sharpmind-de/ktor-env-config/issues/12 — design agreed with the maintainer on 2026-10-14; all decisions are restated under Global Constraints below, so this plan is self-contained.

**Prerequisite:** Issue #14 must be merged first — it provides `getMergedConfig(blockPath: String): Map<String, String>` on `ConfigCore`/`EnvConfig` (flat leaf map of one block, keys relative to the block, deep-merged across default < environment < external). Task 1 verifies this gate before any code is written.

## Global Constraints

- New dependency in `build.gradle.kts`: `implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlin_version")` (kotlin_version = 2.2.20). No kotlinx-serialization dependency — `@SerialName` is detected reflectively by annotation name.
- Supported property types (v1): `String`, `Boolean`, `Int`, `Long`, `Double`, `Float`, `List<String>`, nested data classes, and nullable versions of these. Any other type → `UnsupportedTypeException`.
- Missing-value rule, applied per constructor parameter: (1) constructor default if the parameter `isOptional`; (2) `null` if the parameter type is nullable; (3) otherwise throw `PropertyMissingException`. An unparsable value counts as missing. `getObject` never returns null; `getObjectOrNull` returns null **iff** the block has no leaf keys.
- Key naming: `@SerialName("...")` on the parameter wins outright; otherwise take the first key that exists among `[param.name, snakeCase(param.name)]`, where `snakeCase(s) = s.replace(Regex("([a-z0-9])([A-Z])"), "$1_$2").lowercase()` (`statusEnabled` → `status_enabled`). For error messages, the fallback key is the snake_case form.
- Precedence inside a block follows the existing `getPropertyValue` chain: default block < environment block < external config file.
- New exceptions live in `src/main/kotlin/de/sharpmind/ktor/exceptions/EnvConfigException.kt` and extend `Throwable`, matching `NotInitializedException`/`KeyNotFoundException`. Messages contain the full leaf path (e.g. `polling.status_enabled`).
- `NotInitializedException` must propagate from `getObject*` when EnvConfig was not initialized (never be swallowed by the missing-value rule).
- Follow the existing code patterns: interface in root-package `interfaces.kt`, impl class in `de.sharpmind.ktor.config`, delegation from the `EnvConfig` object, test-only fixtures in `src/test`.
- TDD strictly: every step's test is written and observed failing before implementation. `./gradlew test` (full suite, currently 31 tests) must stay green at every task boundary. One commit per task.

## Review Focus

1. **Three-source precedence inside a block** — a key overridden by the external config file must beat the environment block, which beats default. Expected: `getObject` on a block present in all three sources returns the external value. → pinned by Task 2's `test_getObject_fullPrecedenceAndSnakeCaseNaming`.
2. **Missing required property** — no value in any source, no constructor default, non-nullable → `PropertyMissingException` whose message contains the leaf path; with a default → default; nullable → null. → pinned by Task 3's `test_getObject_missingProperty_defaultOrNullOrThrow`.
3. **Key naming mismatch** — config `status_enabled` vs property `statusEnabled` must bind; `@SerialName` must override the automatic rule. → pinned by Task 2's naming test and `test_getObject_serialNameOverride`.
4. **Nested data class** — nested block deep-merged across sources; a present-but-incomplete nested block throws with the *nested* leaf path; an absent nested block follows the param missing-rule. → pinned by Task 5's nested tests.
5. **Unparsable value and uninitialized state** — `statusFreqSecs = "abc"` with a default falls back to the default (treated as missing); calling `getObject` before `initConfig` throws `NotInitializedException`, not `PropertyMissingException`. → pinned by Task 3's bad-value test and Task 6's `_TestUninitialized_getObject` (runs first via `NAME_ASCENDING`, as in `TestEnvConfig`).

---

### Task 1: ObjectConfig interface + happy-path getObject

**Files:**
- Modify: `src/main/kotlin/de/sharpmind/ktor/config/interfaces.kt` (append after `UrlConfig`)
- Create: `src/main/kotlin/de/sharpmind/ktor/config/ObjectConfigImpl.kt`
- Modify: `src/main/kotlin/de/sharpmind/ktor/EnvConfig.kt` (object declaration + delegation)
- Modify: `src/main/kotlin/de/sharpmind/ktor/exceptions/EnvConfigException.kt` (add `PropertyMissingException`)
- Modify: `build.gradle.kts` (kotlin-reflect dependency)
- Modify: `src/test/kotlin/de/sharpmind/ktor/TestConfigs.kt` (fixture `testConfigJ`)
- Create: `src/test/kotlin/de/sharpmind/ktor/TestEnvConfigObject.kt`

**Interfaces:**
- Consumes: `ConfigCoreImpl.getMergedConfig(blockPath: String): Map<String, String>` (from #14), `ConfigCoreImpl.getPropertyValue(propertyKey: String)`, existing `StringConfigImpl`/`BooleanConfigImpl`/`IntConfigImpl`/`LongConfigImpl`/`DoubleConfigImpl`/`ListConfigImpl` constructors `(ConfigCoreImpl)`.
- Produces:
  - `interface ObjectConfig { fun <T : Any> getObject(propertyKey: String, clazz: KClass<T>): T }` (root package; `getObjectOrNull` added in Task 4)
  - `class ObjectConfigImpl(private val configCore: ConfigCoreImpl) : ObjectConfig` in `de.sharpmind.ktor.config`
  - `class PropertyMissingException(message: String?) : Throwable(message)` in `de.sharpmind.ktor.exceptions`
  - `EnvConfig` implements `ObjectConfig` and delegates to `ObjectConfigImpl`
  - Test fixture: `data class PlainPolling(val statusFreqSecs: Int, val label: String)` and `val testConfigJ` (block `poll`)

- [ ] **Step 1: Verify prerequisite #14 is merged**

Run: `grep -n "getMergedConfig(blockPath" src/main/kotlin/de/sharpmind/ktor/config/interfaces.kt`
Expected: a match. If not, stop — implement #14 first; nothing in this plan works without the block-path map.

- [ ] **Step 2: Write the failing test**

```kotlin
// TestEnvConfigObject.kt, @FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Test
fun test_getObject_fillsClassFromBlock() {
    println("Running test: " + object {}.javaClass.enclosingMethod.name)
    val obj = EnvConfig.initConfig(testConfigJ).getObject("poll", PlainPolling::class)
    assertEquals(300, obj.statusFreqSecs)   // env block overrides default (600)
    assertEquals("poll", obj.label)         // default block value
}
```

```kotlin
// TestConfigs.kt
data class PlainPolling(val statusFreqSecs: Int, val label: String)

val testConfigJ = HoconApplicationConfig(ConfigFactory.parseString("""
    envConfig {
        env = testEnv
        default {
            poll {
                statusFreqSecs = 600
                label = "poll"
            }
        }
        testEnv {
            poll {
                statusFreqSecs = 300
            }
        }
    }
""".trimIndent()))
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew test --tests "de.sharpmind.ktor.TestEnvConfigObject"`
Expected: FAIL to compile with `Unresolved reference 'getObject'`

- [ ] **Step 4: Add `PropertyMissingException`, `ObjectConfig`, and kotlin-reflect**

- `EnvConfigException.kt`: `class PropertyMissingException(message: String?) : Throwable(message)`
- `interfaces.kt`: `interface ObjectConfig { fun <T : Any> getObject(propertyKey: String, clazz: KClass<T>): T }` with `import kotlin.reflect.KClass`
- `build.gradle.kts` dependencies: add `implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlin_version")`

- [ ] **Step 5: Implement `ObjectConfigImpl.getObject`**

Signature as above. Instantiate the existing converters as fields: `private val stringConfig = StringConfigImpl(configCore)` (same for `BooleanConfigImpl`, `IntConfigImpl`, `LongConfigImpl`, `DoubleConfigImpl`, `ListConfigImpl`). Body: `clazz.primaryConstructor ?: throw KeyNotFoundException("Class ${clazz.simpleName} has no primary constructor")`, then for each parameter resolve the leaf path `"$propertyKey.${param.name}"` via the converters' `getXOrNull(leafPath)` matching the parameter's classifier (`String`, `Boolean`, `Int`, `Long`, `Double`, `Float` via `getStringOrNull(...)?.toFloatOrNull()`, `List` of `String`; an unmatched classifier reads as null — i.e. missing, for now); null → `throw PropertyMissingException("Property $leafPath does not exist and has no default value")`; build `args: Map<KParameter, Any>` and return `ctor.callBy(args)`. The snake_case/camelCase key candidates, `isOptional`/nullable handling, `@SerialName`, and nested recursion come in later tasks — Task 1 reads only `param.name` and requires every value present.

- [ ] **Step 6: Delegate from EnvConfig**

Add `ObjectConfig` to the `EnvConfig` object's supertypes, `private val objectConfig by lazy { ObjectConfigImpl(configCore) }`, and `override fun <T : Any> getObject(propertyKey: String, clazz: KClass<T>): T = objectConfig.getObject(propertyKey, clazz)`.

- [ ] **Step 7: Run test to verify it passes, then full suite**

Run: `./gradlew test --tests "de.sharpmind.ktor.TestEnvConfigObject"` → PASS
Run: `./gradlew test` → PASS (all)

- [ ] **Step 8: Commit**

```bash
git add src/main/kotlin/de/sharpmind/ktor/config/interfaces.kt \
        src/main/kotlin/de/sharpmind/ktor/config/ObjectConfigImpl.kt \
        src/main/kotlin/de/sharpmind/ktor/EnvConfig.kt \
        src/main/kotlin/de/sharpmind/ktor/exceptions/EnvConfigException.kt \
        build.gradle.kts \
        src/test/kotlin/de/sharpmind/ktor/TestConfigs.kt \
        src/test/kotlin/de/sharpmind/ktor/TestEnvConfigObject.kt
git commit -m "Add getObject() returning a config block as a data class"
```

---

### Task 2: snake_case naming + three-source precedence + @SerialName

**Files:**
- Modify: `src/main/kotlin/de/sharpmind/ktor/config/ObjectConfigImpl.kt`
- Modify: `src/test/kotlin/de/sharpmind/ktor/TestConfigs.kt` (fixture `testConfigK`)
- Create: `src/test/resources/test-object.conf`
- Modify: `src/test/kotlin/de/sharpmind/ktor/TestEnvConfigObject.kt`

**Interfaces:**
- Consumes: Task 1's `ObjectConfigImpl.getObject`; #14's `getMergedConfig(blockPath)`.
- Produces: private helper `ObjectConfigImpl.snakeCase(name: String): String` and per-parameter key selection (`@SerialName` value, else first existing of `[param.name, snakeCase(param.name)]`); test fixture `data class PollingConfig` (the maintainer's real example) + `val testConfigK`.

- [ ] **Step 1: Write the failing tests**

```kotlin
// TestEnvConfigObject.kt
@Test
fun test_getObject_fullPrecedenceAndSnakeCaseNaming() {
    println("Running test: " + object {}.javaClass.enclosingMethod.name)
    val obj = EnvConfig.initConfig(testConfigK).getObject("polling", PollingConfig::class)
    assertEquals(999, obj.statusFreqSecs)     // external file beats env (300) beats default (600)
    assertTrue(obj.statusEnabled)             // default block
    assertEquals(1200, obj.staleAfterSecs)    // default block
}

@Test
fun test_getObject_serialNameOverride() {
    println("Running test: " + object {}.javaClass.enclosingMethod.name)
    val obj = EnvConfig.initConfig(testConfigK).getObject("odd", OddConfig::class)
    assertEquals(7, obj.weirdKey)
}
```

```kotlin
// TestConfigs.kt
data class PollingConfig(
    val statusEnabled: Boolean = false,
    val versionEnabled: Boolean = false,
    val statusFreqSecs: Int = 30,
    val versionFreqSecs: Int = 60,
    val statusInitialDelaySecs: Int = 0,
    val versionInitialDelaySecs: Int = 0,
    val staleAfterSecs: Int = 300
)

data class OddConfig(@SerialName("odd_name") val weirdKey: Int)

val testConfigK = HoconApplicationConfig(ConfigFactory.parseString("""
    envConfig {
        env = testEnv
        externalConfigFile = "src/test/resources/test-object.conf"
        default {
            polling {
                status_enabled    = true
                version_enabled   = true
                status_freq_secs  = 600
                version_freq_secs = 3600
                status_initial_delay_secs = 60
                version_initial_delay_secs = 120
                stale_after_secs  = 1200
            }
            odd { odd_name = 7 }
        }
        testEnv {
            polling { status_freq_secs = 300 }
        }
    }
""".trimIndent()))
```

```hocon
// src/test/resources/test-object.conf — external config file, highest priority
polling {
    status_freq_secs = 999
}
```

Note: `OddConfig` needs `import kotlinx.serialization.SerialName` in the test — acceptable because ktor already brings `kotlinx-serialization-core` onto the test classpath; production code must still detect the annotation **by name** only.

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests "de.sharpmind.ktor.TestEnvConfigObject"`
Expected: FAIL — `statusFreqSecs` receives the camelCase-key lookup only, so `PropertyMissingException: ...polling.statusFreqSecs...` (snake key not tried).

- [ ] **Step 3: Implement key selection in `ObjectConfigImpl`**

Add `private fun snakeCase(name: String): String = name.replace(Regex("([a-z0-9])([A-Z])"), "$1_$2").lowercase()`. Per parameter: if an annotation whose `annotationClass.qualifiedName == "kotlinx.serialization.SerialName"` exists, its `value` (read via `annotation.javaClass.getMethod("value").invoke(annotation)`) is the only candidate; otherwise candidates are `[param.name, snakeCase(param.name!!)]`, first whose `getXOrNull` leaf resolves wins; if none resolve, the error key is the snake_case form.

- [ ] **Step 4: Run tests to verify they pass, then full suite**

Run: `./gradlew test --tests "de.sharpmind.ktor.TestEnvConfigObject"` → PASS
Run: `./gradlew test` → PASS (all)

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/de/sharpmind/ktor/config/ObjectConfigImpl.kt \
        src/test/kotlin/de/sharpmind/ktor/TestConfigs.kt \
        src/test/kotlin/de/sharpmind/ktor/TestEnvConfigObject.kt \
        src/test/resources/test-object.conf
git commit -m "Map snake_case config keys and SerialName to camelCase properties in getObject"
```

---

### Task 3: missing-property rule (default → null → throw) and unparsable values

**Files:**
- Modify: `src/main/kotlin/de/sharpmind/ktor/config/ObjectConfigImpl.kt`
- Modify: `src/test/kotlin/de/sharpmind/ktor/TestConfigs.kt` (fixture `testConfigL`)
- Modify: `src/test/kotlin/de/sharpmind/ktor/TestEnvConfigObject.kt`

**Interfaces:**
- Consumes: Task 1/2 `ObjectConfigImpl`; `PropertyMissingException`.
- Produces: missing-value rule inside `getObject` (uses `KParameter.isOptional`, `param.type.isMarkedNullable`); fixture `data class DefaultsConfig(val present: Int, val absent: Int = 42, val maybe: String?, val bad: Int = 42)`, `data class RequiredConfig(val requiredValue: Int)`, `data class AllDefaultsConfig(val x: Int = 5)` and `val testConfigL`.

- [ ] **Step 1: Write the failing tests**

```kotlin
@Test
fun test_getObject_missingProperty_defaultOrNullOrThrow() {
    println("Running test: " + object {}.javaClass.enclosingMethod.name)
    val cfg = EnvConfig.initConfig(testConfigL)

    // present -> bound; absent with default -> default; absent nullable -> null; unparsable -> default
    val obj = cfg.getObject("defaults", DefaultsConfig::class)
    assertEquals(1, obj.present)
    assertEquals(42, obj.absent)
    assertNull(obj.maybe)
    assertEquals(42, obj.bad)                    // testConfigL sets bad = "abc"

    // absent, no default, non-nullable -> PropertyMissingException naming the leaf path
    val ex = assertFailsWith<PropertyMissingException> { cfg.getObject("required", RequiredConfig::class) }
    assertTrue(ex.message!!.contains("required.requiredValue"), "message was: ${ex.message}")

    // getObject never returns null: absent block with all defaults -> defaults object
    assertEquals(5, cfg.getObject("noSuchBlock", AllDefaultsConfig::class).x)
}
```

```kotlin
// TestConfigs.kt
data class DefaultsConfig(val present: Int, val absent: Int = 42, val maybe: String?, val bad: Int = 42)
data class RequiredConfig(val requiredValue: Int)
data class AllDefaultsConfig(val x: Int = 5)

val testConfigL = HoconApplicationConfig(ConfigFactory.parseString("""
    envConfig {
        env = testEnv
        default {
            defaults { present = 1, bad = "abc" }
            required { }
            partial { other = 1 }     # block with leaves but without requiredValue — used by Task 4
        }
        testEnv { }
    }
""".trimIndent()))
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests "de.sharpmind.ktor.TestEnvConfigObject"`
Expected: FAIL — `absent` throws `PropertyMissingException` (optional/nullable handling not implemented) and/or `maybe` throws.

- [ ] **Step 3: Implement the missing-value rule**

Per parameter, after the read attempt: `if (value != null) args[param] = value` (wrap the read in try/catch: catch `Throwable`, rethrow if it is `NotInitializedException`, otherwise treat as missing — this makes unparsable values count as missing); `else if (param.isOptional)` omit the parameter from `args`; `else if (param.type.isMarkedNullable)` `args[param] = null`; `else throw PropertyMissingException("Property $leafPath does not exist and has no default value")`.

- [ ] **Step 4: Run tests to verify they pass, then full suite**

Run: `./gradlew test --tests "de.sharpmind.ktor.TestEnvConfigObject"` → PASS
Run: `./gradlew test` → PASS (all)

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/de/sharpmind/ktor/config/ObjectConfigImpl.kt \
        src/test/kotlin/de/sharpmind/ktor/TestConfigs.kt \
        src/test/kotlin/de/sharpmind/ktor/TestEnvConfigObject.kt
git commit -m "Apply default-or-throw rule for missing properties in getObject"
```

---

### Task 4: getObjectOrNull

**Files:**
- Modify: `src/main/kotlin/de/sharpmind/ktor/config/interfaces.kt`
- Modify: `src/main/kotlin/de/sharpmind/ktor/config/ObjectConfigImpl.kt`
- Modify: `src/main/kotlin/de/sharpmind/ktor/EnvConfig.kt`
- Modify: `src/test/kotlin/de/sharpmind/ktor/TestEnvConfigObject.kt`

**Interfaces:**
- Consumes: Task 1–3 binding; `configCore.getMergedConfig(blockPath: String)` for block existence.
- Produces: `fun <T : Any> getObjectOrNull(propertyKey: String, clazz: KClass<T>): T?` on `ObjectConfig` and `EnvConfig`.

- [ ] **Step 1: Write the failing test**

```kotlin
@Test
fun test_getObjectOrNull_blockExistence() {
    println("Running test: " + object {}.javaClass.enclosingMethod.name)
    val cfg = EnvConfig.initConfig(testConfigL)

    // absent block -> null
    assertNull(cfg.getObjectOrNull("noSuchBlock", AllDefaultsConfig::class))
    // present block -> filled object
    assertEquals(1, cfg.getObjectOrNull("defaults", DefaultsConfig::class)!!.present)
    // present block (has leaves) with a required property missing -> still throws;
    // null means "block absent", not "block invalid"
    assertFailsWith<PropertyMissingException> { cfg.getObjectOrNull("partial", RequiredConfig::class) }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "de.sharpmind.ktor.TestEnvConfigObject"`
Expected: FAIL to compile with `Unresolved reference 'getObjectOrNull'`

- [ ] **Step 3: Implement `getObjectOrNull`**

Add to `ObjectConfig`: `fun <T : Any> getObjectOrNull(propertyKey: String, clazz: KClass<T>): T?`. In `ObjectConfigImpl`: `if (configCore.getMergedConfig(propertyKey).isEmpty()) null else getObject(propertyKey, clazz)`. Delegate from `EnvConfig`.

- [ ] **Step 4: Run test to verify it passes, then full suite**

Run: `./gradlew test --tests "de.sharpmind.ktor.TestEnvConfigObject"` → PASS
Run: `./gradlew test` → PASS (all)

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/de/sharpmind/ktor/config/interfaces.kt \
        src/main/kotlin/de/sharpmind/ktor/config/ObjectConfigImpl.kt \
        src/main/kotlin/de/sharpmind/ktor/EnvConfig.kt \
        src/test/kotlin/de/sharpmind/ktor/TestEnvConfigObject.kt
git commit -m "Add getObjectOrNull() returning null for absent config blocks"
```

---

### Task 5: List<String> and nested data classes

**Files:**
- Modify: `src/main/kotlin/de/sharpmind/ktor/config/ObjectConfigImpl.kt`
- Modify: `src/test/kotlin/de/sharpmind/ktor/TestConfigs.kt` (fixture `testConfigM`)
- Modify: `src/test/kotlin/de/sharpmind/ktor/TestEnvConfigObject.kt`

**Interfaces:**
- Consumes: Task 1–4; `ListConfigImpl.getListOrNull` (handles HOCON lists and comma-separated strings incl. `listDelimiter`); recursive self-call to the binding routine.
- Produces: `data class DbConfig(val host: String, val port: Int = 5432)`, `data class AppBlock(val db: DbConfig, val tags: List<String>)`, `val testConfigM`.

- [ ] **Step 1: Write the failing tests**

```kotlin
@Test
fun test_getObject_listAndNestedBlock() {
    println("Running test: " + object {}.javaClass.enclosingMethod.name)
    val obj = EnvConfig.initConfig(testConfigM).getObject("app", AppBlock::class)

    // list: env block's comma-separated string wins over default's HOCON list
    assertEquals(listOf("x", "y", "z"), obj.tags)
    // nested block: host from default, port overridden by env block (deep chain)
    assertEquals("db.internal", obj.db.host)
    assertEquals(1234, obj.db.port)
}

@Test
fun test_getObject_nestedMissingRequiredThrows() {
    println("Running test: " + object {}.javaClass.enclosingMethod.name)
    // nested block present (has leaves) but a required nested property is missing
    val ex = assertFailsWith<PropertyMissingException> {
        EnvConfig.initConfig(testConfigM).getObject("nest", NestBlock::class)
    }
    assertTrue(ex.message!!.contains("nest.db.host"), "message was: ${ex.message}")
}

@Test
fun test_getObject_nestedMissingUsesDefaults() {
    println("Running test: " + object {}.javaClass.enclosingMethod.name)
    // nested block absent entirely + nullable nested param -> null
    val obj = EnvConfig.initConfig(testConfigM).getObject("bare", BareBlock::class)
    assertNull(obj.db)
}
```

```kotlin
// TestConfigs.kt
data class DbConfig(val host: String, val port: Int = 5432)
data class AppBlock(val db: DbConfig, val tags: List<String>)
data class BareBlock(val db: DbConfig?)
data class NestBlock(val db: DbConfig)

val testConfigM = HoconApplicationConfig(ConfigFactory.parseString("""
    envConfig {
        env = testEnv
        default {
            app {
                db { host = "db.internal" }
                tags = ["a", "b", "c"]
            }
            bare { }
            nest { db { port = 1 } }
        }
        testEnv {
            app {
                db { port = 1234 }
                tags = "x,y,z"
            }
        }
    }
""".trimIndent()))
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests "de.sharpmind.ktor.TestEnvConfigObject"`
Expected: FAIL — `PropertyMissingException` with message `Property app.db does not exist...` (nested data classes not yet supported; `test_getObject_nestedMissingRequiredThrows` fails the same way).

- [ ] **Step 3: Implement recursion for nested data classes**

In the type dispatch, after the scalar/`List` cases: if the classifier is a `KClass` with a primary constructor, then — if `configCore.getMergedConfig(leafPath).isEmpty()` treat as missing (null, letting the Task 3 rule decide); otherwise recurse: bind that class against `leafPath` as the new block prefix and return the instance. Nested binding throws `PropertyMissingException` with the nested leaf path (e.g. `app.db.host`) when a required nested property is missing.

- [ ] **Step 4: Run tests to verify they pass, then full suite**

Run: `./gradlew test --tests "de.sharpmind.ktor.TestEnvConfigObject"` → PASS
Run: `./gradlew test` → PASS (all)

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/de/sharpmind/ktor/config/ObjectConfigImpl.kt \
        src/test/kotlin/de/sharpmind/ktor/TestConfigs.kt \
        src/test/kotlin/de/sharpmind/ktor/TestEnvConfigObject.kt
git commit -m "Support List<String> and nested data classes in getObject"
```

---

### Task 6: unsupported types, uninitialized guard, reified convenience, docs

**Files:**
- Modify: `src/main/kotlin/de/sharpmind/ktor/exceptions/EnvConfigException.kt` (add `UnsupportedTypeException`)
- Modify: `src/main/kotlin/de/sharpmind/ktor/config/ObjectConfigImpl.kt`
- Modify: `src/main/kotlin/de/sharpmind/ktor/EnvConfig.kt` (reified members)
- Modify: `src/test/kotlin/de/sharpmind/ktor/TestEnvConfigObject.kt`
- Modify: `src/test/kotlin/de/sharpmind/ktor/TestConfigs.kt` (fixture `WeirdTypeConfig`)
- Modify: `README.md`, `CHANGELOG.md`

**Interfaces:**
- Consumes: Tasks 1–5.
- Produces: `class UnsupportedTypeException(message: String?) : Throwable(message)`; reified members `EnvConfig.getObject<T>(propertyKey: String): T` and `EnvConfig.getObjectOrNull<T>(propertyKey: String): T?`.

- [ ] **Step 1: Write the failing tests**

```kotlin
// Runs first within the class (NAME_ASCENDING), while the singleton is still uninitialized
@Test
fun _TestUninitialized_getObject() {
    println("Running test: " + object {}.javaClass.enclosingMethod.name)
    assertFailsWith<NotInitializedException> { EnvConfig.getObject("polling", PollingConfig::class) }
}

@Test
fun test_getObject_unsupportedTypeThrows() {
    println("Running test: " + object {}.javaClass.enclosingMethod.name)
    val ex = assertFailsWith<UnsupportedTypeException> {
        EnvConfig.initConfig(testConfigL).getObject("weirdtype", WeirdTypeConfig::class)
    }
    assertTrue(ex.message!!.contains("weirdtype"), "message was: ${ex.message}")
}

@Test
fun test_getObject_reifiedConvenience() {
    println("Running test: " + object {}.javaClass.enclosingMethod.name)
    val viaReified = EnvConfig.initConfig(testConfigK).getObject<PollingConfig>("polling")
    assertEquals(999, viaReified.statusFreqSecs)
    assertNull(EnvConfig.getObjectOrNull<AllDefaultsConfig>("noSuchBlock"))
}
```

```kotlin
// TestConfigs.kt
data class WeirdTypeConfig(val unsupported: Map<String, String>)
```

Note: `_TestUninitialized_getObject` relies on this test class running in its own JVM (`forkEvery = 1`, already configured) and on `@FixMethodOrder(MethodSorters.NAME_ASCENDING)` so it executes before any other test initializes the `EnvConfig` singleton — same pattern as `TestEnvConfig._TestUninitializedConfig_getA`.

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests "de.sharpmind.ktor.TestEnvConfigObject"`
Expected: FAIL — `UnsupportedTypeException` unresolved, `getObject<PollingConfig>` unresolved (no reified member yet).

- [ ] **Step 3: Implement**

- In the type dispatch, add an explicit final `else ->` branch that throws `UnsupportedTypeException("Property type ... of $leafPath is not supported by getObject")` (until now, unmatched classifiers fell through to the missing-value rule — the message must contain the leaf path).
- On the `EnvConfig` object: `inline fun <reified T : Any> getObject(propertyKey: String): T = getObject(propertyKey, T::class)` and the `getObjectOrNull` twin.

- [ ] **Step 4: Run test to verify it passes, then full suite**

Run: `./gradlew test --tests "de.sharpmind.ktor.TestEnvConfigObject"` → PASS
Run: `./gradlew test` → PASS (all)

- [ ] **Step 5: Document in README and CHANGELOG**

README: new subsection under "get configuration properties" titled `#### get configuration objects` showing `EnvConfig.getObject<PollingConfig>("polling")`, the snake_case → camelCase rule, `@SerialName` override, the default→null→throw rule, supported types, and `getObjectOrNull` for absent blocks. CHANGELOG: entry under `## [Unreleased] → ### Added`.

- [ ] **Step 6: Commit**

```bash
git add src/main/kotlin/de/sharpmind/ktor/exceptions/EnvConfigException.kt \
        src/main/kotlin/de/sharpmind/ktor/config/ObjectConfigImpl.kt \
        src/main/kotlin/de/sharpmind/ktor/EnvConfig.kt \
        src/test/kotlin/de/sharpmind/ktor/TestEnvConfigObject.kt \
        src/test/kotlin/de/sharpmind/ktor/TestConfigs.kt \
        README.md CHANGELOG.md
git commit -m "Guard unsupported types, add reified getObject, document object binding"
```
