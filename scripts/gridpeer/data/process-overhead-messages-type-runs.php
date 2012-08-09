<?
error_reporting(E_ERROR | E_WARNING | E_PARSE);

//input (stdin or filename in $1) must be a joined upstream-utilization.data file

if ($argv[1])
	$handle = fopen($argv[1],'r');
else
	$handle = fopen('php://stdin','r');

$runs=0;
$firstline='';
if ($handle) {
    while (!feof($handle)) {
        $line = fgets($handle, 4096);
	//echo $line;

	if (false!==strpos($line,'messages')) { //discards the first line (header) of files, printing only the first one
		if (!$firstline) {
			$firstline=$line;
		}
		$runs++;
		continue;
	}

	$splt = split("\t",$line);
	$data[$splt[0]][$runs] = trim($splt[1]);
	
    }
    fclose($handle);
}

echo substr($firstline, 0, strlen($firstline)-1)."\tstddev\n";

foreach ($data as $k=>$v) {
	if (!$k) continue;
	echo "$k\t";
	$mean = array_sum($v)/count($v);
	echo "$mean\t";

	$stddev = stddev($v);
	echo "$stddev\n";

}

function stddev($array) {
	$count=count($array);
	$stddev = 0;
	if ($count>1) {
		$deviationssquared=0;
		foreach ($array as $k2=>$v2)
			$deviationssquared += pow($v2-$mean,2);
		$stddev = sqrt($deviationssquared/($count-1));
	}
	return $stddev;
}
?>
