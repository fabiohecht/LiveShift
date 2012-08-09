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
	//echo $line;

	if (preg_match('/^([^:]+).*\|achieved upload rate at (\d+) is ([\d\.]+)\/([\d\.]+)/',$line,$m)) {
//		echo ($m[1]+$m[2]-$t0).' '.($m[2])."\n";
		$name[$m[1]]=1;
		$data[$m[2]/$grain][$m[1]] += 100*$m[3]/$m[4];
		$samples[$m[2]/$grain][$m[1]] += 1;
	}
       
    }
    fclose($handle);
}
ksort($name);
ksort($data);

$t0 = min(array_keys($data));

echo "time\taverage\t";
echo join("\t",array_keys($name));

echo "\n";
foreach ($data as $k=>$v) {
	echo date('H:i:s',(($k-$t0)*$grain)/1000-3600)."\t";

	ksort($v);
	$sum=0;
	$count=0;
	$out='';
	foreach ($name as $k2=>$v2) {
		$s = $samples[$k][$k2];
		if ($s==0)
			$movingaverage = 0;
		else
			$movingaverage = ($v[$k2]?$v[$k2]:'0')/$s;

		$out .= $movingaverage."\t";
		$sum += $movingaverage;
		$count ++;
	}
	echo $sum/$count."\t";
	echo trim($out)."\n";
}
?>
