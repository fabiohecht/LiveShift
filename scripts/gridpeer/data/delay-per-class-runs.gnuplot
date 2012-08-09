set terminal postscript eps enhanced color solid
set output 'delay-per-class.eps'

set size .6,.6

set multiplot

#
# First plot  (large)
#
set lmargin at screen 0.10
set rmargin at screen 0.48
set bmargin at screen 0.20
set tmargin at screen .55

#set logscale x
set ylabel "delay (s)"
#set y2label "skipped blocks (%)"

set ytics border  norotate  offset character 0, 0, 0 autofreq 
#set y2tics border norotate  offset character 0, 0, 0 autofreq 

set key left top
set key autotitle columnheader
set xtics 
set xdata time
set timefmt "%H:%M:%S"
set format x ""

#set xrange ["00:00:00":"00:20:00"]
#set y2range [0:5]
#set yrange [0:20]

plot 'delay-per-class.data' using 1:2:3 axes x1y1 with errorbars pt 6 lc rgb "red", \
'delay-per-class.data' using 1:4:5 axes x1y1 with errorbars pt 8 lc rgb "red", \
'delay-per-class.data' using 1:7:8 axes x1y1 with errorbars pt 7 lc rgb "blue",\
'delay-per-class.data' using 1:9:10 axes x1y1 with errorbars pt 9 lc rgb "blue"

#'delay-per-class.data' using 1:8:9 axes x1y2 with lp pt 4 lc rgb "red", \
#'delay-per-class.data' using 1:16:17 axes x1y2 with lp pt 5 lc rgb "blue"

# samples minigraph

set xlabel "holding time (min)"
set ylabel "samples"
set ytics border  norotate  offset character 0, 0, 0 autofreq 
unset y2label
unset y2tics
set xtics
set format x "%M"

unset key
set lmargin screen 0.10
set bmargin screen 0.1
set rmargin screen 0.48
set tmargin screen 0.18

set ytics 10000
#set yrange [0:5000]

set key right top
set key autotitle columnheader

plot 'delay-per-class.data' using 1:6 axes x1y1 with line lc rgb "red" title "LU", \
'delay-per-class.data' using 1:11 axes x1y1 with line lc rgb "blue" title "HU"

set nomultiplot


