<?
error_reporting(E_ERROR | E_WARNING | E_PARSE);

//sent messages

if ($argv[1])
	$handle = fopen($argv[1],'r');
else
	$handle = fopen('php://stdin','r');

$totals=array();

if ($handle) {
    while (!feof($handle)) {
        $line = fgets($handle, 4096);

	if (preg_match('/^([^:]+):([^\|]+)\|.*\|in putSegmentBlock\(ss:0 b#([0-9]+) #packets:[0-9]+ hc:[0-9]+ si:([^\)]+)/',$line,$m)) {
		$p=$m[1];
		$t=getTime($m[2]);
		$b=$m[3];
		$si=$m[4];
//echo "rec[$p][$si][$b] = $t\n";
		if (!isset($rec[$p][$si][$b]))
			$rec[$p][$si][$b] = $t;
	}
	else if (preg_match('/^([^:]+):([^\|]+)\|.*\|sending reply \(BlockReplyMessage MID:[0-9]+ sender:[^ ]+ brc:GRANTED sb:ss:0 b#([0-9]+) #packets:[0-9]+ hc:[0-9]+ si:([^\)]+)/',$line,$m)) {
		$p=$m[1];
		$t=getTime($m[2]);
		$b=$m[3];
		$si=$m[4];

//echo "snd[$p][$si][$b] = $t\n";
		if (isset($rec[$p][$si][$b]))
			$snd[]= $t-$rec[$p][$si][$b];
	}
    }
    fclose($handle);
}

echo "avg block holding time\n";
echo array_sum($snd)/count($snd);
echo "\n";



function getTime($strTime) {
	$a = preg_split('/\.|:/',$strTime);
	$out = $a[0];
	$out = $out*60 + $a[1];
	$out = $out*60 + $a[2];
	$out = $out*1000 + $a[3];

	return $out;
}
?>

