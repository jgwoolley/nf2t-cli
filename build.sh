set -e

# Evaluate Maven Coordinates

mvn help:evaluate -Dexpression=project.version -Doutput=version.mvnhelp --quiet
version=$(cat version.mvnhelp)
rm *.mvnhelp

mvn install 
java -jar build-maven-central-zip/target/build-maven-central-zip-${version}.jar docs build-maven-central-zip nf2t-cli
java -jar build-maven-central-zip/target/build-maven-central-zip-${version}.jar mavenCentral build-maven-central-zip nf2t-cli