#!/bin/bash

for f in `ls ppn7-*/skipped-blocks.data`
do 
	echo "`echo $f|sed 's/^\([^\/]*\)\/.*$/\1/'`\t`tail -n1 $f`"
done
