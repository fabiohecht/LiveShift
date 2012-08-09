#!/bin/bash
# installs on local www folder. source: param2; target: param3;

die () {
    echo >&2 "$@"
    exit 1
}

[ "$#" -eq 3 ] || die "help: liveshift-serverinstall.sh <version> <source-folder> <target-folder>"

VERSION=$1
FOLDER=$2
TARGET=$3

echo move to www
mv "${FOLDER}/liveshift.tar" "${TARGET}"
mv "${FOLDER}/liveshift.zip" "${TARGET}"
mv "${FOLDER}/liveshift.exe" "${TARGET}"
mv "${FOLDER}/liveshift.jar" "${TARGET}"
mv "${FOLDER}/liveshift.version" "${TARGET}"
mv "${FOLDER}/liveshift.htm" "${TARGET}"

echo done.