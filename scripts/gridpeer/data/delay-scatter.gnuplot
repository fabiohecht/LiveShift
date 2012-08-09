#set logscale x
set xlabel "simulation time (ms)"
set ylabel "delay (ms)"

set ytics border nomirror norotate  offset character 0, 0, 0 autofreq 

set key left top
set key autotitle columnheader
#set format x "10^{%L}"
#unset key

set xdata time
set timefmt "%H:%M:%S"

set size .7,.7

set terminal postscript eps enhanced color solid
set output 'delay-scatter.eps'

#set term x11
#set output

#set size 1,1

plot 'delay-scatter.data' using 1:2,\
'delay-scatter.data' using 1:3,\
'delay-scatter.data' using 1:4,\
'delay-scatter.data' using 1:5,\
'delay-scatter.data' using 1:6,\
'delay-scatter.data' using 1:7,\
'delay-scatter.data' using 1:8,\
'delay-scatter.data' using 1:9,\
'delay-scatter.data' using 1:10,\
'delay-scatter.data' using 1:11,\
'delay-scatter.data' using 1:12,\
'delay-scatter.data' using 1:13,\
'delay-scatter.data' using 1:14,\
'delay-scatter.data' using 1:15,\
'delay-scatter.data' using 1:16,\
'delay-scatter.data' using 1:17,\
'delay-scatter.data' using 1:18,\
'delay-scatter.data' using 1:19,\
'delay-scatter.data' using 1:20,\
'delay-scatter.data' using 1:21,\
'delay-scatter.data' using 1:22,\
'delay-scatter.data' using 1:23,\
'delay-scatter.data' using 1:24,\
'delay-scatter.data' using 1:25,\
'delay-scatter.data' using 1:26,\
'delay-scatter.data' using 1:27,\
'delay-scatter.data' using 1:28,\
'delay-scatter.data' using 1:29,\
'delay-scatter.data' using 1:30,\
'delay-scatter.data' using 1:31,\
'delay-scatter.data' using 1:32


