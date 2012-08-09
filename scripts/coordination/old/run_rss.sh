echo "Using incentive: $1"
sleep 1

echo '>>> CLEANING UP...'
echo '' > scenario.log
./foreach.sh 'killall java'
./foreach.sh 'killall vlc'
sleep 10
./foreach.sh 'killall -s 9 vlc'
./foreach.sh 'killall -s 9 java'
sleep 10

echo '>>> STARTING VLC ON STREAMING SOURCES...'
ssh root@192.168.10.11 'vlc ~/liveshift/F1.avi --sout "#std{access=udp,mux=ts,dst=127.0.0.1:15350}" -I dummy' > /dev/null &
sleep 2
ssh root@192.168.10.12 'vlc ~/liveshift/F1.avi --sout "#std{access=udp,mux=ts,dst=127.0.0.1:15350}" -I dummy' > /dev/null &
sleep 15

echo '>>> STARTING LIVESHIFT ON STREAMING SOURCES...'
ssh root@192.168.10.11 'cd liveshift;java -jar liveshift-2.0.jar -b localhost -i eth0 -d -r '$1' -s "w:60|p:TestChannel1|w:480|q"' > /dev/null &
sleep 10
ssh root@192.168.10.12 'cd liveshift;java -jar liveshift-2.0.jar -b 192.168.10.11 -i eth0 -d -r '$1' -s "w:60|p:TestChannel2|w:480|q"' > /dev/null &
sleep 3

echo '>>> STARTING LIVESHIFT ON ALL OTHER NODES...'
./foreach_hosts.sh hostsRSSA 'cd ~/liveshift;java -jar ~/liveshift/liveshift-2.0.jar -i eth0 -b 192.168.10.11 -d -r '$1' -rsts "80|40|0.5|360"' > /dev/null
sleep 3
# Freeriders
./foreach_hosts.sh hostsRSSB 'cd ~/liveshift;java -jar ~/liveshift/liveshift-2.0.jar -i eth0 -f -b 192.168.10.11 -d -r '$1' -rsts "80|40|0.5|360"' > /dev/null

echo '>>> WAITING FOR RUNS TO FINISH...'
for i in 1 2 3 4 5 6 7 8
	do sleep 60
done
sleep 60

echo '>>> WE SHOULD BE DONE WITH EVERYTHING, CLEANING UP...'
./foreach.sh 'killall java'
sleep 3
./foreach.sh 'killall vlc'
sleep 15

echo '>>> GETTING THE RESULTS AND COMPRESSING THEM...'
rm results/*
./getResults.sh
tar -cvzpf results_rss_`date '+%d%m%y-%H%M%S'`.tgz results
scp results_rss_*.tgz kevin@130.60.157.126:~
rm results_rss_*.tgz
echo "`date`: Worked!" >> worked.log

echo ">>> WE'RE DONE!"
sleep 2

