#set logscale x
set xlabel "simulation time (min)"
set ylabel "sent messages per peer per second"

set ytics border norotate  offset character 0, 0, 0 autofreq

#set format x "10^{%L}"
unset key

set size .65

set xdata time
set timefmt "%H:%M:%S"
set format x "%M"
#set xrange ["00:00:00":"00:10:00"]

set terminal postscript eps enhanced color solid
set output 'overhead-messages.eps'

#set term x11
#set output

#set size 1,1

plot 'overhead-messages.data' using 1:2 with line


