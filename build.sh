set -e

# Evaluate Maven Coordinates

mvn help:evaluate -Dexpression=project.version -Doutput=version.mvnhelp --quiet
version=$(cat version.mvnhelp)
rm *.mvnhelp

if [ "$1" == "server" ]; then
    mvn install --quiet
else
    mvn install
fi

if [ "$1" == "server" ]; then
    java -jar build-maven-central-zip/target/build-maven-central-zip-${version}.jar mavenCentral build-maven-central-zip nf2t-cli
else 
    java -jar build-maven-central-zip/target/build-maven-central-zip-${version}.jar mavenCentral --gpgUser 0xCED254CF741FE1663B9BEC32D12C9545C6D5AA73 build-maven-central-zip nf2t-cli
fi 

rm -rf ./dist

java -jar build-maven-central-zip/target/build-maven-central-zip-${version}.jar docs build-maven-central-zip nf2t-cli