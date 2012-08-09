set terminal postscript eps enhanced color  font "Times-Roman,15"

set output 'heatmap-all.eps'
unset key

set size .5,.5


set view map
set style data pm3d
set xtics border in scale .5,.5 mirror norotate  offset character 0, 0, 0
set ytics border in scale .5,.5 mirror norotate  offset character 0, 0, 0
set cbtics border in scale .25,.25 mirror norotate  offset character -.5, 0, 0

set xrange [ 0 : * ] noreverse nowriteback
set yrange [ 0 : * ] noreverse nowriteback
set cbrange [ 0.00000 : 1.00000 ] noreverse nowriteback

#set title "all s1l" 
set ylabel "holding time (min)"
set xlabel "playback lag (s)"
set cblabel "CDF" offset character -1.5, 0, 0


set grid cbtics xtics ytics front
#set dgrid3d

#set format cb "%0.1f"

set palette defined (0 "black", 0.5 "red", 0.8 "yellow", 1 "white")
#set palette defined (0 "black", 0.5 "blue", 0.8 "violet", 0.9 "yellow", 1 "white")

set ytics ("0" 0, "10" 6.67, "20" 13.33, "30" 20, "40" 26.67, "50" 33.33, "60" 39)
#set xtics ("0" 0, "5" 4, "33421" 39)

plot 'data-all.mat' matrix with image
