set terminal postscript eps enhanced color  font "Times-Roman,15"
set output 'delay-per-class-simple.eps'

set size .65,.5

#set multiplot

#
# First plot  (large)
#


#set bmargin at screen 0.20
#set lmargin screen 0.11

#set logscale x
set ylabel "playback lag (s)"
#set y2label "skipped blocks (%)"

set ytics border  norotate  offset character 0, 0, 0 autofreq 
#set y2tics border norotate  offset character 0, 0, 0 autofreq 

set key left top horiz
set key autotitle columnheader
set xtics 
set xlabel "holding time (min)"

set xrange [ 0 : 55 ] noreverse nowriteback
set yrange [ 0 : * ] noreverse nowriteback

plot 'delay-per-class.data' using 1:2 axes x1y1 with l lt 1 lc rgb "black" title "avg LU", \
     'delay-per-class.data' using 1:6 axes x1y1 with l lt 2 lc rgb "black" title "avg HU", \
     'delay-per-class.data' using 1:3 axes x1y1 with l lt 1 lc rgb "blue" title "avg min LU", \
     'delay-per-class.data' using 1:7 axes x1y1 with l lt 2 lc rgb "blue" title "avg min HU"

#'delay-per-class.data' using 1:4 axes x1y2 with lp pt 4 lc rgb "red", \
#'delay-per-class.data' using 1:8 axes x1y2 with lp pt 5 lc rgb "blue"

# samples minigraph

#set ylabel "samples"
#set ytics border  norotate  offset character 0, 0, 0 autofreq 
#unset y2label
#unset y2tics
#set xtics

#unset key
#set lmargin screen 0.11
#set bmargin screen 0.1
#set tmargin screen 0.18

#set ytics 2000
#set yrange [0:7500]

#set key right top
#set key autotitle columnheader

#plot 'delay-per-class.data' using 1:5 axes x1y1 with line lt 2 lc rgb "red" title "LU", \
#     'delay-per-class.data' using 1:9 axes x1y1 with line lt 1 lc rgb "blue" title "HU"

#set nomultiplot


