#!/bin/bash

function re() {
    r "$@"
    if [ $? -ne 0 ];then
        exit $?
    fi
}

function r() {
    echo "$@"
    "$@"
}

[ "$TRAVIS_PULL_REQUEST" = "false" -a "$TRAVIS_BRANCH" != "master" ] && exit 0

re ./gradlew clean
re rm -rf cov-int

re cov-build --dir cov-int ./gradlew compileJava
re tar czvf embulk-coverity.tar.gz cov-int

echo "Posting to https://scan.coverity.com/builds?project=embulk%2Fembulk"

curl --form token="$COVERITY_SCAN_TOKEN" \
  --form email=frsyuki@gmail.com \
  --form file=@embulk-coverity.tar.gz \
  --form version="$(date +%Y%m%d)-$TRAVIS_COMMIT" \
  --form description="Embulk coverity scan" \
  "https://scan.coverity.com/builds?project=embulk%2Fembulk"

