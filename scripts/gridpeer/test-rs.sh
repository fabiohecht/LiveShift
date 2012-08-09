
length=3600

mypath=$(dirname $(readlink -f $0))

pkill -f liveshift-2.0.jar

mkdir log

sleep 1

java -jar $mypath/../../dist/liveshift-2.0.jar ch.uzh.csg.liveshift.core.CommandLineInterface --bootstrap=127.0.0.1:11011 --peer-name=c1 -pp2p 11011 -penc 15351 --delete-storage -gur 2 --publish=C1 $1 $2 $3 &

sleep 10

java -jar $mypath/../../dist/liveshift-2.0.jar ch.uzh.csg.liveshift.core.CommandLineInterface --bootstrap=127.0.0.1:11011 --peer-name=c2 -pp2p 11012 -penc 15352 --delete-storage -gur 2 --publish=C2 $1 $2 $3 &
sleep 1
java -jar $mypath/../../dist/liveshift-2.0.jar ch.uzh.csg.liveshift.core.CommandLineInterface --bootstrap=127.0.0.1:11011 --peer-name=c3 -pp2p 11013 -penc 15353 --delete-storage -gur 2 --publish=C3 $1 $2 $3 &

sleep 10

java -jar $mypath/../../dist/liveshift-2.0.jar ch.uzh.csg.liveshift.core.CommandLineInterface --bootstrap=127.0.0.1:11011 --delete-storage --peer-name=p1 -pp2p 11001 -gur 1 -rss $length $1 $2 $3 &
sleep 1
java -jar $mypath/../../dist/liveshift-2.0.jar ch.uzh.csg.liveshift.core.CommandLineInterface --bootstrap=127.0.0.1:11011 --delete-storage --peer-name=p2 -pp2p 11002 -gur 1 -rss $length $1 $2 $3 &
sleep 1
java -jar $mypath/../../dist/liveshift-2.0.jar ch.uzh.csg.liveshift.core.CommandLineInterface --bootstrap=127.0.0.1:11011 --delete-storage --peer-name=p3 -pp2p 11003 -gur 1 -rss $length $1 $2 $3 &
sleep 1
java -jar $mypath/../../dist/liveshift-2.0.jar ch.uzh.csg.liveshift.core.CommandLineInterface --bootstrap=127.0.0.1:11011 --delete-storage --peer-name=p4 -pp2p 11004 -gur 1 -rss $length $1 $2 $3 &
sleep 1
java -jar $mypath/../../dist/liveshift-2.0.jar ch.uzh.csg.liveshift.core.CommandLineInterface --bootstrap=127.0.0.1:11011 --delete-storage --peer-name=p5 -pp2p 11005 -gur 1 -rss $length $1 $2 $3 &

sleep $length

pkill -f liveshift-2.0.jar

#$mypath/data/makegraphs.sh $length 'log/liveshift.log.0*'

