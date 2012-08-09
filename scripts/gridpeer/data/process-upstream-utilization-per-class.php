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

	if (preg_match('/^([chs])[0-9]+:([^:]+).*\|achieved upload rate at (\d+) is ([\d\.]+)\/([\d\.]+)/',$line,$m)) {
//print_r($m);
		$type = $m[1]=='h'?'LU':($m[1]=='s'?'HU':($m[1]=='c'?'PC':'other?'));
//		$data[$type] += $m[4];
//		$capacity[$type] += $m[5];
//		$samples[$type] += 1;
		
//echo "+".$m[4]/$m[5];

		$relative[$type][] = $m[4]/$m[5];
		$relative['all'][] = $m[4]/$m[5];
	}
    }
    fclose($handle);
}


//print_r($relative);


ksort($relative);

echo "class\taverage relative upstream utilization\tstddev relative upstream utilization\n";

foreach ($relative as $k=>$v) {
	echo $k."\t";

	echo avg($v)."\t".sd($v)."\n";

//	echo $v.'/'.$capacity[$k]."\n";
}


/*
foreach ($data as $k=>$v) {
	echo $k."\t";
	$avg = $v/$capacity[$k];
	echo ($avg?$avg:0)."\n";

//	echo $v.'/'.$capacity[$k]."\n";
}

echo "all\t".(array_sum($data)/array_sum($capacity))."\n";

*/


// Function to calculate square of value - mean
function sd_square($x, $mean) { return pow($x - $mean,2); }

// Function to calculate standard deviation (uses sd_square)    
// square root of sum of squares devided by N-1
function sd($array) {return count($array)<=1?0:sqrt(array_sum(array_map("sd_square", $array, array_fill(0,count($array), (avg($array)) ) ) ) / (count($array)-1) );}

function avg($array) {return count($array)==0?0:array_sum($array)/count($array);}


?>
