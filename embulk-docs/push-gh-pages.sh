#!/bin/bash

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
git add docs

git config user.name "$GIT_USER_NAME"
git config user.email "$GIT_USER_EMAIL"
git commit -m "Updated document $revision" || exit 0

git config credential.helper "store --file=.git_credential_helper"
echo "https://$GITHUB_TOKEN:@github.com" > "$HOME/.git_credential_helper"
trap "rm -rf $HOME/.git_credential_helper" EXIT
git push origin gh-pages
