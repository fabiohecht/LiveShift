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
set output 'delay-scatter-dot.eps'

#set term x11
#set output

#set size 1,1

plot 'delay-scatter-dot.data' using 1:2 with dots

