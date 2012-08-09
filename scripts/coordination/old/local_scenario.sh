
echo "Starting VLC on s1";
	vlc F1.avi --sout "#std{access=udp,mux=ts,dst=127.0.0.1:15350}" -I dummy &> output.vlc & 

sleep 1

echo "Creating boostrap and source peer s1";
        java -jar dist/liveshift-2.0.jar -i lo -n p$i -pp2p 11101 -psig 11102 -b 127.0.0.1:11101 -d -r dummy -p dummy -s "w:2|p:FormulaOne|w:3600|q" &> output.s1 &

sleep 5

echo "Creating peer p1";
java -jar dist/liveshift-2.0.jar -i lo -n p1 -pp2p 11111 -psig 11112 -b 127.0.0.1:11101 -d -r PSH -p dummy -s "w:2|c:FormulaOne|w:3600|q" -history "p2:-1000" &> output.p1 &

sleep 1

echo "Creating peer p2";
java -jar dist/liveshift-2.0.jar -i lo -n p2 -pp2p 11121 -psig 11122 -b 127.0.0.1:11101 -d -r PSH -p dummy -s "w:2|c:FormulaOne|w:3600|q" -history "p1:1000,p3:-1000" &> output.p2 &

sleep 1

echo "Creating peer p3";
java -jar dist/liveshift-2.0.jar -i lo -n p3 -pp2p 11131 -psig 11132 -b 127.0.0.1:11101 -d -r PSH -p dummy -s "w:2|c:FormulaOne|w:3600|q" -history "p2:1000,p4:500" &> output.p3 &

sleep 1

echo "Creating peer p4";
java -jar dist/liveshift-2.0.jar -i lo -n p4 -pp2p 11141 -psig 11142 -b 127.0.0.1:11101 -d -r PSH -p dummy -s "w:2|c:FormulaOne|w:3600|q" -history "p3:-500" &> output.p4 &

echo "All done.";

