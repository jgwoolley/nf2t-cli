set -e

# Evaluate Maven Coordinates

mvn help:evaluate -Dexpression=project.groupId -Doutput=groupId.mvnhelp
mvn help:evaluate -Dexpression=project.artifactId -Doutput=artifactId.mvnhelp
mvn help:evaluate -Dexpression=project.version -Doutput=version.mvnhelp

groupId=$(cat groupId.mvnhelp)
artifactId=$(cat artifactId.mvnhelp)
version=$(cat version.mvnhelp)

rm *.mvnhelp

# Create JavaDocs

mkdir --parents ./public/javadocs/
rm -rf ./public/*

# Create Variables

prefix_name="${artifactId}-${version}"

cp pages.html public/index.html
cp maven.zip public/
unzip "./target/${prefix_name}-javadoc.jar" -d public/javadocs