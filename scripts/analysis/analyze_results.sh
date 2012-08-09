echo 'Request Messages Sent'
cat ~/results/liveshift_*.log | grep 'sendRequest|in sendRequest' | wc -l
echo 'OK Messages received'
cat ~/results/liveshift_*.log | grep 'handleOK|block received OK' | wc -l
echo 'NotAvailable Messages received'
cat ~/results/liveshift_*.log | grep 'handleNotAvailable|received empty reply' | wc -l
echo 'QueueFull Messages received'
cat ~/results/liveshift_*.log | grep 'handleQueueFull|received queue is full message' | wc -l
echo 'InsufficientReputation Messages received'
cat ~/results/liveshift_*.log | grep 'handleInsufficientReputation|received insufficient reputation message' | wc -l
echo 'Null Messages received'
cat ~/results/liveshift_*.log | grep 'handleReceivedMessage|No (null) message received from ' | wc -l
echo 'Disconnect Messages received'
cat ~/results/liveshift_*.log | grep 'handleDisconnect|received disconnect message' | wc -l

echo 'Requests from Streaming Peers'
cat ~/results/liveshift*.log | egrep -e 'selected peer.*168.10\.1' | wc -l
echo 'Requests from Other Peers'
cat ~/results/liveshift*.log | egrep -e 'selected peer.*168.10\.[^1]' | wc -l

echo 'PSH tries'
cat ~/results/liveshift*.log | grep 'going for PSH2' |wc -l
