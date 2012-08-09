<?
error_reporting(E_ERROR | E_WARNING | E_PARSE);

//sent messages

if ($argv[1])
	$handle = fopen($argv[1],'r');
else
	$handle = fopen('php://stdin','r');

$num=0;

$t0=0;
$t1=0;

if ($handle) {
    while (!feof($handle)) {
        $line = fgets($handle, 4096);

	if (preg_match('/^([^:]+):([^\|]+)\|.*processQueue\|(?:sending (?:message|reply)|trying to send message) ([\w]+)Message.*size=([0-9]+)/',$line,$m)) {

		$type = $m[3];
$isblock=false;
   		$size_modifier=0;
		switch ($type) {
			case 'UsReply':
				preg_match('/rc:([^ ]+)/',$line,$m2);
				switch ($m2[1]) {
					case 'WAIT':
					$type='Subscribed';
					break;
					case 'GRANTED':
					$type='Granted';
					break;
					case 'BACK_IN_Q':
					$type='BackInQ';
					break;
					case 'REJECTED':
					$type='NotSubscribed';
					break;
				}
				break;
			case 'UsUpdate':
				preg_match('/(NOT_)?INTERESTED/',$line,$m2);
				$type=str_replace(' ','',ucwords(strtolower(str_replace('_',' ',$m2[0]))));
				break;
			case 'SducUsRequest':
			case 'UsRequest':
				$type='SubscribeRequest';
				break;
			case 'BlockReply':
				preg_match('/brc:([^ ]+)/',$line,$m2);
				if ($m2[1]=='GRANTED') {
$isblock=true;
					$size_modifier=59300;  //to simulate bitrate larger (I want 500 kbit/s, I had 25.6 kbit/s)
				}
		}
		//done separately to easily be removed if wanted
		switch ($type) {
			case 'Subscribed':
			case 'NotSubscribed':
				$type='(Not)Subscribed';
				break;
			case 'Granted':
			case 'BackInQ':
				$type='Granted/BackInQ';
				break;
			case 'Interested':
			case 'NotInterested':
				$type='(Not)Interested';
				break;
		}
		
		$size = ($m[4]+$size_modifier+64)/1024;

$total_transmitted+=$size;

if ($isblock) {
//echo '|'.$total_transmitted;
//echo '#'.
	$total_blocks+=62500/1024;

//if (++$counterccc==100) break;
}
	}
    }
    fclose($handle);
}

echo "overhead is ($total_transmitted-$total_blocks)/$total_blocks = ".(($total_transmitted-$total_blocks)/$total_blocks)."\n";

