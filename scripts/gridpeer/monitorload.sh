lockfile=/tmp/LiveShift/load.lock

cpucount=`cat /proc/cpuinfo|grep -c processor`

if [ ! -e $lockfile ]; then
   trap "rm -f $lockfile; exit" INT TERM EXIT
   touch $lockfile
   while  [ -e $lockfile ]
      do echo `hostname` $cpucount `date +%s` `cat /proc/loadavg` java:`ps -u hecht | grep -c java` >> $1
      sleep 1
   done
   rm $lockfile
   trap - INT TERM EXIT
else
   echo "load monitor is already running"
fi



