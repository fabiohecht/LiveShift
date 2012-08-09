echo 'PEER			 :R_GOT	:ANS_OK	:ANS_NO	:R_SENT	:OK	:OKSP	:OKOP	:N/A	:QUFULL	:INREP	:NULL	:RFSP	:RFOP	:PSH	:PSH_SU	:PSH_CD	:PSH_FA	'

for file in ~/results/*; do
	filenice=$(echo $file | sed 's/.*_//g' | sed 's/^192\.168\.\([0-9]\)\.\([0-9][0-9]\).log/192\.168\.\1\.\2\.log  /g' | sed 's/^192\.168\.\([0-9]\)\.\([0-9][0-9][0-9]\).log/192\.168\.\1\.\2\.log /g' | sed 's/^192\.168\.\([0-9][0-9]\)\.\([0-9][0-9]\).log/192\.168\.\1\.\2\.log /g')
	rgot=$(cat $file | egrep -a -e 'handleIncomingMessage\|in handleIncomingMessage.*REQUEST' | wc -l)
	ranok=$(cat $file | egrep -a -e 'sendResponse\|sent reply' | grep 'type:OK sb:' |  wc -l)
	ranno=$(cat $file | egrep -a -e 'sendResponse\|sent reply' | grep 'NO BLOCK REPLIED' |  wc -l)
	rsent=$(cat $file | grep -a 'sendRequest|in sendRequest' | wc -l)
	ok=$(cat $file | grep -a 'handleOK|block received OK' | wc -l)
        oksp=$(cat $file | grep -a 'handleOK|block received OK' | egrep -a -e '.*192\.168\.10\.(11|12)' | wc -l)
        okop=$(cat $file | grep -a 'handleOK|block received OK' | egrep -a -v -e '.*192\.168\.10\.(11|12)' | wc -l)
	na=$(cat $file | grep -a 'handleNotAvailable|received empty reply' | wc -l)
	qf=$(cat $file | grep -a 'handleQueueFull|received queue is full message' | wc -l)
	ir=$(cat $file | grep -a 'handleInsufficientReputation|received insufficient reputation message' | wc -l)
	null=$(cat $file | grep -a 'handleReceivedMessage|No (null) message received from ' | wc -l)
	
	rfsp=$(cat $file | egrep -a -e 'selected peer.*168\.10\.(11|12)' | wc -l)
	rfop=$(cat $file |  grep 'selectPeer| selected peer' | egrep -a -v -e '.*168\.10\.(11|12)' | wc -l)
	psh=$(cat $file | grep -a 'doPSH|going for PSH2' | wc -l)
	pshs=$(cat $file | grep -a 'PSH success' |wc -l)
	pshc=$(cat $file | grep -a 'PSH failure: Check was denied' |wc -l)
	pshf=$(cat $file | grep -a 'PSH failure while requesting check' |wc -l)
	
	echo "$filenice	 :$rgot	:$ranok	:$ranno	:$rsent	:$ok	:$oksp	:$okop	$na	:$qf	:$ir	:$null	:$rfsp	:$rfop	:$psh	:$pshs	:$pshc	:$pshf"
done
