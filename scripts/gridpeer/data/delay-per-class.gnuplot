set terminal postscript eps enhanced color  font "Times-Roman,15"
set output 'delay-per-class.eps'

set size .65,.65

set multiplot

#
# First plot  (large)
#


set bmargin at screen 0.20
set lmargin screen 0.11

#set logscale x
set ylabel "playback lag (s)"
#set y2label "skipped blocks (%)"

set ytics border  norotate  offset character 0, 0, 0 autofreq 
#set y2tics border norotate  offset character 0, 0, 0 autofreq 

set key left top
set key autotitle columnheader
set xtics 
set xdata time
set timefmt "%H:%M:%S"
set format x ""

#set xrange ["00:00:00":"00:18:00"]
#set y2range [0:5]
#set yrange [0:20]

plot 'delay-per-class.data' using 1:2 axes x1y1 with lp lt 1 pt 6 lc rgb "red" title "avg LU", \
     'delay-per-class.data' using 1:6 axes x1y1 with lp lt 1 pt 7 lc rgb "blue" title "avg HU", \
     'delay-per-class.data' using 1:3 axes x1y1 with lp lt 1 pt 8 lc rgb "red" title "avg min LU", \
     'delay-per-class.data' using 1:7 axes x1y1 with lp lt 1 pt 9 lc rgb "blue" title "avg min HU"

#'delay-per-class.data' using 1:4 axes x1y2 with lp pt 4 lc rgb "red", \
#'delay-per-class.data' using 1:8 axes x1y2 with lp pt 5 lc rgb "blue"

# samples minigraph

set xlabel "holding time (min)"
set ylabel "samples"
set ytics border  norotate  offset character 0, 0, 0 autofreq 
unset y2label
unset y2tics
set xtics
set format x "%M"

unset key
set lmargin screen 0.11
set bmargin screen 0.1
set tmargin screen 0.18

set ytics 2000
set yrange [0:7500]

set key right top
set key autotitle columnheader

plot 'delay-per-class.data' using 1:5 axes x1y1 with line lt 2 lc rgb "red" title "LU", \
     'delay-per-class.data' using 1:9 axes x1y1 with line lt 1 lc rgb "blue" title "HU"

set nomultiplot


