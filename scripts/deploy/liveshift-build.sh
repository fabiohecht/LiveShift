#!/bin/bash

die () {
    echo >&2 "$@"
    exit 1
}

#[ "$#" -eq 2 ] || die "usage: liveshift-build <version> <comment>"


VERSION=0.5
#COMMENT=$2

echo build liveshift - version ${VERSION}

cd ../..

mkdir bin
mkdir dist

echo ${VERSION} > ./dist/liveshift.version
echo "`eval date +%Y%m%d` - version: ${VERSION}" >> changelog
#echo "${COMMENT}" >> changelog
#echo "" >> changelog

echo build config
cat ./build.xml | sed -e "s,/dist/liveshift-VERSION.jar,/dist/liveshift-${VERSION}.jar," > ./build.xml.tmp
mv ./build.xml ./build.xml.bkp
mv ./build.xml.tmp ./build.xml
cat ./build.xml

echo create runnable jar
ant create_run_jar

echo create windows executable

cp ../../launch4j.xml ./launch4j.xml

echo launch4j config
cat ./launch4j.xml | sed -e "s,/dist/liveshift-VERSION.,/dist/liveshift-${VERSION}.," > ./launch4j.xml.tmp
mv ./launch4j.xml ./launch4j.xml.bkp
mv ./launch4j.xml.tmp ./launch4j.xml
cat ./launch4j.xml
launch4j "`pwd`/launch4j.xml"

rm ./build.xml
mv ./build.xml.bkp ./build.xml

rm ./launch4j.xml
mv ./launch4j.xml.bkp ./launch4j.xml

echo files created

echo "executable jar: `pwd`/dist/liveshift-${VERSION}.jar"
echo "windows executable: `pwd`/dist/liveshift-${VERSION}.exe"

echo build done.
