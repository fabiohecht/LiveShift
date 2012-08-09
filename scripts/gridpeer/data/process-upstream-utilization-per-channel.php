<?
error_reporting(E_ERROR | E_WARNING | E_PARSE);

if ($argv[1])
	$handle = fopen($argv[1],'r');
else
	$handle = fopen('php://stdin','r');

$grain=5000;

if ($handle) {
    while (!feof($handle)) {
        $line = fgets($handle, 4096);
//	echo $line;

	if (preg_match('/^([chs])[0-9]+:([^:]+).*\|achieved upload rate for channel \((C[0-9])\) at (\d+) is ([\d\.]+)\/([\d\.]+)/',$line,$m)) {
//print_r($m);
		//$type = $m[1]=='h'?'LU':($m[1]=='s'?'HU':($m[1]=='c'?'PC':'other?'));
		$type = $m[3];
		$data[$type] += $m[5];
	}
	if (preg_match('/^([chs])[0-9]+:([^:]+).*\|achieved upload rate at (\d+) is ([\d\.]+)\/([\d\.]+)/',$line,$m)) {
//print_r($m);
    		//$type = $m[1]=='h'?'LU':($m[1]=='s'?'HU':($m[1]=='c'?'PC':'other?'));
		$type = 'all';
		$data[$type] += $m[4];
		$capacity += $m[5];
	}
    }
    fclose($handle);
}
ksort($data);

echo "channel\taverage relative upstream utilization\n";

foreach ($data as $k=>$v) {
	echo $k."\t";
	$avg = $v/$capacity;
	echo ($avg?$avg:0)."\n";
}
?>
