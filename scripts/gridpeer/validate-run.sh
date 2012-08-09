#!/bin/bash

#validates a run

#$1 pass2 file
#$2 first peer to report (normally 7)
#$3 last peer to report (depends on scenario, s4=171)

#validates that every peer from x1 to x2 reports something
#validates that there is a maximum difference in number of reports from every peer

p1=$2
p2=$3
difflimit=.9

sum=0
cnt=0
p=-1
ispeer=1

for l in `grep DELAY $1 |sed 's/:/ /'|awk '{cnt[substr($1,2)]++} END {for (i in cnt) {print i,cnt[i]}}'|sort -n`
do

if [ $ispeer -eq 1 ]
then
#  echo p=$l
  if [ $p -eq -1 ]
  then
    if [ $l -ne $p1 ]
    then
      echo "First peer $p1 not present"
      exit 1
    fi
    p=$l
  else
    p=$(($p+1))
    if [ $p -ne $l ]
    then
      echo "Peer $p not present"
      exit 2
    fi
  fi

  ispeer=0
else
  #echo r=$l
  
  sum=$(( $sum + $l ))
  cnt=$(( $cnt + 1 ))

  avgok=`echo $sum $cnt $l $difflimit|awk '{x=$1/$2/$3; if ((x<1 && x<$4) || (x>1 && x-1>1-$4)) {print "no"}}'`


  echo "$p $sum/$cnt="$(($sum/$cnt))" > $l*$difflimit ? $avgok "
  
  if [ ! -e $avgok ]
  then
    echo "Peer $p underreported, perhaps it died at some point"
    exit 4
  fi
  ispeer=1
fi

done



    if [ $p -ne $p2 ]
    then
      echo "Last peer is $p not $p2"
      exit 3
    fi

echo "Final average is "`echo $sum $cnt|awk '{print $1/$2}'`

exit 0

