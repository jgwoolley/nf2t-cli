set -e

mvn clean install

VERSION="0.0.2"
REPOSITORY_PATH="com/yelloowstone/nf2t/nf2t-cli/$VERSION/"
POM_TARGET_PATH="./target/nf2t-cli-0.0.2.pom"

mkdir --parents "$REPOSITORY_PATH"
rm -f "$REPOSITORY_PATH*"

generate_file() {
	echo "$1"
	gpg --yes --local-user 0xCED254CF741FE1663B9BEC32D12C9545C6D5AA73 -ab $1
	md5sum $1 | cut -d ' ' -f 1 > "$1.md5"
	sha1sum $1 | cut -d ' ' -f 1 > "$1.sha1"
	sha256sum $1 | cut -d ' ' -f 1 > "$1.sha256"
	sha512sum $1 | cut -d ' ' -f 1 > "$1.sha512"

	cp "$1" "./${REPOSITORY_PATH}"
	cp "$1.md5" "./${REPOSITORY_PATH}"
	cp "$1.sha1" "./${REPOSITORY_PATH}"
	cp "$1.sha256" "./${REPOSITORY_PATH}"
	cp "$1.asc" "./${REPOSITORY_PATH}"
}

for file in ./target/*.jar; do
	generate_file $file
done

cp "pom.xml" "$POM_TARGET_PATH"

generate_file "$POM_TARGET_PATH"

rm -f ./maven.zip

zip -r ./maven.zip "$REPOSITORY_PATH"
rm -rf "$REPOSITORY_PATH"
