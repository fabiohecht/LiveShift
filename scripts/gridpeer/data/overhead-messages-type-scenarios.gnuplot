#set logscale y2
#set logscale y
unset xlabel # "message type"
set ylabel "number of sent messages"

set ytics border norotate  offset character 0, 0, 0 autofreq 

set key outside below enhanced box linetype -1 linewidth 1.000 autotitle columnheader

set size 1
set terminal postscript eps enhanced color solid font "Times-Roman,15"
set output 'overhead-messages-type.eps'


set boxwidth 1
set style fill   solid 1.00 border -1
set style histogram columnstacked #gap 1 title  offset character 0, 0, 0
set style data histograms
#set xtics border in scale 1,0.5 nomirror rotate by -45  offset character 0, 0, 0
set xtics in scale 0,0 nomirror rotate by -35

set y2label "size (kbyte)"
set y2tics border norotate  offset character 0, 0, 0 autofreq 
set ytics nomirror


#set xtics   ("Disconnect" 0.00000, "PeerSuggestion" 1.00000, "Granted/BackInQ" 2.00000, "(Not)Interested" 3.00000, "Ping" 4.00000, "BlockRequest" 5.00000, "SubscribeRequest" 6.00000, "(Not)Subscribed" 7.00000, "Have" 8.00000, "BlockReply" 9.00000)

plot newhistogram "S6", 'x6-s6/overhead-messages-type.data' using 2:xtic(1) t 1,\
     newhistogram "S7", 'x6-s7/overhead-messages-type.data' using 2:xtic(1) t 2


# 'overhead-messages-type-scenarios.data' using 4:ytic(1) ti col axes x1y2 , \
# 'overhead-messages-type-scenarios.data' using xtic(1):2 ti col axes x1y2


