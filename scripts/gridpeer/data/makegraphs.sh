#!/bin/bash

mypath=$(dirname $(readlink -f $0))

echo 'graphing'

gnuplot $mypath/delay.gnuplot 2> /dev/null &
gnuplot $mypath/delay-per-class.gnuplot 2> /dev/null &
gnuplot $mypath/delay-per-class-simple.gnuplot 2> /dev/null &
gnuplot $mypath/delay-scatter.gnuplot 2> /dev/null &
gnuplot $mypath/upstream-utilization.gnuplot 2> /dev/null &
gnuplot $mypath/upstream-utilization-scatter.gnuplot 2> /dev/null &
gnuplot $mypath/overhead-messages.gnuplot 2> /dev/null &
gnuplot $mypath/overhead-messages-type.gnuplot 2> /dev/null &
gnuplot $mypath/delay-per-class-percentile.gnuplot 2> /dev/null &

wait

echo 'done'
