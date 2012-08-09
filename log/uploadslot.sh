grep '|UploadSlot|FINE|run' $1 |grep Got|sed 's/\([^\|]*\)|.*Got.*\(name:[^\,]*\).*\(no:[0-9]*\).*/\1 \2 \3/'
