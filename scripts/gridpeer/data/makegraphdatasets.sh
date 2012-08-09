
mypath=$(dirname $(readlink -f $0))

if [ -z "$1" ]; then
	echo you forgot the length.
fi

echo 'processing logs...'

if [ ! -s 'pass2' ]; then
	
	mkfifo pass0 pass1
	
	cat pass0 | egrep 'INFO|called|DELAY|achieved|((sent|received|dht)MessagesAverage)|dhtOperation |in putSegmentBlock|duplicate block received|Playback failed|tuning|trying to send message' > pass1 &
	cat pass1 | php -f $mypath/delete-remainings.php "$1" > pass2 &

	if [ ! -s "$2" ]; then
		if [ ! -e 'log.tar.gz' ]; then
			echo 'nothing to graph here.'
			rm pass0 pass1
			exit 1
		else
			tar xOf log.tar.gz --verbose 2>&1 | awk '{if (match($0,"liveshift.log")) {fn=$0} else printf("%s:%s\n",fn,$0)}' > pass0
		fi
	else
		cat $2 > pass0
	fi
	
	rm pass0 pass1
	
fi

echo 'building datasets...'

php -f $mypath/process-delay.php pass2 > delay.data &
php -f $mypath/process-delay-per-class.php pass2 > delay-per-class.data &
php -f $mypath/process-delay-percentiles.php pass2 > delay-per-class-percentile.data &
php -f $mypath/process-scatter.php pass2 > delay-scatter.data &
php -f $mypath/process-upstream-utilization.php pass2 > upstream-utilization.data &
php -f $mypath/process-upstream-utilization-per-class.php pass2 > upstream-utilization-per-class.data &
php -f $mypath/process-upstream-utilization-per-channel.php pass2 > upstream-utilization-per-channel.data &
php -f $mypath/process-overhead-messages.php pass2 > overhead-messages.data &
php -f $mypath/process-overhead-messages-type.php pass2 > overhead-messages-type.data &
php -f $mypath/process-overhead-dhtoperations-type.php pass2 > overhead-dhtoperations-type.data &
php -f $mypath/process-overhead-kbyte.php pass2 > overhead-kbyte.data &
php -f $mypath/process-duplicate-blocks.php pass2 > duplicate-blocks.data &
php -f $mypath/process-failed-playback.php pass2 > failed-playback.data &
php -f $mypath/process-failed-playback-per-class.php pass2 > failed-playback-per-class.data &
php -f $mypath/process-skipped-blocks.php pass2 > skipped-blocks.data &
php -f $mypath/process-block-request-success-rate.php pass2 > block-request-success-rate.data &

wait

echo 'done'
