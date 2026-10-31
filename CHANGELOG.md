# Changelog

All notable changes to this project are documented in this file.

**Breaking changes** are listed under a `### Breaking` subsection for the relevant release. They are also summarized in [GitHub Releases](https://github.com/sharpmind-de/ktor-env-config/releases) when publishing a new version.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

### Added

- List properties can be supplied as comma-separated strings (e.g. from environment variables). See README section "type evaluation → list".
- Optional config key `listDelimiter` to override the list delimiter when parsing from a string (default: comma).
- Optional config key `envConfig.secretKeys` (a list or a comma-separated string) to extend the set of key names masked by `dumpConfig()`.

### Fixed

- `dumpConfig()` masks secret values so they are not written to the log. A key is masked when its name, or a nested name such as `db.password`, matches `password`, `passwd`, `secret`, `token`, `api_key` / `apikey`, `credential`, or `private_key` / `privatekey` (case-insensitive). Getters still return the real value.
- List values given as a single string (for example from an environment variable) are parsed on Ktor 3.6 as well as on 3.2 and 3.4. Ktor 3.6 wraps the "not a list" error, so the previous `ConfigException` catch never saw it.

### Changed

- The Ktor dependency is the version range `[3.2.0, 3.7.0)` (3.2.x through 3.6.x) instead of a single pinned release. The same artifact is built to work with Ktor 3.2, 3.4, and 3.6.

### Breaking

- Previously, requesting a list (e.g. `getList(...)`) for a value that was a string (delimiter-separated or otherwise) would throw an exception. Now such a value is parsed as a list: delimiter-separated strings become multiple elements, and a string without the delimiter produces a single-element list.

## [3.0.0] and earlier

Releases before adoption of this changelog. See [GitHub Releases](https://github.com/sharpmind-de/ktor-env-config/releases) for history.
