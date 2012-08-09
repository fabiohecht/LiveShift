#!/bin/bash

for file in `ls */*/log.tar.gz`
do
#	echo $file
	tar xzf $file -O|egrep '(send|reply) (TC|UD)P message'|grep -v DIRECT_DATA|cut -d ',' -f 4|cut -d '=' -f 2|awk '{sum+=$1+64} END { print sum}'

done

