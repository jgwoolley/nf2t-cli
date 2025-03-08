set -e

# Evaluate Maven Coordinates

mvn help:evaluate -Dexpression=project.version -Doutput=version.mvnhelp --quiet
version=$(cat version.mvnhelp)
rm *.mvnhelp

pwd

echo "Running mvn install"

if [ "$1" == "server" ]; then
    mvn install --quiet
else
    mvn install
fi

if [ "$1" == "server" ]; then
    java -jar setup-project/target/setup-project-${version}.jar mavenCentral setup-project nf2t-cli
else 
    java -jar setup-project/target/setup-project-${version}.jar mavenCentral --gpgUser 0xCED254CF741FE1663B9BEC32D12C9545C6D5AA73 setup-project nf2t-cli
fi 

rm -rf ./dist

java -jar setup-project/target/setup-project-${version}.jar docs setup-project nf2t-cli