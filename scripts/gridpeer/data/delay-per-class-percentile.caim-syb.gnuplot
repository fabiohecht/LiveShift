set terminal postscript eps enhanced color  font "Times-Roman,15"
set output 'delay-per-class-percentile.eps'

set size .65,.5

set ylabel "playback lag (s)"

set ytics border  norotate  offset character 0, 0, 0 autofreq 

set key left top horiz
set key autotitle columnheader
set xtics 
set xlabel "holding time (min)"

set xrange [ 0 : 8.5 ] noreverse nowriteback
set yrange [ 0 : * ] noreverse nowriteback

plot 'delay-per-class-percentile.data' using 2:4 i 0 with lines lt 1 lc rgb "red" title "50% LU", \
     'delay-per-class-percentile.data' using 2:4 i 1 with lines lt 2 lc rgb "red" title "80% LU", \
     'delay-per-class-percentile.data' using 2:4 i 2 with lines lt 3 lc rgb "red" title "95% LU", \
     'delay-per-class-percentile.data' using 2:5 i 0 with lines lt 1 lc rgb "blue" title "50% HU", \
     'delay-per-class-percentile.data' using 2:5 i 1 with lines lt 2 lc rgb "blue" title "80% HU", \
     'delay-per-class-percentile.data' using 2:5 i 2 with lines lt 3 lc rgb "blue" title "95% HU", \
     'delay-per-class-percentile.data' using 2:3 i 2 with lines lt 1 lc rgb "green" title "SYB" 
#, \
#     'delay-per-class-percentile.data' using 2:5 i 0 with lines lt 1 lc rgb "green" title "50% all", \
#     'delay-per-class-percentile.data' using 2:5 i 1 with lines lt 2 lc rgb "green" title "80% all", \
#     'delay-per-class-percentile.data' using 2:5 i 2 with lines lt 3 lc rgb "green" title "95% all"


