#!/bin/bash

for f in `ls ppn7-*/liveness.data`
do
	echo "`echo $f|sed 's/^\([^\/]*\)\/.*$/\1/'`\t`grep DELAY pass2|awk '{if ($4) {sum+=1-$6/$4; cnt++}} END {print sum/cnt,cnt}'`"
done

