#set logscale y
unset xlabel # "message type"
set ylabel "size of sent messages (kbyte)                       "

set ytics border norotate  offset character 0, 0, 0 autofreq 

#set format x "10^{%L}"
#set key left top
#set key autotitle columnheader
unset key
set size .65, .5

set terminal postscript eps enhanced color solid font "Times-Roman,15"
set output 'overhead-messages-type-size.eps'

#set term x11
#set output

#set size 1,1

set ytics 10
set mytics 5

set boxwidth 1
set style fill   solid 1.00 border -1
set style histogram clustered gap 1 title  offset character 0, 0, 0
set style data histograms
#set xtics border in scale 1,0.5 nomirror rotate by -45  offset character 0, 0, 0
set xtics in scale 0,0 nomirror rotate by -67

#set y2label "size (kbyte)"
#set y2tics border norotate  offset character 0, 0, 0 autofreq 
set ytics #nomirror

plot 'overhead-messages-type.data' using 4:xtic(1) ti col fs pattern 0
#, \
# 'overhead-messages-type.data' using 4:xtic(1) ti col axes x1y2 fs pattern 4 lt 3


