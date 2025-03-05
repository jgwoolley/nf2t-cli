set -e

mvn install && java -jar build-maven-central-zip/target/build-maven-central-zip-0.0.9-SNAPSHOT.jar --gpgUser 0xCED254CF741FE1663B9BEC32D12C9545C6D5AA73 build-maven-central-zip nf2t-cli