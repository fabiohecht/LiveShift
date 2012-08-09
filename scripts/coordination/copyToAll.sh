#!/bin/bash


while read i
do

	if [ ! ${i:0:1} = '#' ]
	then
		echo "Processing host $i"
		scp -r $2 -p$i hecht@localhost:liveshift  < /dev/null
	fi
done < $1

