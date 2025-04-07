#!/usr/bin/env bash

# See https://www.youtube.com/watch?v=9fSkygQ-ZjI
set -euxo pipefail

mvn versions:set -DnewVersion=0.0.11
mvn versions:commit
