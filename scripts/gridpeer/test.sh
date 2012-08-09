pkill -f liveshift-2.0.jar
killall vlc
sleep 1
rm log/*log*
./start_vlc.sh &
sleep 1
java -jar ../../dist/liveshift-2.0.jar ch.uzh.csg.liveshift.core.CommandLineInterface --bootstrap=127.0.0.1:11101 --peer-name=peer0 --port-p2p=11101 --delete-storage --storage-time-limit=60000 --storage-protection-time=30 --storage-auto-interval-span=5 --statistics --publish=TestChannel &
sleep 5
GROUNDZERO=`date +%s`
java -jar ../../dist/liveshift-2.0.jar ch.uzh.csg.liveshift.core.CommandLineInterface --bootstrap=127.0.0.1:11101 --delete-storage --peer-name=peer1 --port-p2p=11102 --storage-time-limit=60 --storage-protection-time=30 --storage-auto-interval-span=5 --script=cTestChannel,s$(($GROUNDZERO+0)),w10,q &
java -jar ../../dist/liveshift-2.0.jar ch.uzh.csg.liveshift.core.CommandLineInterface --bootstrap=127.0.0.1:11101 --delete-storage --peer-name=peer2 --port-p2p=11103 --storage-time-limit=60 --storage-protection-time=30 --storage-auto-interval-span=5 --script=w10,cTestChannel,s$(($GROUNDZERO+0)),w10,q &
java -jar ../../dist/liveshift-2.0.jar ch.uzh.csg.liveshift.core.CommandLineInterface --bootstrap=127.0.0.1:11101 --delete-storage --peer-name=peer3 --port-p2p=11104 --storage-time-limit=60 --storage-protection-time=30 --storage-auto-interval-span=5 --script=w20,cTestChannel,s$(($GROUNDZERO+20)),w280,q &
java -jar ../../dist/liveshift-2.0.jar ch.uzh.csg.liveshift.core.CommandLineInterface --bootstrap=127.0.0.1:11101 --delete-storage --peer-name=peer4 --port-p2p=11105 --storage-time-limit=60 --storage-protection-time=30 --storage-auto-interval-span=5 --statistics --script=w120,cTestChannel,s$(($GROUNDZERO+0)),w10,q &
java -jar ../../dist/liveshift-2.0.jar ch.uzh.csg.liveshift.core.CommandLineInterface --bootstrap=127.0.0.1:11101 --delete-storage --peer-name=peer5 --port-p2p=11106 --storage-time-limit=60 --storage-protection-time=30 --storage-auto-interval-span=5 --statistics --script=w220,cTestChannel,s$(($GROUNDZERO+0)),w10,q &
read
pkill -f liveshift-2.0.jar
killall vlc

