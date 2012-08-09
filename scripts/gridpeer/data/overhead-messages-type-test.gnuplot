set terminal postscript eps enhanced color solid font "Times-Roman,15"
set output 'overhead-messages-type-with-dht.eps'

set size .65, .5

set style fill   solid 1.00 border -1
#set style histogram clustered gap 1 title  offset character 0, 0, 0
#set style data histograms errorbars
set boxwidth .5 

unset key

unset xlabel # "message type"
set ylabel "sent messages per second"

set ytics border norotate  offset character 0, 0, 0 autofreq 
set ytics 1
set mytics 5
set xtics in scale 0,0 nomirror rotate by -35

set xtics ("Ping" 1, "PeerSuggestion" 2, "Disconnect" 3)


plot 'overhead-messages-type-with-dht.data' using 1:2:3 with boxerrorbars

#, \
#errorbars'' using 3:xtic(1) ti col with boxerrorbars

#, \
# 'overhead-messages-type.data' using 4:xtic(1) ti col axes x1y2 fs pattern 4 lt 3


