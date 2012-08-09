#parameters (by substitution)
#%SCENARIO%: name of the scenario (to get the right data file)
#%SUFFIX%: to differentiate different graphs

scenario=%SCENARIO%

set xlabel "liveness"
set ylabel "continuity"

set ytics border norotate  offset character 0, 0, 0 autofreq 
#set format y "%6.2f"

set key bottom left box
#set key under box

set bar .5
set size .6,.5

#set xrange [:*1.1]
#set yrange [*:1.1]

set terminal postscript eps enhanced color  font "Times-Roman,15"
set output sprintf("liveness-continuity-s%d-%SUFFIX%-eb.eps",scenario)

plot for [i=0:3] sprintf("s%d-%SUFFIX%-tsp-eb.data",scenario) using 1:3*i+3:3*i+2:3*i+4 title columnheader(3*i+2) ls i+4 lc rgb "#000000" with xyerrorbars,\
     sprintf("s%d-%SUFFIX%-agg-eb.data",scenario) using 2:4 title '' ls 1 lc rgb "#000000" with lines


