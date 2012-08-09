set terminal postscript eps enhanced color solid font "Times-Roman,15"
set output 'ppfxc0-%SCENARIO-failed-playback.eps'

set title "Scenario %SCENARIO"
set size .5, .5

set xlabel "playback policy"
set ylabel "failed playback (%)"

unset key

set style fill transparent solid 0.5 noborder

set xtics in scale 0,0 nomirror rotate by -40

#set ytics .1
#set mytics 2

set boxwidth 0.4

set xrange [-.5:12.5]

plot '%PREFIX-%SCENARIO-failed-playback.data' u ($0):2:3:4:(0.4):xtic(1)  w boxerror lc rgb "#444444"

