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

	if (preg_match('/^([^:]+):.*\|(\d+) DELAY AT (\d+) IS (\d+) (PLAY|STALL|SKIP)/',$line,$m)) {
//		echo ($m[1]+$m[2]-$t0).' '.($m[2])."\n";

		$name[$m[1]]=1;		
		$data[$m[2]/$grain][$m[1]] = $m[4];
		

		if ($m[5]=='PLAY')
			$lastplay[$m[1]] = $m[2]/$grain;
		elseif ($m[5]!='PLAY' && $lastplay[$m[1]] < $m[2]/$grain - $fail_time)
			$playfailures[$m[1]][$lastplay[$m[1]]] = 1;
	}
       
    }
    fclose($handle);
}

ksort($name);
ksort($data);

echo "time"."\t";
foreach (array_keys($name) as $v)
	echo $v."\t";
echo "\n";
foreach ($data as $k=>$v) {
	if (!$t0) $t0 = $k;

	foreach ($name as $k2=>$v2) {
		echo date('H:i:s',(($k-$t0)*$grain)/1000-3600)."\t";
		echo $v[$k2]."\n";
	}
}
?>
