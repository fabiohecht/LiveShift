#!/bin/bash

#copy some file in some directory of all hosts
while read i
do

	if [ ! ${i:0:1} = '#' ]
	then
		echo "Processing host $i"
		rsync --rsh="ssh -p$i" --exclude '.svn' -av --progress /home/fabio/doc/workspace/LiveShift-Trunk/scripts /home/fabio/doc/workspace/LiveShift-Trunk/dist hecht@localhost:liveshift
	fi
done < $1

