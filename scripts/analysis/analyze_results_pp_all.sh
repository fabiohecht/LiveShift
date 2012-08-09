for file in *.tgz; do
	echo 'Processing '$file
	~/untar.sh $file > /dev/null
	echo "$file:" >> results_pp.log
	~/analyze/analyze_results_pp.sh >> results_pp.log
done
