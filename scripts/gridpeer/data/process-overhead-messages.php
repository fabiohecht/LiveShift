<?
error_reporting(E_ERROR | E_WARNING | E_PARSE);

//sent messages

if ($argv[1])
	$handle = fopen($argv[1],'r');
else
	$handle = fopen('php://stdin','r');

$grain=30000;

$timemodifier=0;
$firsttime=0;
if ($handle) {
    while (!feof($handle)) {
        $line = fgets($handle, 4096);

	if (preg_match('/^([^:]+):([^\|]+)\|.*\|sentMessagesAverage:([\d\.]+)/',$line,$m)) {
		if (!isset($peer[$m[1]])) {
			if (!$firsttime)
				$firsttime=round(getTime($m[2])/$grain);
			else
				$firsttime=min($firsttime,round(getTime($m[2])/$grain));
			$timemodifier=0;
//echo "ft=$firsttime\n";
		}
		if ($firsttime>round(getTime($m[2])/$grain))
			$timemodifier=24*3600;  //turn of the day, clock goes back to 0

		$time=round(getTime($m[2])/$grain)+$timemodifier;
		$data[$time][$m[1]] = $m[3];
		$peer[$m[1]]=1;

//echo $line."\tdata[$time][".$m[1]."]\n";

	}
    }
    fclose($handle);
}

echo "\"simulation time (min:s)\"\t\"average\"\t";
echo join("\t",array_keys($peer))."\n";

ksort($data);

//print_r($data);

foreach ($data as $k=>$v) {
	echo date('H:i:s',(($k-$firsttime)*$grain)/1000-3600)."\t";
	$sum=0;
	$out='';
	foreach ($v as $k1=>$v2) {
		$sum += $v2;
		$out.="$v2\t";
	}
	
	echo ($sum/count($v))."\t".trim($out)."\n";
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
