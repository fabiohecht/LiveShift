#!/bin/bash


while read i
do

        if [ ! ${i:0:1} = '#' ]
        then
                ac=0
                count=0
                echo "Processing host $i"

while read j
do

        if [ ! ${i:0:1} = '#' ]
        then
#echo "ssh $i 'ping $j -c 1'"
                ssh $i "ping $j -c 1"< /dev/null>/dev/null
                ret=$?

if [ $ret != 0 ]
then

                echo "$ret: routing problem from $i to $j <<<<<<<<<<<<<<<<<<<"
else
                echo "$ret: routing OK from $i to $j"
fi

                ac=$(($ac+$ret))
                count=$(($count+1))
#echo " host $j added $ac/$count"
        fi
done < $1


        fi
done < $1


