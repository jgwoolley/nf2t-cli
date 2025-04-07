#!/usr/bin/env bash

# See https://www.youtube.com/watch?v=9fSkygQ-ZjI
set -euxo pipefail

rm -rf ~/.m2/repository/

cd ./nf2t-cli

repoUrl=https://repo1.maven.org/maven2/
groupId=com.yelloowstone.nf2t
artifactId=nf2t-cli
version=0.0.4
packaging=jar

mvn help:evaluate -Dexpression=settings.localRepository -Doutput=localRepository.mvnhelp
localRepository=$(cat localRepository.mvnhelp)
rm *.mvnhelp

group_repository_path="${localRepository}/${groupId//./\/}"
artifact_path="${group_repository_path}/${artifactId//./\/}/${version}/"

if [ "$group_repository_path" -lt 3 ]; then
  echo "Error: Could not delete ${group_repository_path}"
  exit 1
fi

rm -rf $group_repository_path

mvn org.apache.maven.plugins:maven-dependency-plugin:2.1:get \
    -DrepoUrl=${repoUrl} \
    -Dartifact=${groupId}:${artifactId}:${version}:${packaging}

java -jar "${artifact_path}/${artifactId}-${version}.jar generateSchema"