#!/bin/bash

#copy some file in some directory of all hosts

for i in `cat hosts`;
do
	if [ ! ${i:0:1} = '#' ];
	then
		scp root@${i}:~/liveshift/liveshift.log results/liveshift_${i}.log
	fi
done
