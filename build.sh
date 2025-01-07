set -e

# Configuration
GPG_USER="0xCED254CF741FE1663B9BEC32D12C9545C6D5AA73"

# Evaluate Maven Coordinates

mvn help:evaluate -Dexpression=project.groupId -Doutput=groupId.mvnhelp
mvn help:evaluate -Dexpression=project.artifactId -Doutput=artifactId.mvnhelp
mvn help:evaluate -Dexpression=project.version -Doutput=version.mvnhelp

groupId=$(cat groupId.mvnhelp)
artifactId=$(cat artifactId.mvnhelp)
version=$(cat version.mvnhelp)

rm *.mvnhelp

# Create Variables

artifact_path="${groupId//./\/}/${artifactId//./\/}/${version}/"
prefix_name="${artifactId}-${version}"
prefix_path="${artifact_path}/${prefix_name}"


# Run Maven

mvn clean install

# Create Maven Central ZIP Folder Structure

mkdir --parents "$artifact_path"

artifact_root_path=$(echo "${artifact_path}" | cut -d '/' -f 1)

# Check if the variable is less than 3
if [ "$artifact_root_path" -lt 3 ]; then
  echo "Error: Could not delete ${artifact_root_path}"
  exit 1
fi

rm -rf "$artifact_root_path"
mkdir --parents "$artifact_path"

mvn help:effective-pom "-Doutput=${prefix_path}.pom"
cp "./target/${prefix_name}-javadoc.jar" "$artifact_path"
cp "./target/${prefix_name}-sources.jar" "$artifact_path"
cp "./target/${prefix_name}.jar" "$artifact_path"

# Create Maven Central ZIP file

for file in "$artifact_path"/*; do
	echo "$file"
	gpg --yes --local-user $GPG_USER -ab $file
	md5sum $file | cut -d ' ' -f 1 > "$file.md5"
	sha1sum $file | cut -d ' ' -f 1 > "$file.sha1"
	sha256sum $file | cut -d ' ' -f 1 > "$file.sha256"
	sha512sum $file | cut -d ' ' -f 1 > "$file.sha512"
done

rm -f "./maven.zip"
zip -r "./maven.zip" "$artifact_path"

echo "./${groupId}.${artifactId}.${version}.zip"