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
  
    	if (preg_match('/^([^:]+):([^\|]+)\|.*\|dhtOperation ([\w]+)/',$line,$m)) {
    
    		$type = $m[3];
    
    //    $size_modifier=0;
    
    		$sum_type[$type] += 1;
    //		$sum_size[$type] += $m[4]*$size_modifier;
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
//asort($sum_size);

//echo "\"DHT/DT Operation\"\t\"avg # of messages sent per peer per second\"\tsize\n";
echo "\"DHT/DT Operation\"\t\"avg # of messages sent per peer per second\"\n";

$sumall=0;
$count=count($peer);
foreach ($sum_type as $k=>$v) {
	echo $k."\t";
	//echo ($sum[size[$k]/$count/(($t1-$t0)/1000))."\t";
	echo ($v/$count/(($t1-$t0)/1000))."\n";
	$sumall += $v;
}
echo "all\t";
echo ($sumall/$count/(($t1-$t0)/1000))."\n";





function getTime($strTime) {
	$a = preg_split('/\.|:/',$strTime);
	$out = $a[0];
	$out = $out*60 + $a[1];
	$out = $out*60 + $a[2];
	$out = $out*1000 + $a[3];

	return $out;
}
?>
