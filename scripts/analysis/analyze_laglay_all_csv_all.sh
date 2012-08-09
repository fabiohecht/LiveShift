for type in dummy tft psh; do
	mv ~/results_$type/*.tgz ~
	~/analyze/analyze_laglay_all_csv.sh
	mv ~/*.tgz ~/results_$type/
	mv results_laglay.log ~/results_$type/
done
