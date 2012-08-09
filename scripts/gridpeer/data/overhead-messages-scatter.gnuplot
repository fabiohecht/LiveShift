#set logscale x
set xlabel "simulation time (s)"
set ylabel "sent messages per peer per second"

set ytics border nomirror norotate  offset character 0, 0, 0 autofreq

set key left top
set key autotitle columnheader
#set format x "10^{%L}"

set size 2

set xdata time
set timefmt "%H:%M:%S"

#set xrange ["00:00:00":"00:10:00"]

set terminal postscript eps enhanced color solid
set output 'overhead-messages.eps'

#set term x11
#set output

#set size 1,1

plot 'overhead-messages.data' using 1:3,\
'overhead-messages.data' using 1:4,\
'overhead-messages.data' using 1:5,\
'overhead-messages.data' using 1:6,\
'overhead-messages.data' using 1:7,\
'overhead-messages.data' using 1:8,\
'overhead-messages.data' using 1:9,\
'overhead-messages.data' using 1:10,\
'overhead-messages.data' using 1:11,\
'overhead-messages.data' using 1:12,\
'overhead-messages.data' using 1:13,\
'overhead-messages.data' using 1:14,\
'overhead-messages.data' using 1:15,\
'overhead-messages.data' using 1:16,\
'overhead-messages.data' using 1:17,\
'overhead-messages.data' using 1:18,\
'overhead-messages.data' using 1:19,\
'overhead-messages.data' using 1:20,\
'overhead-messages.data' using 1:21,\
'overhead-messages.data' using 1:22,\
'overhead-messages.data' using 1:23,\
'overhead-messages.data' using 1:24,\
'overhead-messages.data' using 1:25,\
'overhead-messages.data' using 1:26,\
'overhead-messages.data' using 1:27,\
'overhead-messages.data' using 1:28,\
'overhead-messages.data' using 1:29,\
'overhead-messages.data' using 1:30,\
'overhead-messages.data' using 1:31,\
'overhead-messages.data' using 1:32,\
'overhead-messages.data' using 1:33,\
'overhead-messages.data' using 1:34,\
'overhead-messages.data' using 1:35,\
'overhead-messages.data' using 1:36,\
'overhead-messages.data' using 1:37,\
'overhead-messages.data' using 1:38,\
'overhead-messages.data' using 1:39,\
'overhead-messages.data' using 1:40,\
'overhead-messages.data' using 1:41,\
'overhead-messages.data' using 1:42,\
'overhead-messages.data' using 1:43,\
'overhead-messages.data' using 1:44,\
'overhead-messages.data' using 1:45,\
'overhead-messages.data' using 1:46,\
'overhead-messages.data' using 1:47,\
'overhead-messages.data' using 1:48,\
'overhead-messages.data' using 1:49,\
'overhead-messages.data' using 1:50,\
'overhead-messages.data' using 1:51,\
'overhead-messages.data' using 1:52,\
'overhead-messages.data' using 1:53,\
'overhead-messages.data' using 1:54,\
'overhead-messages.data' using 1:55,\
'overhead-messages.data' using 1:56,\
'overhead-messages.data' using 1:57,\
'overhead-messages.data' using 1:58,\
'overhead-messages.data' using 1:59,\
'overhead-messages.data' using 1:60,\
'overhead-messages.data' using 1:61,\
'overhead-messages.data' using 1:62,\
'overhead-messages.data' using 1:63,\
'overhead-messages.data' using 1:64,\
'overhead-messages.data' using 1:65,\
'overhead-messages.data' using 1:66,\
'overhead-messages.data' using 1:67,\
'overhead-messages.data' using 1:68,\
'overhead-messages.data' using 1:69,\
'overhead-messages.data' using 1:70,\
'overhead-messages.data' using 1:71,\
'overhead-messages.data' using 1:72,\
'overhead-messages.data' using 1:73,\
'overhead-messages.data' using 1:74,\
'overhead-messages.data' using 1:75,\
'overhead-messages.data' using 1:76,\
'overhead-messages.data' using 1:77,\
'overhead-messages.data' using 1:78,\
'overhead-messages.data' using 1:79,\
'overhead-messages.data' using 1:80,\
'overhead-messages.data' using 1:81,\
'overhead-messages.data' using 1:82,\
'overhead-messages.data' using 1:83,\
'overhead-messages.data' using 1:84,\
'overhead-messages.data' using 1:85,\
'overhead-messages.data' using 1:86,\
'overhead-messages.data' using 1:87,\
'overhead-messages.data' using 1:88,\
'overhead-messages.data' using 1:89,\
'overhead-messages.data' using 1:80,\
'overhead-messages.data' using 1:91,\
'overhead-messages.data' using 1:92,\
'overhead-messages.data' using 1:93,\
'overhead-messages.data' using 1:94,\
'overhead-messages.data' using 1:95,\
'overhead-messages.data' using 1:96,\
'overhead-messages.data' using 1:97,\
'overhead-messages.data' using 1:98,\
'overhead-messages.data' using 1:99,\
'overhead-messages.data' using 1:90
