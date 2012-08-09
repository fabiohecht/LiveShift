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

   		$size_modifier=0;
		switch ($type) {
			case 'SducSubscribe':
				$type='Subscribe';
				break;
			case 'BlockReply':
				preg_match('/brc:([^ ]+)/',$line,$m2);
				if ($m2[1]=='GRANTED')
					$size_modifier=59300;  //to simulate bitrate larger (I want 500)
		}
		//done separately to easily be removed if wanted
		switch ($type) {
			case 'Subscribed':
			case 'NotSubscribed':
				$type='(Not)Subscribed';
				break;
			case 'Granted':
			case 'Queued':
				$type='Granted/Queued';
				break;
			case 'Interested':
			case 'NotInterested':
				$type='(Not)Interested';
				break;
		}
		
		$sum_type[$type] += 1;
		$sum_size[$type] += ($m[4]+$size_modifier)/1024;
		$peer[$m[1]]=1;

		if (!$t0) $t0=getTime($m[2]);
		else {
			$t0=min($t0,getTime($m[2]));
			$t1=max($t1,getTime($m[2]));
		}
	}
    }
    fclose($handle);
}
asort($sum_type);

echo "\"Message Type\"\t\"# of messages\"\tsize\n";

$count=count($peer);
foreach ($sum_type as $k=>$v) {
	echo $k."\t";
	echo ($v/$count/(($t1-$t0)/1000))."\t";
	echo ($sum_size[$k]/$count/(($t1-$t0)/1000))."\n";
}





function getTime($strTime) {
	$a = preg_split('/\.|:/',$strTime);
	$out = $a[0];
	$out = $out*60 + $a[1];
	$out = $out*60 + $a[2];
	$out = $out*1000 + $a[3];

	return $out;
}
?>
