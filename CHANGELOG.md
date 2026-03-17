# Changelog

All notable changes to this project are documented in this file.

**Breaking changes** are listed under a `### Breaking` subsection for the relevant release. They are also summarized in [GitHub Releases](https://github.com/sharpmind-de/ktor-env-config/releases) when publishing a new version.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

### Added

- List properties can be supplied as comma-separated strings (e.g. from environment variables). See README section "type evaluation → list".
- Optional config key `listDelimiter` to override the list delimiter when parsing from a string (default: comma).

### Breaking

- Previously, requesting a list (e.g. `getList(...)`) for a value that was a string (delimiter-separated or otherwise) would throw an exception. Now such a value is parsed as a list: delimiter-separated strings become multiple elements, and a string without the delimiter produces a single-element list.

## [3.0.0] and earlier

Releases before adoption of this changelog. See [GitHub Releases](https://github.com/sharpmind-de/ktor-env-config/releases) for history.
