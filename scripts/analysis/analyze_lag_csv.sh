for file in ~/results/*.log; do
	cat $file | grep 'actual play delay' | sed 's/\..*actual play delay is /;/g' | sed 's/ ms//g' > $file'_lag.csv'
done
