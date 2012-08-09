#variables to replace:
#%PREFIX
#%POLICY
#%SCENARIO

set terminal postscript eps enhanced color  font "Times-Roman,15"
set output '%PREFIX-delay-boxplot-%POLICY-%SCENARIO.eps'

set title '%POLICY %SCENARIO'
set size .3,.45

set ylabel "playback lag (s)"
set xlabel "holding time (min)"

set ytics border  norotate  offset character 0, 0, 0 autofreq  scale 1
set xtics ("1" 1,"2" 2 1,"3" 3, "4" 4 1,"10" 5, "11" 6 1,"20" 7) nomirror  scale 0,.5
set mxtics 1
set xrange [-.1:8.1]

unset key #under horizontal samplen 2

set boxwidth 0.65 absolute

pol='%POLICY'

plot '%PREFIX-%POLICY-%SCENARIO-lag.data-perc' using 1:3:2:6:5  with candlesticks title pol[1:2] lc rgb "black" whiskerbars, \
     ''                 using 1:4:4:4:4 with candlesticks lc rgb "black"  notitle


