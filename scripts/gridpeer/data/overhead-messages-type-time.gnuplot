#set logscale x
set xlabel "message type"
set ylabel "total # of messages sent"

set ytics border nomirror norotate  offset character 0, 0, 0 autofreq 

#set format x "10^{%L}"
unset key

set terminal postscript eps enhanced color solid
set output 'overhead-messages-type-time.eps'

#set term x11
#set output

set xdata time
set timefmt "%H:%M:%S"

#set size 1,1

set boxwidth 0.9 absolute
set style fill   solid 1.00 border -1
set style histogram clustered gap 1 title  offset character 0, 0, 0
set style data histograms
set xtics  nomirror rotate by -15 

# set terminal png transparent nocrop enhanced font arial 8 size 420,320 
# set output 'fillbetween.1.png'
set style data lines

#set xrange [ 10.0000 : * ] noreverse nowriteback  # (currently [:10.0000] )
#set yrange [ 0.00000 : 175.000 ] noreverse nowriteback
plot 'overhead-messages-type-time.data' u 1:3:xtic(2) w filledcu


