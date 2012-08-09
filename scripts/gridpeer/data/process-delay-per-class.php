<?
error_reporting(E_ERROR | E_WARNING | E_PARSE);

if ($argv[1])
	$handle = fopen($argv[1],'r');
else
	$handle = fopen('php://stdin','r');

$grain=30000;

$delay['super'] = array();
$delay['home'] = array();
$count['super'] = array();
$count['home'] = array();
$skipped['super'] = array();
$skipped['home'] = array();


if ($handle) {
    while (!feof($handle)) {
        $line = fgets($handle, 4096);
	
	if (preg_match('/^([^:]+):.*\|\d+ DELAY AT (\d+) IS (\d+) [A-Z]+ SKIPPED ([0-9\.E-]+)/',$line,$m)) {

$class = $m[1][0]=='s'?'super':($m[1][0]=='h'?'home':'peercaster');

		if ($next<$m[2]) {
			$time[$m[2]/$grain]=1;
			$delay[$class][$m[2]/$grain] += $m[3];
			$count[$class][$m[2]/$grain] += 1;
			$skipped[$class][$m[2]/$grain] += $m[4]*100;
			$next=$m[2]+$grain;
		}
		else
			$next=0;
	}


    }
    fclose($handle);
}

ksort($time);

echo "\"holding time (min:s)\"\t\"average delay LU\"\t\"minimum delay LU\"\t\"skipped blocks LU\"\t\"samples LU\"\t\"average delay HU\"\t\"minimum delay HU\"\t\"skipped blocks HU\"\t\"samples HU\"\n";

foreach ($time as $t=>$garbage) {
	echo ((($t*$grain)/1000)/60)."\t";	

	$class = 'home';
	$samples=$count[$class][$t];
	if ($samples>0) {
		echo ($delay[$class][$t]/$samples/1000)."\t";
		echo "0\t";  //delay min removed (too complicated for several playback policies, and sort of useless)
		echo ($skipped[$class][$t]/$samples)."\t";
		echo $samples."\t";
	}
	else echo "\t\t\t\t";

	$class = 'super';
	$samples=$count[$class][$t];
	if ($samples>0) {
		echo ($delay[$class][$t]/$samples/1000)."\t";
		echo "0\t";  //delay min removed (too complicated for several playback policies, and sort of useless)
		echo ($skipped[$class][$t]/$samples)."\t";
		echo $samples."\n";
	}
	else echo "\t\t\t\n";
}

?>
