#!/usr/bin/env sh

set -eu

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
WRAPPER_DIR="$BASE_DIR/.mvn/wrapper"
WRAPPER_JAR="$WRAPPER_DIR/maven-wrapper.jar"
WRAPPER_PROPS="$WRAPPER_DIR/maven-wrapper.properties"

if [ ! -f "$WRAPPER_PROPS" ]; then
	echo "Maven Wrapper properties not found: $WRAPPER_PROPS" >&2
	exit 1
fi

WRAPPER_URL=$(grep -E '^wrapperUrl=' "$WRAPPER_PROPS" | cut -d'=' -f2- || true)
if [ -z "${WRAPPER_URL}" ]; then
	WRAPPER_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar"
fi

if [ ! -s "$WRAPPER_JAR" ]; then
	mkdir -p "$WRAPPER_DIR"
	echo "Downloading Maven Wrapper JAR..." >&2
	echo "  from: $WRAPPER_URL" >&2
	echo "  to:   $WRAPPER_JAR" >&2
	if command -v curl >/dev/null 2>&1; then
		curl -fsSL "$WRAPPER_URL" -o "$WRAPPER_JAR"
	elif command -v wget >/dev/null 2>&1; then
		wget -qO "$WRAPPER_JAR" "$WRAPPER_URL"
	else
		echo "Neither curl nor wget is available to download the wrapper JAR." >&2
		exit 1
	fi
fi

JAVA_EXE=${JAVA_HOME:-}/bin/java
if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_EXE" ]; then
	exec "$JAVA_EXE" "-Dmaven.multiModuleProjectDirectory=$BASE_DIR" -classpath "$WRAPPER_JAR" org.apache.maven.wrapper.MavenWrapperMain "$@"
fi

exec java "-Dmaven.multiModuleProjectDirectory=$BASE_DIR" -classpath "$WRAPPER_JAR" org.apache.maven.wrapper.MavenWrapperMain "$@"
