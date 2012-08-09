set terminal postscript eps enhanced color  font "Times-Roman,15"
set output 'delay-per-class-percentile-fixed.eps'

set size .6,.5

set ylabel "playback lag (s)"

set ytics border  norotate  offset character 0, 0, 0 autofreq 

set key left top horiz samplen 3
set key autotitle columnheader
set xtics 5
set mxtics 5
set ytics 10
set mytics 5
set xlabel "holding time (min)"

set xrange [ 0 : 55 ] noreverse nowriteback
set yrange [ 0 : 63 ] noreverse nowriteback

plot 'delay-per-class-percentile-fixed.data' using 2:3 i 0 with lines lt 1 lw 1 lc rgb "black" title "50% LU", \
     'delay-per-class-percentile-fixed.data' using 2:5 i 0 with lines lt 1 lw 4 lc rgb "black" title "50% HU", \
     'delay-per-class-percentile-fixed.data' using 2:3 i 1 with lines lt 2 lw 1 lc rgb "black" title "80% LU", \
     'delay-per-class-percentile-fixed.data' using 2:5 i 1 with lines lt 2 lw 4 lc rgb "black" title "80% HU", \
     'delay-per-class-percentile-fixed.data' using 2:3 i 2 with lines lt 3 lw 1 lc rgb "black" title "95% LU", \
     'delay-per-class-percentile-fixed.data' using 2:5 i 2 with lines lt 3 lw 4 lc rgb "black" title "95% HU"
#, \
#     'delay-per-class-percentile-fixed.data' using 2:5 i 0 with lines lt 1 lc rgb "green" title "50% all", \
#     'delay-per-class-percentile-fixed.data' using 2:5 i 1 with lines lt 2 lc rgb "green" title "80% all", \
#     'delay-per-class-percentile-fixed.data' using 2:5 i 2 with lines lt 3 lc rgb "green" title "95% all"


