#!/bin/bash


while read i
do

	if [ ! ${i:0:1} = '#' ]
	then
		echo "Processing host $i"
		ssh $i $2 < /dev/null &
	fi
done < $1

