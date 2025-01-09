set -e

# Evaluate Maven Coordinates

mvn help:evaluate -Dexpression=project.groupId -Doutput=groupId.mvnhelp
mvn help:evaluate -Dexpression=project.artifactId -Doutput=artifactId.mvnhelp
mvn help:evaluate -Dexpression=project.version -Doutput=version.mvnhelp

groupId=$(cat groupId.mvnhelp)
artifactId=$(cat artifactId.mvnhelp)
version=$(cat version.mvnhelp)

rm *.mvnhelp

# Create Variables

prefix_name="${artifactId}-${version}"

# Creat test files

TEST_PATH="./quick_test/"

rm -rf $TEST_PATH
mkdir --parent "$TEST_PATH/1"
mkdir --parent "$TEST_PATH/2"
mkdir --parent "$TEST_PATH/3"
mkdir --parent "$TEST_PATH/4"
mkdir --parent "$TEST_PATH/5"


touch "$TEST_PATH/1/A" "$TEST_PATH/1/B" "$TEST_PATH/1/C"

java -jar "./target/${prefix_name}.jar" "package" "--in" "$TEST_PATH/1" "--out" "$TEST_PATH/2"
tar czf "$TEST_PATH/3/test.tar.gz" "$TEST_PATH/2/data.flowfilev3"

java -jar "./target/${prefix_name}.jar" "unpackage" "--in" "$TEST_PATH/3/test.tar.gz" "--out" "$TEST_PATH/4" "--results" "$TEST_PATH/5"