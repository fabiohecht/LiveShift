#!/bin/bash

#$1 root directory of the experiment


if [ -z $1 ]; then
	echo you forgot the root directory of the experiment
	exit 1
fi


mypath=$(dirname $(readlink -f $0))


function graphit {

	echo graphing $1

	gnuplot $mypath/delay.gnuplot 2> /dev/null &
	gnuplot $mypath/delay-per-class.gnuplot 2> /dev/null &
	gnuplot $mypath/delay-per-class-simple.gnuplot 2> /dev/null &
	gnuplot $mypath/delay-scatter.gnuplot 2> /dev/null &
	gnuplot $mypath/upstream-utilization.gnuplot 2> /dev/null &
	gnuplot $mypath/upstream-utilization-scatter.gnuplot 2> /dev/null &
	gnuplot $mypath/overhead-messages.gnuplot 2> /dev/null &
	gnuplot $mypath/overhead-messages-type-runs.gnuplot 2> /dev/null &
	gnuplot $mypath/delay-per-class-percentile.gnuplot 2> /dev/null &

	wait
}

cd $1

for f in `ls -d [0-9]*`
do

	if [ -d $f ]
	then
		cd $f

		for g in `ls`
		do
			if [ -d $g ]
			then
				echo "graphing $1/$f/$g"

				cd $g

				$mypath/makegraphs.sh

				cd ..
			fi
		done

		echo "graphing $1/$f"

		$mypath/makegraphs.sh

		cd ..
	fi
done

graphit $1

echo 'done'
echo "all done with $1"



