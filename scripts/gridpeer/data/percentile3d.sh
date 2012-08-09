#$1==filename
#$2==column number in $1 is normally (input file format: ht,avglag,avgskip,avgplay) >=2
#$3==percentiles (0..100) e.g. 50,80,95

if [ -e $2 ]
then
	$2=1
fi


hts=`cut -f1 -d' ' $1|sort -n|uniq`

cnt=1
for ht in $hts
do
	#echo ht=$ht

	temp=`mktemp`
	
	wc=`egrep "^$ht " $1|wc -l |cut -f 1 -d ' '`

	egrep "^$ht " $1|sort -n -k$2,$2>$temp

	result=$cnt
	for p in `echo $3|tr "," "\n"`
	do
		
		element=`echo $p $wc |awk '{printf ("%d\n",$1/100*$2 + 0.5)}'`

		if [ $element -eq 0 ]
		then
			element=1
		fi

		#echo p=$p wc=$wc elem=$element
		
		res=`head -$element $temp|tail -1|cut -d' ' -f$2`

		echo $ht $p $res
	done
	
	echo
	
	rm $temp
	

	cnt=$(($cnt+2))
#	cnt=`echo $cnt| tr 'a-y' 'b-z'`
done

