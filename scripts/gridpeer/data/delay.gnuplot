set terminal postscript eps enhanced color solid
set output 'delay.eps'

set size .8,.8

set multiplot

#
# First plot  (large)
#
set lmargin at screen 0.15
set rmargin at screen 0.7
set bmargin at screen 0.25
set tmargin at screen .75

#set logscale x
set ylabel "delay (s)"
set y2label "skipped blocks"

set ytics border  nomirror norotate  offset character 0, 0, 0 autofreq 
set y2tics border nomirror norotate  offset character 0, 0, 0 autofreq 

set key left top
set key autotitle columnheader
unset xtics
set xdata time
set timefmt "%H:%M:%S"

#set xrange ["00:00:00":"00:20:00"]

plot 'delay.data' using 1:2 axes x1y1, \
'delay.data' using 1:3 axes x1y1, \
'delay.data' using 1:4 axes x1y2 with line

# samples minigraph

set xlabel "holding time (ms)"
set ylabel "samples"
set ytics border nomirror norotate  offset character 0, 0, 0 autofreq 
unset y2label
unset y2tics
set xtics
#set format x "%g"

unset key
set lmargin screen 0.15
set bmargin screen 0.1
set rmargin screen 0.7
set tmargin screen 0.23

set ytics 1000
set yrange [0:5000]

plot 'delay.data' using 1:5 axes x1y1 with line

set nomultiplot


