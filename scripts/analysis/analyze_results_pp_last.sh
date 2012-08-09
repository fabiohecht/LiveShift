last=$(ls ~/*.tgz | sort | tail -n 1)
~/untar.sh $last > /dev/null
echo '::: '$last' :::'
~/analyze/analyze_results_pp.sh
