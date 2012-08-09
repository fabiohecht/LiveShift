#set logscale x
set xlabel "simulation time (min)"
set ylabel "upstream utilization (%)"

set ytics border norotate  offset character 0, 0, 0 autofreq 

#set key center top
#set key autotitle columnheader
#set format x "10^{%L}"
unset key

set size .65

set xdata time
set timefmt "%H:%M:%S"
set format x "%M"
#set xrange ["00:00:00":"00:10:00"]

set terminal postscript eps enhanced color solid
set output 'upstream-utilization.eps'

#set term x11
#set output

#set size 1,1

plot 'upstream-utilization.data' using 1:2 with line

