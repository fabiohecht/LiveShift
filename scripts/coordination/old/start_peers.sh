
i=1

echo "Creating boostrap peer (p1)";
        java -jar liveshift-2.0.jar -i eth0 -n p$i -pp2p 11${i}1 -psig 11${i}2 -b 127.0.0.1:1111 -d -s "w:2|p:FormulaOne|w:3600|q" &> output.$i &

sleep 5

for i in `seq 2 $1`;
do
	echo "Creating peer $i";
	java -jar liveshift-2.0.jar -i eth0 -n p$i -pp2p 11${i}1 -psig 11${i}2 -b 127.0.0.1:1111 -d -s "w:2|c:FormulaOne|w:3600|q" &> output.$i &

	sleep 2
done

