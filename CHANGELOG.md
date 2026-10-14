# Changelog

All notable changes to this project are documented in this file.

**Breaking changes** are listed under a `### Breaking` subsection for the relevant release. They are also summarized in [GitHub Releases](https://github.com/sharpmind-de/ktor-env-config/releases) when publishing a new version.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

### Added

- List properties can be supplied as comma-separated strings (e.g. from environment variables). See README section "type evaluation → list".
- Optional config key `listDelimiter` to override the list delimiter when parsing from a string (default: comma).
- New methods `EnvConfig.getMergedConfig()` and `EnvConfig.getMergedConfig(blockPath)` return the full or selected merged config block (default block < environment block < external config file) as flat maps of dotted leaf keys (keys relative to the selected block).
- New methods `EnvConfig.getObject(propertyKey, clazz)` and `EnvConfig.getObjectOrNull(propertyKey, clazz)` — plus reified convenience overloads `EnvConfig.getObject<T>(propertyKey)` and `EnvConfig.getObjectOrNull<T>(propertyKey)` — bind a whole config block to a data class: snake_case config keys are matched to camelCase constructor parameters (with `@SerialName` override), missing values fall back to constructor defaults or `null`, `List<String>` and nested data classes are supported, and unsupported property types throw `UnsupportedTypeException`. `getObjectOrNull` returns `null` for an absent block. See README section "get configuration properties → get configuration objects".

### Fixed

- `dumpConfig()` now includes values from the external config file, which previously were missing from the dump despite having the highest priority.
- `initConfig()` now logs a warning when a configured external config file is missing or unsupported, instead of silently ignoring it. A missing external config file is by design not an error: initialization continues without external overrides.

### Breaking

- The new methods `ConfigCore.getMergedConfig()` and `ConfigCore.getMergedConfig(blockPath)` were added to the public `ConfigCore` interface **without default implementations**. Code implementing `ConfigCore` directly (other than the provided `EnvConfig`/`ConfigCoreImpl`) must add both methods to compile against this version — source-incompatible. Consumers of the `EnvConfig` object are not affected.
- Previously, requesting a list (e.g. `getList(...)`) for a value that was a string (delimiter-separated or otherwise) would throw an exception. Now such a value is parsed as a list: delimiter-separated strings become multiple elements, and a string without the delimiter produces a single-element list.

## [3.0.0] and earlier

Releases before adoption of this changelog. See [GitHub Releases](https://github.com/sharpmind-de/ktor-env-config/releases) for history.
