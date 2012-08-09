<?
error_reporting(E_ERROR | E_WARNING | E_PARSE);

$windowsize=5;
$window=array();
$w=0;

if (isset($argv[2]))
	$handle = fopen($argv[2],'r');
else
	$handle = fopen('php://stdin','r');

/*
h48:15:26:03.745|Thread-6|CommandLineInterface|INFO|randomSwitchScenario|tuning to channel [C2] at [1291904763743] ms (ts:2 ms) and holding for [95] s
h48:15:26:05.023|MessageProcessor-Tuner|SegmentStorage|INFO|putSegmentBlock|in putSegmentBlock(ss:0 b#363 #packets:2 hc:4 si:(C2,2153174:0))
*/

if ($handle) {
    $lastT=0;
    $lastB=-1;
    $lastS='';
    $jitter=0;
    while (!feof($handle)) {
        $line = fgets($handle, 4096);
	//s14 C1,2151287:0 48 12:50:50.315

	if (preg_match('/^([^ ]+) ([^ ]+) ([^ ]+) ([^ ]+)$/',$line,$m)) {
		//print_r($m);
		$curT=getTime($m[4]);
		$curB=$m[3];
		$curS=$m[2];
		if ($lastT!=0) {
			$diff = ($curT-$lastT);
			$jitter=$jitter+($diff-$jitter)/16;
			echo $m[1].' '.$m[2].' '.$m[3].' '.$diff.' [';
			foreach ($window as $v)
				echo $v.':';
			echo '] ';
			echo ($diff);

			//continuous or not
			if ($lastB!=-1 && $curS==$lastS)
				echo ($curB==$lastB+1)?'/ IN ORDER':'/ OUT OF ORDER';	
	
			echo "\n";

			$prediction=array_sum($window)/count($window);  //simple prediction (=current)
			$window[$w++%$windowsize]=$diff;

		}
		$lastT=$curT;
		$lastB=$curB;
		$lastS=$curS;
	}
	else if (strpos($line, 'tuning')!==FALSE) {
	    $lastT=0;
	    $lastB=-1;
	    $lastS='';
	    $jitter=0;
	}
    }
    fclose($handle);

}
else
	die('error opening file: '.$argv[2]."\n");






function getTime($strTime) {
	$a = preg_split('/\.|:/',$strTime);
	$out = $a[0];
	$out = $out*60 + $a[1];
	$out = $out*60 + $a[2];
	$out = $out*1000 + $a[3];

	return $out;
}

function how_close($diff, $prediction) {
	echo " ($diff:$prediction)";
	return  abs($diff - $prediction)/$diff;
}
?>
