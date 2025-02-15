set -e

cd ./nf2t-cli

# Evaluate Maven Coordinates

mvn help:evaluate -Dexpression=project.groupId -Doutput=groupId.mvnhelp
mvn help:evaluate -Dexpression=project.artifactId -Doutput=artifactId.mvnhelp
mvn help:evaluate -Dexpression=project.version -Doutput=version.mvnhelp
git log -1 --pretty=format:'%H' >> gitHash.mvnhelp

groupId=$(cat groupId.mvnhelp)
artifactId=$(cat artifactId.mvnhelp)
version=$(cat version.mvnhelp)
gitHash=$(cat gitHash.mvnhelp)

rm *.mvnhelp

# Create JavaDocs

mkdir --parents ./dist/
rm -rf ./dist/

cp -a ./public/ ./dist/
cp ../build.sh ./dist/
cp ../docs.sh ./dist/
cp ../download_jar.sh ./dist/

mkdir --parents ./dist/javadocs/
mkdir --parents ./dist/man/

# Create Variables

prefix_name="${artifactId}-${version}"

cp maven.zip ./dist/
unzip "./target/${prefix_name}-javadoc.jar" -d ./dist/javadocs

java -jar "./target/${prefix_name}.jar" gen-manpage -d ./dist/man/ --exit

asciidoctor --source-dir "./dist/man" "./dist/man/*.adoc"
asciidoctor ../README.asciidoc "--attribute" "gitHash=${gitHash}" "--attribute" "mavenGroupId=${groupId}" "--attribute" "mavenArtifactId=${artifactId}" "--attribute" "mavenVersion=${version}" "--out-file" "./dist/index.html" "-a" "toc=left"
