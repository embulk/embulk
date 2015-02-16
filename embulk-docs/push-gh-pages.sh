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

#[ "$TRAVIS_PULL_REQUEST" != "false" ] && exit 0

revision="$(git rev-parse HEAD)"
remote="$(git config remote.origin.url | sed "s+^git:+http:+")"
re ./gradlew site

r git fetch --unshallow || echo "using complete repository."

re rm -rf gh_pages
re git clone . gh_pages
re cd gh_pages

re git remote add travis_push "$remote"
re git fetch travis_push

re git checkout -b gh-pages travis_push/gh-pages
re rm -rf docs
re cp -a ../embulk-docs/build/html docs
re git add docs

re git config user.name "$GIT_USER_NAME"
re git config user.email "$GIT_USER_EMAIL"
r git commit -m "Updated document $revision"

re git config credential.helper "store --file=.git_credential_helper"
echo "https://$GITHUB_TOKEN:@github.com" > "$HOME/.git_credential_helper"
trap "rm -rf $HOME/.git_credential_helper" EXIT
re git push travis_push gh-pages
