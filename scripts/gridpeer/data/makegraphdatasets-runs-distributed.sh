#!/bin/bash

#$1 root directory of the experiment
#$2 length of the experiment

if [ -z $1 ]; then
	echo you forgot the root directory of the experiment
fi
if [ -z $2 ]; then
	echo you forgot the length of the experiment
fi

mypath=$(dirname $(readlink -f $0))


function graphit {
	echo 'aggregating datasets...'

	php -f $mypath/process-delay.php pass2 > delay.data &
	php -f $mypath/process-delay-per-class.php pass2 > delay-per-class.data &
	php -f $mypath/process-delay-percentiles.php pass2 > delay-per-class-percentile.data &
	php -f $mypath/process-scatter.php pass2 > delay-scatter.data &
	php -f $mypath/process-overhead-dhtoperations-type.php pass2 > overhead-dhtoperations-type.data &
  php -f $mypath/process-upstream-utilization-per-class.php pass2 > upstream-utilization-per-class.data &
  php -f $mypath/process-upstream-utilization-per-channel.php pass2 > upstream-utilization-per-channel.data &

	php -f $mypath/process-x-runs.php upstream-utilization-all.data > upstream-utilization.data &
	php -f $mypath/process-x-runs.php overhead-messages-all.data > overhead-messages.data &
	php -f $mypath/process-x-runs.php overhead-messages-type-all.data > overhead-messages-type.data &
  php -f $mypath/process-s-runs.php duplicate-blocks-all.data > duplicate-blocks.data &
  php -f $mypath/process-s-runs.php failed-playback-all.data > failed-playback.data &
  php -f $mypath/process-x-runs.php failed-playback-per-class-all.data > failed-playback-per-class.data &
	php -f $mypath/process-s-runs.php skipped-blocks-all.data > skipped-blocks.data &
	php -f $mypath/process-block-request-success-rate.php pass2 > block-request-success-rate.data &
	php -f $mypath/process-s-runs.php continuity-all.data > continuity.data &
	php -f $mypath/process-s-runs.php liveness-all.data > liveness.data &
	
	wait
}

cd $1
rm *.data

if [ ! -s 'pass2' ]; then
	newpass2l0=1 #doesn't exist, needs a new one
fi

for f in `ls -d [0-9]*`
do

	if [ -d $f ]
	then
		cd $f
		rm *-all.data
		
		if [ ! -s 'pass2' ]; then
			newpass2l1=1 #doesn't exist, needs a new one
		fi
		
		for g in `ls`
		do
			if [ -d $g ]
			then
				echo "building datasets for $1/$f/$g"

				cd $g

				$mypath/makegraphdatasets.sh $2
				
				if [[ $newpass2l1 -eq 1 ]]; then
					cat pass2 >> ../pass2
				fi

				cd ..
			fi
		done

		echo "working on $1/$f"

		$mypath/makegraphdatasets.sh $2

		cat overhead-messages.data >> ../overhead-messages-all.data
		cat overhead-messages-type.data >> ../overhead-messages-type-all.data
		cat upstream-utilization.data >> ../upstream-utilization-all.data
		cat duplicate-blocks.data >> ../duplicate-blocks-all.data
		cat failed-playback.data >> ../failed-playback-all.data
		cat failed-playback-per-class.data >> ../failed-playback-per-class-all.data
		cat skipped-blocks.data >> ../skipped-blocks-all.data
		cat continuity.data >> ../continuity-all.data
		cat liveness.data >> ../liveness-all.data
		
		if [[ $newpass2l0 -eq 1 ]]; then
			cat pass2 >> ../pass2
		fi
		cd ..
	fi
done

graphit $1

echo 'done'
echo "all done with $1"



