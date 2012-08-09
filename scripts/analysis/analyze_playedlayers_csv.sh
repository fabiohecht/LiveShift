for file in ~/results/*.log; do
	cat $file | grep 'play|playing' | sed 's/\..*play|playing /;/g' | sed 's/ blocks//g' > $file'_layers.csv'
done
