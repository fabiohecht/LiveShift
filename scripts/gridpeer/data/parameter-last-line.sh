#!/bin/bash

#$1 = e.g. 'ppfxc0-skipstall-*-s4/skipped-blocks.data'

#takes the parameter from the file name (if it exists) and the last line of each file and puts them in one line

for f in `ls $1|sort -n -t'-' -k3`
do
	par=`echo $f|sed 's/^[^-]\+-\([^-]\{2\}\)[^-]\+\(-\?[^-]*\)-s.*$/\1\2/'`
	echo $par `tail -1 $f`
done

