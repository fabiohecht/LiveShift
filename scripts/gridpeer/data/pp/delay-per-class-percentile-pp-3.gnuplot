#variables to replace:
#%PREFIX
#%POLICY
#%SCENARIO
#%YRANGE
set terminal postscript eps enhanced color  font "Times-Roman,15"
set output '%PREFIX-delay-linesplot-%POLICY-%SCENARIO.eps'

set title '%POLICY %SCENARIO'
set size .5,.5

set xlabel "holding time (min)"
set ylabel "playback lag (s)"

set ytics border  norotate  offset character 0, 0, 0 autofreq 

set key under horiz samplen 3
#set xtics 5
#set mxtics 5
#set ytics 10
#set mytics 5


set xrange [ 0 : 18 ] noreverse nowriteback
set yrange [ 0 : %YRANGE ] noreverse nowriteback

pol='%POLICY'

plot \
'%PREFIX-%POLICY-%PAR1-%SCENARIO-lag.data2-perc2' using 1:4 with linespoints lt 1 lw 1 pt 1 lc rgb "black" title pol[1:2]."-%PAR1", \
'%PREFIX-%POLICY-%PAR2-%SCENARIO-lag.data2-perc2' using 1:4 with linespoints lt 2 lw 1 pt 2 lc rgb "black" title pol[1:2]."-%PAR2", \
'%PREFIX-%POLICY-%PAR3-%SCENARIO-lag.data2-perc2' using 1:4 with linespoints lt 3 lw 1 pt 8 lc rgb "black" title pol[1:2]."-%PAR3"

