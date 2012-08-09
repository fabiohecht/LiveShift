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
	
	if (preg_match('/^([^:]+):.*\|\d+ DELAY AT (\d+) IS (\d+) [A-Z]+ SKIPPED ([0-9\.E-]+)/',$line,$m)) {

		if ($next<$m[2]) {
			$delay[$m[2]/$grain] += $m[3];
			$count[$m[2]/$grain] += 1;
			$skipped[$m[2]/$grain] += $m[4];
			$next=$m[2]+$grain;
		}
		else
			$next=0;
	}
    }
    fclose($handle);
}

ksort($delay);

echo "\"holding time (min:s)\"\t\"average delay (s)\"\t\"minimum delay (s)\"\t\"skipped blocks (%)\"\tsamples\n";

foreach ($delay  as $k=>$v) {

	$samples=$count[$k];

	if ($samples>0) {
		echo date('H:i:s',($k*$grain)/1000-3600)."\t";
		echo ($v/$samples/1000)."\t";
		echo "0\t";  //delay min removed (too complicated for several playback policies, and sort of useless)
		echo ($skipped[$k]/$samples)."\t";
		echo $samples."\n";
	}
}

?>
