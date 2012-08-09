<?
error_reporting(E_ERROR | E_WARNING | E_PARSE);

if ($argv[1])
	$handle = fopen($argv[1],'r');
else
	$handle = fopen('php://stdin','r');

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
	
	if (preg_match('$^([^:]+):.*\|\d+ DELAY AT ([0-9]+) IS ([0-9]+) .* SKIPPED [0-9\.E-]+ ([0-9]+)/([0-9]+)$',$line,$m)) {

		$class = $m[1][0];

		$lag = $m[3]/$grainlag;
		$hold = $m[2]/$grainhold;

		$atleastone = true;

		$holds[$hold][$lag][$class] += 1;

		$classcount[$class]+=1;

		$count++;
		$maxhold[$class]=max($maxhold[$class],$hold);
		$maxlag[$class]=max($maxlag[$class],$lag);
////echo ($hold*$grainhold).';'.($lag*$grainlag)."\n";

//if ($counttt++==100) break;
	}

    }
    fclose($handle);
}

////die();


/*
print_r($maxhold);
print_r($maxlag);

print_r($classcount);
print_r($holds);
*/
ksort($holds,SORT_NUMERIC);

echo "#hold\tlag\tcdf-LU\tcdf-HU\tcdf-all\n";

$ac=array();
foreach ($holds as $hold=>$lags) {
	
	for ($i=0; $i<max($maxlag['h'],$maxlag['s']); $i++) {

		echo ($hold)."\t";
		echo ($i)."\t";
			
		$sum=0;

		foreach (array('h','s') as $class) {

			$classes = $lags[$i];
		
		        // find numSamples for which these two are satisfied
			$samples = $classes[$class];

			$ac[$class]+=$samples;
//echo ($ac[$class]).'/'.$classcount[$class]."=";
			echo ($ac[$class]/$classcount[$class])."\t";

			$sum+=$samples;
		}
//echo $sum.'/'.$count."=";
		$ac['a']+=$sum;
		echo ($ac['a']/$count)."\n";
	}
	echo "\n";
}

?>

