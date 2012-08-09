#$1==filename (data2)
#$2==column number in $1 is normally (input file format: ht,avglag,avgskip,avgplay) >=2
#$3==holding time

#output = the CDF for the holding time spedified

if [ -e $2 ]
then
	$2=1
fi

	#echo ht=$ht
	
	cnt=`egrep "^$3 " $1|wc -l |cut -f 1 -d ' '`
	samplerate=.1
	lastsample=0
	ln=1
	for p in `egrep "^$3 " $1|cut -d" " -f$2|sort -n`
	do

		thissample=`awk "BEGIN {print $ln/$cnt}"`
		if [[ `awk "BEGIN {print $lastsample+$samplerate < $thissample||$lastsample==0||$ln==$cnt?1:0}"` > 0 ]]
		then
			echo $p $thissample
			lastsample=$thissample

		fi
		ln=$(($ln+1))	
	done
