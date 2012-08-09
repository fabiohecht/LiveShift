
#will run with vanilla, dus, tft, ipb, tft+dus, ipb+dus

#$1 name of the experiment
#$2 root directory for results (one for the experiment will be created inside of it)
#$3 path to liveshift-xx.jar

mypath=$(dirname $(readlink -f $0))

mkdir "$2/$1"
cd "$2/$1"

$mypath/run-simulation.sh vanilla $2/$1 $3
$mypath/run-simulation.sh dus $2/$1 $3 -dus
$mypath/run-simulation.sh tft $2/$1 $3 -r tft
$mypath/run-simulation.sh ipb $2/$1 $3 -r ipb
$mypath/run-simulation.sh tftdus $2/$1 $3 -r tft -dus
$mypath/run-simulation.sh ipbdus $2/$1 $3 -r ipb -dus

echo 'finished all.'

