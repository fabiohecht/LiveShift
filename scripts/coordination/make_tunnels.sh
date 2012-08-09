#!/bin/bash

j=0
for i in  `cat $1`;
do
	if [ ! ${i:0:1} = '#' ];
	then
		j=$(($j+1))
		echo "Tunneling to host $i using lo port 200$j";
		ssh -Nf hecht@asterix.ifi.uzh.ch -L 200$j:$i:22
	fi
done
