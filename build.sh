set -e

mvn clean install

VERSION="0.0.2-SNAPSHOT"
REPOSITORY_PATH="com/yelloowstone/nf2t/cli/$VERSION/"

mkdir --parents "$REPOSITORY_PATH"
rm -f "$REPOSITORY_PATH*"

for file in ./target/*.jar; do
	echo $file
	gpg --yes --local-user 0xCED254CF741FE1663B9BEC32D12C9545C6D5AA73 -ab $file
	md5sum $file | cut -d ' ' -f 1 > "$file.md5"
	sha1sum $file | cut -d ' ' -f 1 > "$file.sha1"
	sha256sum $file | cut -d ' ' -f 1 > "$file.sha256"
	sha512sum $file | cut -d ' ' -f 1 > "$file.sha512"

	cp "$file" "./$REPOSITORY_PATH"
	cp "$file.md5" "./$REPOSITORY_PATH"
	cp "$file.sha1" "./$REPOSITORY_PATH"
	cp "$file.sha256" "./$REPOSITORY_PATH"
	cp "$file.asc" "./$REPOSITORY_PATH"
done

cp pom.xml "./$REPOSITORY_PATH"

zip -r ./maven.zip "$REPOSITORY_PATH"
rm -rf "$REPOSITORY_PATH"
