# Final Fix Report — `getObject` whole-branch review

## Finding 1 — absent `default` block

**Changed**

- `src/main/kotlin/de/sharpmind/ktor/config/ConfigCoreImpl.kt`: `getMergedConfig(blockPath)` now obtains the root config once and calls `rootConfig.config("default")` only when `rootConfig.propertyOrNull("default")` is non-null. This makes an absent default block an intentional empty contribution while leaving the environment-block catch and external-config merge unchanged. The no-argument overload still delegates to this shared implementation.
- `src/test/kotlin/de/sharpmind/ktor/TestConfigs.kt`: added fixture O with an environment-only `app` block, no default block, and external merge leaves.
- `src/test/kotlin/de/sharpmind/ktor/TestEnvConfigObject.kt`: added regressions for `getObjectOrNull` returning null for an absent block without a default, and for binding an environment-only nested object with assertions on the actual host, port, and tags.
- `src/test/kotlin/de/sharpmind/ktor/TestEnvConfigMerged.kt`: added coverage that both no-argument and block-path merges return environment and external leaves without a default block.

The guard was verified against the Ktor 3.3.0 source for `HoconApplicationConfig`: `propertyOrNull` uses `Config.hasPath` and returns a config value for an object-valued path, while returning null when absent.

**RED evidence (before production fix)**

- `./gradlew test --tests "de.sharpmind.ktor.TestEnvConfigObject"`: `15 tests completed, 2 failed`; the absent-block test failed with `com.typesafe.config.ConfigException$Missing` at `TestEnvConfigObject.kt:98`, and the nested environment-only binding failed with `PropertyMissingException` at `TestEnvConfigObject.kt:86`.
- `./gradlew test --tests "de.sharpmind.ktor.TestEnvConfigMerged"`: `6 tests completed, 1 failed`; the no-default merge test failed with `com.typesafe.config.ConfigException$Missing` at `TestEnvConfigMerged.kt:22`.

These failures reproduce the raw missing-config exception and the swallowed nested-block existence error identified in review.

**GREEN evidence (after fix)**

- `./gradlew test --tests "de.sharpmind.ktor.TestEnvConfigObject" --tests "de.sharpmind.ktor.TestEnvConfigMerged"`: successful; XML reports `TestEnvConfigObject` 15 tests / 0 failures and `TestEnvConfigMerged` 6 tests / 0 failures.
- `./gradlew test`: `BUILD SUCCESSFUL`; XML totals: 49 tests, 0 failures, 0 errors.

## Finding 2 — README and CHANGELOG wording

**Changed**

- `README.md`: clarified that `getObjectOrNull` returns null for a block with no leaf keys, whether absent or empty; qualified the pre-initialization statement to account for zero-parameter data classes returned by `getObject` without reading config.
- `CHANGELOG.md`: changed the return description from an absent block to a block with no leaf keys (absent or empty).

## Validation and commit

- `git diff --check`: clean.
- Full suite: `./gradlew test` — successful, 49 tests / 0 failures / 0 errors.
- Fix + tests + README/CHANGELOG commit: `188248c`.
