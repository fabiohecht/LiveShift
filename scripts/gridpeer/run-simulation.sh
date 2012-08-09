
#$1 name of the experiment
#$2 root directory for results (one for the experiment will be created inside of it)
#$3 path to liveshift-xx.jar
#$4 $5 $6 parameters to be passed (optional)

runs=20
mypath=$(dirname $(readlink -f $0))
lspath=$(readlink -f $3)

if [ -z "$1" ]; then
	echo you forgot to name the experiment.
fi
if [ ! -d "$2" ]; then
	echo $2 must exist.
	exit
fi
if [ ! -d "$lspath" ]; then
	echo $3 must exist.
	exit
fi

echo running experiment $1

mkdir "$2/$1"
cd "$2/$1"

for run in `seq 1 $runs`
do
	echo on run $run
	
	mkdir $run
	cd $run

	$mypath/test-sc1.sh $lspath $4 $5 $6 1&> output 2&>error 

	echo run $run done, compressing data

	tar czf log.tar.gz log
	tar czf errors.tar.gz error.*
	tar czf output.tar.gz output
	rm -rf log error.* output pass1
	
	cd ..

done

echo 'finished. good luck!'

