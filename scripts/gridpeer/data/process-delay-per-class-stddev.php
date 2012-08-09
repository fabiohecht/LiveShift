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
	
	if (preg_match('/^([^:]+):.*\|\d+ DELAY AT (\d+) IS (\d+) [A-Z]+ MIN (\d+) SKIPPED ([0-9\.E-]+)/',$line,$m)) {

$class = $m[1][0]=='s'?'super':($m[1][0]=='h'?'home':'peercaster');

		if ($next<$m[2]) {
			$time[$m[2]/$grain]=1;
			$delay[$class][$m[2]/$grain][] = $m[3]/1000;
			$delaymin[$class][$m[2]/$grain][] = $m[4]/1000;
//			$skipped[$class][$m[2]/$grain] += $m[5]*100;
			$next=$m[2]+$grain;
		}
		else
			$next=0;
//if ($counter++>10000) break;

	}

    }
    fclose($handle);
}

ksort($time);

echo "\"holding time (min:s)\"\t\"average delay LU\"\t\"average delay LU stddev\"\t\"minimum delay LU\"\t\"minimum delay LU stddev\"\t\"samples LU\"\t\"average delay HU\"\t\"average delay HU stddev\"\t\"minimum delay HU\"\t\"minimum delay HU stddev\"\t\"samples HU\"\n";

//print_r($delay);

foreach ($time as $t=>$garbage) {
	echo date('H:i:s',($t*$grain)/1000-3600)."\t";	

	$class = 'home';
	if (is_array($delay[$class][$t])) {
		$count=0;
		$sum=0;
		$deviationssquared=0;
		$out='';
		foreach ($delay[$class][$t] as $i=>$value) {
			$deviationssquared += pow($value-$mean,2);
			$sum+=$value;
			$count++;
		}
		if ($count) {
			if ($count-1>0)
				$stddev = sqrt($deviationssquared/($count-1));
			else
				$stddev='';
			$mean = $sum/$count;
//echo "mean=$sum/$count=$mean\n";
			if (!$stddev) $stddev='0';
			$out .= "$mean\t$stddev\t";
		}
		else
			$out .= "\t\t";
		
		$deviationssquared=0;
		foreach ($delaymin[$class][$t] as $i=>$value) {
			$deviationssquared += pow($value-$mean,2);
			$sum+=$value;
			$count++;
		}
		if ($count) {
			if ($count-1>0)
				$stddev = sqrt($deviationssquared/($count-1));
			else
				$stddev='';
			$mean = $sum/$count;
//echo "mean=$sum/$count=$mean\n";
			if (!$stddev) $stddev='0';
			$out .= "$mean\t$stddev\t";
		}
		else
			$out .= "\t\t";

		echo $out.$count."\t";
	}
	else
		echo "\t\t\t\t\t";

	$class = 'super';
	if (is_array($delay[$class][$t])) {
		$count=0;
		$sum=0;
		$deviationssquared=0;
		$out='';
		foreach ($delay[$class][$t] as $i=>$value) {
			$deviationssquared += pow($value-$mean,2);
			$sum+=$value;
			$count++;
		}
		if ($count) {
			if ($count-1>0)
				$stddev = sqrt($deviationssquared/($count-1));
			else
				$stddev='';
			$mean = $sum/$count;
			if (!$stddev) $stddev='0';
			$out .= "$mean\t$stddev\t";
		}
		else
			$out .= "\t\t";
		
		$deviationssquared=0;
		foreach ($delaymin[$class][$t] as $i=>$value) {
			$deviationssquared += pow($value-$mean,2);
			$sum+=$value;
			$count++;
		}
		if ($count) {
			if ($count-1>0)
				$stddev = sqrt($deviationssquared/($count-1));
			else
				$stddev='';
			$mean = $sum/$count;
//echo "mean=$sum/$count=$mean\n";
			if (!$stddev) $stddev='0';
			
			$out .= "$mean\t$stddev\t";
		}
		else
			$out .= "\t\t";

		echo $out.$count."\n";
	}
	else
		echo "\t\t\t\t\n";
}

?>
