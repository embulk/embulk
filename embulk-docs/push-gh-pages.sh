#!/bin/sh

set -e
[ "$TRAVIS_PULL_REQUEST" != "false" ] && exit 0

revision="`git rev-parse HEAD`"
./gradlew site

rm -rf gh_pages
git clone . gh_pages
cd gh_pages

git checkout gh-pages
rm -rf docs
cp -a ../embulk-docs/build/html docs

git config user.name "$GIT_USER_NAME"
git config user.email "$GIT_USER_EMAIL"
git commit -m "Updated document $revision"

curl "https://$GITHUB_TOKEN:@github.com" -o $HOME/.git/credentials
git push origin gh-pages
rm -rf $HOME/.git/credentials
