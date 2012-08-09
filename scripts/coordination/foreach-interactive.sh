#!/bin/bash


while read i
do

	if [ ! ${i:0:1} = '#' ]
	then
		h=${i%%	*}
		echo "Processing host $h"
		ssh $h $2 < /dev/null
	fi
done < $1

