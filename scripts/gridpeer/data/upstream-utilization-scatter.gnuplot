#set logscale x
set xlabel "simulation time (ms)"
set ylabel "upstream utilization"

set ytics border nomirror norotate  offset character 0, 0, 0 autofreq 

set key center top
set key autotitle columnheader
#set format x "10^{%L}"
#unset key

set size .7,.7

set xdata time
set timefmt "%H:%M:%S"

set terminal postscript eps enhanced color solid
set output 'upstream-utilization-scatter.eps'

#set term x11
#set output

#set size 1,1

plot 'upstream-utilization.data' using 1:3,\
'upstream-utilization.data' using 1:4,\
'upstream-utilization.data' using 1:5,\
'upstream-utilization.data' using 1:6,\
'upstream-utilization.data' using 1:7,\
'upstream-utilization.data' using 1:8,\
'upstream-utilization.data' using 1:9,\
'upstream-utilization.data' using 1:10,\
'upstream-utilization.data' using 1:11,\
'upstream-utilization.data' using 1:12,\
'upstream-utilization.data' using 1:13,\
'upstream-utilization.data' using 1:14,\
'upstream-utilization.data' using 1:15,\
'upstream-utilization.data' using 1:16,\
'upstream-utilization.data' using 1:17,\
'upstream-utilization.data' using 1:18,\
'upstream-utilization.data' using 1:19,\
'upstream-utilization.data' using 1:20,\
'upstream-utilization.data' using 1:21,\
'upstream-utilization.data' using 1:22


