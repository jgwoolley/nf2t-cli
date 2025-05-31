#!/usr/bin/env bash

# See https://www.youtube.com/watch?v=9fSkygQ-ZjI
# -e Exit immediately if a command exits with a non-zero status.
# -u Treat unset varibales as an error when substituting.
# -o Set the variabel corresponding to the option-name:
# pipefail: the return value of a pipeline is the status of the last command to exit with a non-zero status
# -x print commands and their arguments as they are executed
set -euxo pipefail

set +u
mode=$1
set -u

# Evaluate Maven Coordinates

mvn help:evaluate -Dexpression=project.version -Doutput=version.mvnhelp --quiet
version=$(cat version.mvnhelp)
rm *.mvnhelp

pwd

echo "Running mvn install"

if [ "${mode}" == "server" ]; then
    mvn install --quiet
else
    mvn install
fi

if [ "${mode}" == "server" ]; then
    java -jar nf2t-cli/target/nf2t-cli-${version}.jar mavenCentral nf2t-cli nf2t-lib
else 
    java -jar nf2t-cli/target/nf2t-cli-${version}.jar mavenCentral --gpgUser 0xCED254CF741FE1663B9BEC32D12C9545C6D5AA73 nf2t-cli
fi 

rm -rf ./dist

java -jar nf2t-cli/target/nf2t-cli-${version}.jar jars docs --isPicocli nf2t-cli nf2t-lib
