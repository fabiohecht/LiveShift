<?
error_reporting(E_ERROR | E_WARNING | E_PARSE);

if ($argv[1])
	$handle = fopen($argv[1],'r');
else
	$handle = fopen('php://stdin','r');

$sum_all = array();
$sum_fail = array();

if ($handle) {
    while (!feof($handle)) {
        $line = fgets($handle, 4096);
  	
	if (preg_match('/^([chs]).*tuning to channel/',$line,$m)) {
		$type = $m[1]=='h'?'LU':($m[1]=='s'?'HU':($m[1]=='c'?'PC':'other?'));
	        $sum_all[$type] +=1;
		continue;
	}
	if (preg_match('/^([chs]).*Playback failed/',$line,$m)) {
		$type = $m[1]=='h'?'LU':($m[1]=='s'?'HU':($m[1]=='c'?'PC':'other?'));
	        $sum_fail[$type] +=1;
		continue;
	}
    }
    fclose($handle);
}
//asort($sum_size);

//echo "\"DHT/DT Operation\"\t\"avg # of messages sent per peer per second\"\tsize\n";
echo "class\t\"% failed playback\"\n";


foreach ($sum_all as $k=>$v) {
	echo $k."\t";
	echo $sum_fail[$k]*100/$v."\n";
//	echo $v.'/'.$capacity[$k]."\n";
}

?>
