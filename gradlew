#!/bin/sh
# Gradle wrapper script for Unix
APP_HOME=$(cd "$(dirname "$0")" && pwd)
exec "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" "$@" 2>/dev/null \
  || exec java -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
