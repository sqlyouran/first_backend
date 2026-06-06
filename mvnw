#!/bin/sh
# Maven Wrapper script - downloads and runs Maven
# Based on Apache Maven Wrapper 3.2.0

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
MAVEN_HOME="${SCRIPT_DIR}/.mvn/wrapper/dists/apache-maven-3.9.6"
MAVEN_URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.tar.gz"

if [ ! -f "${MAVEN_HOME}/bin/mvn" ]; then
    echo "Downloading Maven 3.9.6..."
    mkdir -p "${MAVEN_HOME}"
    TMPFILE=$(mktemp /tmp/maven-XXXXXX.tar.gz)
    curl -sSL -o "${TMPFILE}" "${MAVEN_URL}"
    tar -xzf "${TMPFILE}" -C "${MAVEN_HOME}" --strip-components=1
    rm -f "${TMPFILE}"
    echo "Maven 3.9.6 installed to ${MAVEN_HOME}"
fi

exec "${MAVEN_HOME}/bin/mvn" "$@"
