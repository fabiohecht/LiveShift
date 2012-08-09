<?
error_reporting(E_ERROR | E_WARNING | E_PARSE);

if ($argv[1])
	$handle = fopen($argv[1],'r');
else
	$handle = fopen('php://stdin','r');

$percentiles = array(50,80,95);

$grainhold=30000;
$grainlag=100;

$tplay['super'] = 0;
$tplay['home'] = 0;
$tstall['super'] = 0;
$tstall['home'] = 0;


//echo "\"peer class\"\t\"channel\"\t\"holding time (ms)\"\t\"lag (ms)\"\t\"1-lag/holding (play index)\"\n";

if ($handle) {
    while (!feof($handle)) {
        $line = fgets($handle, 4096);

//echo $line;
	
	if (preg_match('$^([^:]+):.*\|\d+ DELAY AT ([0-9]+) IS ([0-9]+)$',$line,$m)) {

		$class = $m[1][0];

		$lag = round($m[3]/$grainlag);
		$hold = $m[2]/$grainhold;
		$holdclass[$class[0]][$hold][] = $lag;
		$holdclass['zall'][$hold][] = $lag;

////echo ($hold*$grainhold).';'.($lag*$grainlag)."\n";

//if ($counttt++==100) break;
	}

    }
    fclose($handle);
}

/*
print_r($holdclass);

print_r($maxhold);
print_r($maxlag);

print_r($classcount);
print_r($holds);
*/
ksort($holdclass,SORT_NUMERIC);

foreach ($holdclass as $class=>$holds) {
	
	ksort($holds,SORT_NUMERIC);

	foreach ($holds as $hold=>$lags) {
		
		sort($lags, SORT_NUMERIC);

		foreach ($percentiles as $perc) {
			$results[$perc][$hold][$class] = $lags[round($perc/100*count($lags))]*$grainlag/1000;
		}

	}
}


echo "percentile\tht\tlag-LU\tlag-HU\tlag-all\n";

foreach ($results as $perc=>$holds) {
	foreach ($holds as $hold=>$classes) {
		echo "$perc\t".(($hold*$grainhold)/60000)."\t";
		ksort($classes);
		echo join("\t", $classes);
		echo "\n";
	}
	echo "\n\n";
}
?>
