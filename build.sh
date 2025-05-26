#!/usr/bin/env bash

# See https://www.youtube.com/watch?v=9fSkygQ-ZjI
set -euxo pipefail

# Evaluate Maven Coordinates

mvn help:evaluate -Dexpression=project.version -Doutput=version.mvnhelp --quiet
version=$(cat version.mvnhelp)
rm *.mvnhelp

pwd

echo "Running mvn install"

if [ "${1:=""}" == "server" ]; then
    mvn install --quiet
else
    mvn install
fi

if [ "$1" == "server" ]; then
    java -jar nf2t-cli/target/nf2t-cli-${version}.jar mavenCentral nf2t-cli
else 
    java -jar nf2t-cli/target/nf2t-cli-${version}.jar mavenCentral --gpgUser 0xCED254CF741FE1663B9BEC32D12C9545C6D5AA73 nf2t-cli
fi 

rm -rf ./dist

java -jar nf2t-cli/target/nf2t-cli-${version}.jar docs nf2t-cli
