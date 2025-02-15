set -e

cd ./nf2t-cli

# Configuration
GPG_USER="0xCED254CF741FE1663B9BEC32D12C9545C6D5AA73"

# Startup Variables

if [ "$CI" = "true" ]; then
	MVN_ARGS="--no-transfer-progress --batch-mode"
fi

# Evaluate Maven Coordinates

mvn $MVN_ARGS help:evaluate -Dexpression=project.groupId -Doutput=groupId.mvnhelp
mvn $MVN_ARGS help:evaluate -Dexpression=project.artifactId -Doutput=artifactId.mvnhelp
mvn $MVN_ARGS help:evaluate -Dexpression=project.version -Doutput=version.mvnhelp

groupId=$(cat groupId.mvnhelp)
artifactId=$(cat artifactId.mvnhelp)
version=$(cat version.mvnhelp)

echo "Deleting Maven Metadata Artifacts"
rm *.mvnhelp

# Create Variables

artifact_path="${groupId//./\/}/${artifactId//./\/}/${version}/"
prefix_name="${artifactId}-${version}"
prefix_path="${artifact_path}/${prefix_name}"

# Run Maven

# TODO: Or --quit https://maven.apache.org/ref/3.6.1/maven-embedder/cli.html
mvn clean install $MVN_ARGS

# Create Maven Central ZIP Folder Structure

mkdir --parents "$artifact_path"

artifact_root_path=$(echo "${artifact_path}" | cut -d '/' -f 1)
echo "Attempting to delete $artifact_root_path"

# Check if the variable is less than 3
if [[ ${#artifact_root_path} -lt 3 ]]; then
  echo "Error: Could not delete ${artifact_root_path}"
  exit 1
fi

echo "Deleting Artifact Root Path: $artifact_root_path"
rm -rf "$artifact_root_path"
mkdir --parents "$artifact_path"

mvn help:effective-pom $MVN_ARGS "-Doutput=${prefix_path}.pom"
cp "./target/${prefix_name}-javadoc.jar" "$artifact_path"
cp "./target/${prefix_name}-sources.jar" "$artifact_path"
cp "./target/${prefix_name}.jar" "$artifact_path"

# Create Maven Central ZIP file

for file in "$artifact_path"/*; do
	echo "$file"

	md5sum $file | cut -d ' ' -f 1 > "$file.md5"
	sha1sum $file | cut -d ' ' -f 1 > "$file.sha1"
	sha256sum $file | cut -d ' ' -f 1 > "$file.sha256"
	sha512sum $file | cut -d ' ' -f 1 > "$file.sha512"

	if gpg --yes --local-user $GPG_USER -ab $file; then
	 	echo "GPG: Successfully Signed"
	else
		echo "GPG: Failed to Sign"
	fi
done

rm --force "./maven.zip"
zip -r "./maven.zip" "$artifact_path"

echo "${groupId}.${artifactId}.${version}"
