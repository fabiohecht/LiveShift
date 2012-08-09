#parameters (by substitution)
#%SCENARIO%: name of the scenario (to get the right data file)
#%SUFFIX%: to differentiate different graphs

scenario=%SCENARIO%

set xlabel "liveness"
set ylabel "continuity"

set ytics border norotate  offset character 0, 0, 0 autofreq 

set key bottom left box
#set key under box

set size .6,.5

#set xrange [:1]
set yrange [*:1]

set terminal postscript eps enhanced color  font "Times-Roman,15"
set output sprintf("liveness-continuity-s%d-%SUFFIX%.eps",scenario)

plot for [i=2:5] sprintf("s%d-%SUFFIX%-tsp.data",scenario) using 1:i title columnheader(i) lc rgb "#000000" with points,\
     sprintf("s%d-%SUFFIX%-agg.data",scenario) using 2:3 title '' ls 1 lc rgb "#000000" with lines


