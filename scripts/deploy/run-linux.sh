#!/bin/bash

if [ -f ./update-liveshift.jar ]
then
echo update exists!
echo replacing current version
mv ./liveshift.jar ./liveshift.jar.bkp
mv ./update-liveshift.jar ./liveshift.jar
fi

java -jar ./liveshift.jar