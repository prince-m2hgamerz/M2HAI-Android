#!/usr/bin/env sh

# Gradle wrapper script (Unix)
# Auto-generated for BlackboxAI

APP_HOME=$(cd "$(dirname "$0")" && pwd)

CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

if [ ! -f "$CLASSPATH" ]; then
  echo "Missing gradle-wrapper.jar in $CLASSPATH" >&2
  exit 1
fi

# Use Java
JAVA=${JAVA_HOME:+$JAVA_HOME/bin/java}
if [ -z "$JAVA" ]; then
  JAVA=java
fi

exec "$JAVA" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"

