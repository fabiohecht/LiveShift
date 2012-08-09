#!/bin/bash

#copy some file in some directory of all hosts
while read i
do

	if [ ! ${i:0:1} = '#' ]
	then
		echo "Processing host $i"
		rsync --rsh="ssh -p$i" --exclude '.svn' -av --progress hecht@localhost:liveshift/results /home/fabio/doc/workspace/LiveShift-Trunk/results/$i
	fi
done < $1

