set terminal postscript eps enhanced color solid font "Times-Roman,15"
set output 'block-retry-heatmap.eps'

set size .6, .5


set xlabel "# neighbors"
set ylabel "time (s)"
set cblabel "CDF"


set xrange [ 0.00000 : 50.0000 ] noreverse nowriteback
set yrange [ 0.00000 : 25.0000 ] noreverse nowriteback

set palette rgbformulae 22, 13, -31

plot 'tmp-block-retry-freq-cdf' u 1:2:3 w image

