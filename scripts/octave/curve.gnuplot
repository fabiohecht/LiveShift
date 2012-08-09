set terminal postscript eps enhanced color  font "Times-Roman,15"
set output '~/liveshift/tmp/test-curve.eps'

#set size .5,.5

set style data lines
set key right top
set key autotitle columnheader

set title "test curve" 
set ylabel "holding time (min)"
set xlabel "playback lag (s)"

set xrange [ 0 : * ] noreverse nowriteback
set yrange [ 0 : * ] noreverse nowriteback

#set ytics ("0" 0, "10" 6.67, "20" 13.33, "30" 20, "40" 26.67, "50" 33.33, "60" 39)
#set xtics ("0" 0, "5" 4, "33421" 39)

plot '~/liveshift/tmp/percentiles' u 1:2 , \
  '~/liveshift/tmp/percentiles' u 1:3 , \
  '~/liveshift/tmp/percentiles' u 1:4 
