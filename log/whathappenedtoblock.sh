

egrep 'handleIncomingMessage.*HaveMessage.*block#'$1'|scheduling block '$1'|sendRequest.*block#'$1'|GRANTED sb:block#'$1'|getMergedVideo.*b#'$1 $2   |sed 's/\([^\|]*\|\).*HaveMessage.*/\1;Have/' |sed 's/\([^\|]*\|\).*addBlocksToQueue.*/\1;Scheduled/'|sed 's/\([^\|]*\|\).*sendRequest.*/\1;Requesting/'|sed 's/\([^\|]*\|\).*BlockReplyMessage.*/\1;Receiving/'|sed 's/\([^\|]*\|\).*getMergedVideo.*/\1;Trying to play/'| sed 's/.*/\0;#'$1'/'

