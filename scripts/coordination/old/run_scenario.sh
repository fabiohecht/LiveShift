
# config

hosts_file=hosts-demo
bootstrappeer=192.168.1.124
channel1host=192.168.1.124
#channel2host=192.168.1.136


echo hosts_file=$hosts_file
echo bootstrappeer=$bootstrappeer
echo channel1host=$channel1host



echo "Using incentive: $1"
sleep 1

echo '>>> CLEANING UP...'
echo '' > scenario.log
./foreach.sh $hosts_file 'ntpdate swisstime.ethz.ch'
./foreach.sh $hosts_file 'killall java'
./foreach.sh $hosts_file 'killall vlc'
sleep 10
./foreach.sh $hosts_file 'killall -s 9 vlc'
./foreach.sh $hosts_file 'killall -s 9 java'
sleep 10

echo '>>> STARTING VLC ON STREAMING SOURCES...'
ssh root@$channel1host 'vlc ~/liveshift/F1.avi --sout "#std{access=udp,mux=ts,dst=127.0.0.1:15350}" -I dummy' > /dev/null &
sleep 2
#ssh root@$channel2host 'vlc ~/liveshift/F1.avi --sout "#std{access=udp,mux=ts,dst=127.0.0.1:15350}" -I dummy' > /dev/null &
#sleep 15

echo '>>> STARTING LIVESHIFT ON STREAMING SOURCES...'
ssh root@$channel1host 'cd liveshift;~/jre/bin/java -jar liveshift-2.0.jar -b '$bootstrappeer' -i eth0 -d -r '$1' -s "w:60|p:TestChannel1|w:500|q"' > /dev/null &
sleep 10
#ssh root@$channel2host 'cd liveshift;~/jre/bin/java -jar liveshift-2.0.jar -b '$bootstrappeer' -i eth0 -d -r '$1' -s "w:60|p:TestChannel2|w:500|q"' > /dev/null &
#sleep 3

echo '>>> STARTING LIVESHIFT ON ALL OTHER NODES...'
./foreach.sh $hosts_file 'cd ~/liveshift;~/jre/bin/java -jar ~/liveshift/liveshift-2.0.jar -i eth0 -b '$bootstrappeer' -d -r '$1' -s "w:80|c:TestChannel1|w:360|q"' > /dev/null
sleep 3
#./foreach_hosts.sh hostsB 'cd ~/liveshift;~/jre/bin/java -jar ~/liveshift/liveshift-2.0.jar -i eth0 -b '$bootstrappeer' -d -r '$1' -s "w:80|c:TestChannel2|w:360|q"' > /dev/null
#sleep 3
#./foreach_hosts.sh hostsC 'cd ~/liveshift;~/jre/bin/java -jar ~/liveshift/liveshift-2.0.jar -i eth0 -b '$bootstrappeer' -d -r '$1' -s "w:80|c:TestChannel2|w:180|c:TestChannel1|w:180|q"' > /dev/null
#sleep 3
#./foreach_hosts.sh hostsD 'cd ~/liveshift;~/jre/bin/java -jar ~/liveshift/liveshift-2.0.jar -i eth0 -b '$bootstrappeer' -d -r '$1' -s "w:80|c:TestChannel1|w:180|c:TestChannel2|w:180|q"' > /dev/null

echo exiting;
exit(0);

echo '>>> WAITING FOR RUNS TO FINISH...'
for i in 1 2 3 4 5 6 7 8; do
	for j in 1 2 3 4 5 6; do
		sleep 10
		if [ $(grep "was not found. Stopping LiveShift" scenario.log | wc -l) -gt 0 ]
		then exit
		fi
	done
done
sleep 60

echo '>>> WE SHOULD BE DONE WITH EVERYTHING, CLEANING UP...'
./foreach.sh $hosts_file 'killall java'
sleep 3
./foreach.sh $hosts_file 'killall vlc'
sleep 15

echo '>>> GETTING THE RESULTS AND COMPRESSING THEM...'
rm results/*
./getResults.sh
tar -cvzpf results_`date '+%d%m%y-%H%M%S'`.tgz results
scp results_*.tgz kevin@130.60.157.126:~
rm results_*.tgz
echo "`date`: Worked!" >> worked.log

echo ">>> WE'RE DONE!"
sleep 2

