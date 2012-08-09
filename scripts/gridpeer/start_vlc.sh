vlc F1.avi --sout "#std{access=udp,mux=ts,dst=127.0.0.1:${1}}" -I dummy &
