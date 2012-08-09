#set logscale x
unset xlabel # "message type"
set ylabel "upstream utilization (%)"

set ytics border norotate  offset character 0, 0, 0 autofreq 

#set format x "10^{%L}"
set key left top
set key autotitle columnheader
set size .65, .5
set terminal postscript eps enhanced color solid font "Times-Roman,15"
set output 'upstream-utilization-per-class-scenarios.eps'

#set term x11
#set output

#set size 1,1

set boxwidth 1
set style fill   solid 1.00 border -1
set style histogram clustered gap 1 title  offset character 0, 0, 0
set style data histograms
#set xtics border in scale 1,0.5 nomirror rotate by -45  offset character 0, 0, 0
set xtics in scale 0,0 nomirror #rotate by -35

#set y2label "size (kbyte)"
#set y2tics border norotate  offset character 0, 0, 0 autofreq 
#set ytics nomirror

plot 'upstream-utilization-per-class-scenarios.data' using 2:xtic(1) ti col, \
 'upstream-utilization-per-class-scenarios.data' using 3:xtic(1) ti col, \
 'upstream-utilization-per-class-scenarios.data' using 4:xtic(1) ti col

