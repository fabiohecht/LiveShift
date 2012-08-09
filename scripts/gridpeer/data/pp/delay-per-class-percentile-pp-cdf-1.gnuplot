#%PREFIX
#%POLICY
#%SCENARIO
#%YRANGE
set terminal postscript eps enhanced color  font "Times-Roman,15"
set output '%PREFIX-delay-CDF-%POLICY-%SCENARIO.eps'

set title '%POLICY %SCENARIO at 10 min holding time'
set size .5,.5

set xlabel "playback lag (s)"
set ylabel "CDF"

set ytics border  norotate  offset character 0, 0, 0 autofreq 

set key under horiz samplen 3
#set xtics 5
#set mxtics 5
#set ytics 10
#set mytics 5

set logscale x

set xrange [ 1 : ] noreverse nowriteback
set yrange [ 0 : 1 ] noreverse nowriteback
pol='%POLICY'

plot \
'%PREFIX-%POLICY-%SCENARIO-lag.data2-perc-cdf-10' with lines lt 1 lw 1 lc rgb "black" title '', \
'%PREFIX-%POLICY-%SCENARIO-lag.data2-perc-cdf-10' every 4 with points lt 1 lw 1 pt 1 lc rgb "black" title '', \
-1 with linespoints pt 1 lt 1 lw 1 lc rgb "black" title pol[1:2]
