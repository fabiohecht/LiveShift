<?
error_reporting(E_ERROR | E_WARNING | E_PARSE);

if ($argv[1])
	$handle = fopen($argv[1],'r');
else
	$handle = fopen('php://stdin','r');

$grain=5000;
$fail_time=10000;

if ($handle) {
    while (!feof($handle)) {
        $line = fgets($handle, 4096);
	//echo $line;

	if (preg_match('/liveshift\.log\.([^:]+).*called (p\d+)$/',$line,$m)) {
		//print_r($m);
		$name[$m[1]] = $m[2];
	}
	elseif (preg_match('/liveshift\.log\.([^:]+).*\|(\d+) DELAY AT (\d+) IS (\d+) (PLAY|STALL|SKIP)/',$line,$m)) {
//		echo ($m[1]+$m[2]-$t0).' '.($m[2])."\n";
		$data[$m[2]/$grain][$m[1]] = $m[4];
		if ($m[5]=='PLAY')
			$lastplay[$m[1]]=$m[2]/$grain;
		elseif ($m[5]!='PLAY' && $lastplay[$m[1]] < $m[2]/$grain - $fail_time/$grain)
			$playfailures[$m[1]][$lastplay[$m[1]]] = $m[2]/$grain;
	}
       
    }
    fclose($handle);
}
ksort($name);
ksort($data);

print_r($name);
print_r($lastplay);
print_r($playfailures);


/*   	MUST ZERO ON CHANNEL SWITCH, ALSO DEFINE $fail_time AND MAKE SOFTWARE CHANGE CHANNEL WHEN THAT HAPPENS
$t0 = min(array_keys($data));

echo "duration\tfailures\n";
foreach ($data as $k=>$v) {
	echo ($k-$t0)*$grain."\t";
	ksort($v);
	foreach ($name as $k2=>$v2)
		echo $playfailures[$k2]."\t";
	echo "\n";
}
*/
?>
