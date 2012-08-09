for exp in *.tgz; do
        echo 'Processing '$exp
	echo ' ' >> results_laglay.log
	echo '::: '$exp >> results_laglay.log
	echo ' ' >> results_laglay.log
        ~/untar.sh $exp > /dev/null
        
	for file in ~/results/*.log; do
		echo $file >> results_laglay.log
		echo 'START_TIME: '`cat $file | head -n 1 | sed 's/\..*//g'` >> results_laglay.log
		echo 'PLAY_DELAY:' >> results_laglay.log
	        cat $file | grep -a 'actual play delay' | sed 's/\..*actual play delay is /;/g' | sed 's/ ms//g' >> results_laglay.log
		echo 'PLAY_LAYERS:' >> results_laglay.log
        	cat $file | grep -a 'play|playing' | sed 's/\..*play|playing /;/g' | sed 's/ blocks//g' >> results_laglay.log
	done

done

