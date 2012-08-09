echo "Using incentive: $1"
echo '' > worked.log

for i in 1 2 3 4 5 6 7 8
	do for j in 1 2 3 4 5
		do if [ $(cat worked.log | wc -l) -gt 30 ]
			then echo "`date`: GOT ALL RESULTS, ENDING."
			exit
		fi
		echo "`date`: RUN NO. $i-$j"
		./run_rss.sh $1 > scenario.log 2>&1
		echo "`date`: DONE."
	done 
done
