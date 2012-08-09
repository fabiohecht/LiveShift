#!/bin/bash

#$1 root directory of the experiment

length=600

if [ -z $1 ]; then
	echo you forgot the root directory of the experiment
fi

mypath=$(dirname $(readlink -f $0))

cd $1
rm pass[12]
rm *-all.data
for f in `ls -d [0-9]*`
do
	if [ -d $f ]
	then
		echo graphing $1/$f

		cd $f

		if [ -e pass1.tar.gz ]; then
			tar xf pass1.tar.gz  #could use log but it's bigger
		fi
		if [ -e pass2 ]; then
			rm pass2
		fi
		$mypath/makegraphs.sh $length pass1
		cat pass2 >> ../pass2
		cat overhead-messages.data >> ../overhead-messages-all.data
		cat upstream-utilization.data >> ../upstream-utilization-all.data

		cd ..
	fi
done

echo graphing $1

echo 'building datasets...'

php -f $mypath/process-delay.php pass2 > delay.data &
php -f $mypath/process-scatter.php pass2 > delay-scatter.data &
php -f $mypath/process-upstream-utilization-runs.php upstream-utilization-all.data > upstream-utilization.data &
php -f $mypath/process-overhead-messages-runs.php overhead-messages-all.data > overhead-messages.data &
#php -f $mypath/process-overhead-messages-type.php pass2 > overhead-messages-type.data &

wait

echo 'graphing'

gnuplot $mypath/delay.gnuplot &
gnuplot $mypath/delay-scatter.gnuplot &
gnuplot $mypath/upstream-utilization.gnuplot &
gnuplot $mypath/upstream-utilization-scatter.gnuplot &
gnuplot $mypath/overhead-messages.gnuplot &
gnuplot $mypath/overhead-messages-type.gnuplot &

wait

echo 'done'
echo "all done with $1"

