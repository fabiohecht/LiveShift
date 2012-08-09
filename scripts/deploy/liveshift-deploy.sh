#!/bin/bash

die () {
    echo >&2 "$@"
    exit 1
}

#[ "$#" -eq 1 ] || die "please add version to build"
#echo $1 | grep -E -q '^([0-9]\.?)+$' || die "version must be in format: 1.11"


VERSION=0.5

echo deploy liveshift version ${VERSION}

echo copy files

cd ../..

cp "./dist/liveshift-${VERSION}.jar" ./dist/liveshift.jar
cp "./dist/liveshift-${VERSION}.exe" ./dist/liveshift.exe

cp ./liveshift.properties ./dist/


echo add vlc lib

mkdir ./dist/vlclib
mkdir ./dist/vlclib/vlc

echo create tar for linux and zip for windows
cp ./scripts/deploy/run-linux.sh ./dist/liveshift.sh
chmod +x ./dist/liveshift.sh
cp ./scripts/deploy/run-windows.bat ./dist/liveshift.bat
chmod +x ./dist/liveshift.bat

echo copy vlc folder without svn files -linux-
cp ./vlclib/libvlc* ./dist/vlclib/
cd ./vlclib/linux
find . -name "*" -type d | grep  -v ".svn" | cut -c 3- | while read i ; do mkdir -v ../../dist/vlclib/vlc/$i ; done
find . -name "*" -type f | grep  -v ".svn" | cut -c 3- | while read i ; do cp -vf $i ../../dist/vlclib/vlc/$i ; done
cd ../..

cd dist

tar -cvf ./liveshift.tar ./liveshift.jar ./liveshift.sh ./vlclib ./liveshift.version ./liveshift.properties

rm -r ./vlclib

cd ..

mkdir ./dist/vlclib

echo copy vlc folder without svn files -windows-
cp ./vlclib/libvlc* ./dist/vlclib/
cd ./vlclib/windows
find . -name "*" -type d | grep  -v ".svn" | cut -c 3- | while read i ; do mkdir -v ../../dist/vlclib/$i ; done
find . -name "*" -type f | grep  -v ".svn" | cut -c 3- | while read i ; do cp -vf $i ../../dist/vlclib/$i ; done
cd ../..

cd dist

zip -r ./liveshift.zip ./liveshift.exe ./liveshift.bat ./vlclib/* ./liveshift.version ./liveshift.properties

rm -r ./vlclib


rm ./liveshift.sh
rm ./liveshift.bat

cd ..

echo version ${VERSION}
echo created files:
echo linux/mac: `pwd`/dist/liveshift.tar
echo windows: `pwd`/dist/liveshift.zip
echo done.