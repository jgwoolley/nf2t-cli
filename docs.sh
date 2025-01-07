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

mkdir --parents ./public/
rm -rf ./public/*

mkdir ./public/javadocs/
mkdir ./public/man/

# Create Variables

prefix_name="${artifactId}-${version}"

cp pages.html public/index.html
cp maven.zip public/
unzip "./target/${prefix_name}-javadoc.jar" -d public/javadocs

java -jar "./target/${prefix_name}.jar" gen-manpage -d public/man/

asciidoctor --source-dir "public/man" "public/man/*.adoc"

echo '<html><head><meta http-equiv="refresh" content="0; url=nf2t.html" /></head><body><p><a href="nf2t.html">Redirect</a></p></body></html>' >> "public/man/index.html"