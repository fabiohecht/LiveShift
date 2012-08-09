
#$1 path to liveshift-xx.jar
#$2 $3 $4 parameters to be passed

numpeercasters=6
numpeers=32
length=3600

pkill -9 -f liveshift-2.0.jar

sleep 10

rm -rf /tmp/LiveShift
rm -rf log
mkdir log

for i in `seq 1 $numpeercasters`
do
	sudo nice --5 sudo -u hecht $1/java -jar $1/liveshift-2.0.jar ch.uzh.csg.liveshift.core.CommandLineInterface --peer-name=c$i --bootstrap=127.0.0.1:10001 -pp2p $((10000+$i)) -penc $((11000+$i)) --delete-storage -gur 6 --publish=C$i $2 $3 $4 2&> error.c$i &
	sleep 5
done

sleep 5

for i in `seq 1 $numpeers`
do
	sudo nice --4 sudo -u hecht $1/java -jar $1/liveshift-2.0.jar ch.uzh.csg.liveshift.core.CommandLineInterface --peer-name=p$i --bootstrap=127.0.0.1:10001 --delete-storage -pp2p $((12000+$i)) -gur 1 -rss $length $2 $3 $4 2&> error.p$i  &
	sleep 3
done

sleep $(($length-5-5*$numpeercasters-3*$numpeers))

pkill -f liveshift-2.0.jar
sleep 5
pkill -9 -f liveshift-2.0.jar
sleep 10

