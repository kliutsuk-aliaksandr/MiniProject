#!/usr/bin/env sh
# Lightweight Gradle bootstrap for this repository.
# Downloads the pinned Gradle distribution once and reuses it from GRADLE_USER_HOME.
set -e

GRADLE_VERSION="8.13"
if [ -z "$GRADLE_USER_HOME" ]; then
    GRADLE_USER_HOME="$HOME/.gradle"
fi
INSTALL_ROOT="$GRADLE_USER_HOME/miniproject"
GRADLE_HOME="$INSTALL_ROOT/gradle-$GRADLE_VERSION"

if [ ! -x "$GRADLE_HOME/bin/gradle" ]; then
    mkdir -p "$INSTALL_ROOT"
    ZIP_FILE="$INSTALL_ROOT/gradle-$GRADLE_VERSION-bin.zip"
    if [ ! -f "$ZIP_FILE" ]; then
        echo "Downloading Gradle $GRADLE_VERSION..."
        if command -v curl >/dev/null 2>&1; then
            curl -fL "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -o "$ZIP_FILE"
        else
            echo "curl is required to download Gradle." >&2
            exit 1
        fi
    fi
    if ! command -v unzip >/dev/null 2>&1; then
        echo "unzip is required to install Gradle." >&2
        exit 1
    fi
    unzip -q -o "$ZIP_FILE" -d "$INSTALL_ROOT"
fi

exec "$GRADLE_HOME/bin/gradle" "$@"
