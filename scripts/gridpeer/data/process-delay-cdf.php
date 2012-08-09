<?
error_reporting(E_ERROR | E_WARNING | E_PARSE);

if ($argv[1])
	$handle = fopen($argv[1],'r');
else
	$handle = fopen('php://stdin','r');

$grain=30000;

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

		$class = $m[1][0]=='s'?'super':($m[1][0]=='h'?'home':'peercaster');

		$lag = $m[3];
		$hold = $m[2];

		$atleastone = true;

//PER REPORT
			$pi=(1-$lag/$hold);
			$holds[$hold][$class[0]] += 1;
			$lags[$lag][$class[0]] += 1;
			$pis[$pi*1000][$class[0]] += 1;
			$classcount[$class[0]]+=1;
			$maxhold[$class[0]]=max($maxhold[$class[0]],$hold);
			$maxlag[$class[0]]=max($maxlag[$class[0]],$lag);
//			echo "$class[0]\t$channel\t$hold\t$lag\t$pi\n";

//if ($counttt++==100) break;
	}
	if (preg_match('$tuning to channel \[(C[0-9]+)\]$',$line,$m)) {
		//switched
/* ENABLE PER SESSION
		if ($atleastone) {
			$pi=(1-$lag/$hold);
			$holds[$hold][$class[0]] += 1;
			$lags[$lag][$class[0]] += 1;
			$pis[$pi*1000][$class[0]] += 1;
			$classcount[$class[0]]+=1;
			$maxhold[$class[0]]=max($maxhold[$class[0]],$hold);
			$maxlag[$class[0]]=max($maxlag[$class[0]],$lag);
//			echo "$class[0]\t$channel\t$hold\t$lag\t$pi\n";
		}
*/
		$channel = $m[1];
		$atleastone = false;
	}

    }
    fclose($handle);
}

//last session
/* ENABLE PER SESSION
		if ($atleastone) {
			$pi=(1-$lag/$hold);
			$holds[$hold][$class[0]] += 1;
			$lags[$lag][$class[0]] += 1;
			$pis[$pi*1000][$class[0]] += 1;
			$classcount[$class[0]]+=1;
			$maxhold[$class[0]]=max($maxhold[$class[0]],$hold);
			$maxlag[$class[0]]=max($maxlag[$class[0]],$lag);
//			echo "$class[0]\t$channel\t$hold\t$lag\t$pi\n";
		}
*/








//BELOW IS FOR ALL RELATIVE 0..1

//normalizes holds and lags
foreach ($holds as $hold=>$a)
	foreach ($a as $class=>$count)
		$holdsn[$hold*1000/$maxhold[$class]][$class] += $count;

foreach ($lags as $lag=>$a)
	foreach ($a as $class=>$count)
		$lagsn[$lag*1000/$maxlag[$class]][$class] += $count;

//todo: per channel

echo "cdf\tpi-LU\tpi-HU\thold-LU\thold-HU\tlag-LU\tlag-HU\n";

$ac=array();
for ($i=0; $i<=1000; $i++) {
	echo ($i/1000)."\t";

	foreach (array('h','s') as $class) {
		$count = $pis[$i][$class];
		$acpis[$class]+=$count;
		echo ($acpis[$class]/$classcount[$class])."\t";
		//echo ($count/$classcount[$class])."\t";
	}
	foreach (array('h','s') as $class) {
		$count = $holdsn[$i][$class];
		$acholds[$class]+=$count;
		echo ($acholds[$class]/$classcount[$class])."\t";
		//echo ($count/$classcount[$class])."\t";
	}
	foreach (array('h','s') as $class) {
		$count = $lagsn[$i][$class];
		$aclags[$class]+=$count;
		echo ($aclags[$class]/$classcount[$class])."\t";
		//echo ($count/$classcount[$class])."\t";
	}

	echo "\n";
}

?>

