<?
error_reporting(E_ERROR | E_WARNING | E_PARSE);

if ($argv[1])
	$handle = fopen($argv[1],'r');
else
	$handle = fopen('php://stdin','r');

$grain=30000;

$titleline='';
$firstcolumnistime=false;

if ($handle) {
    while (!feof($handle)) {
        $line = fgets($handle, 4096);
	
	if (!$line) continue;

	if ($titleline=='' || $line==$titleline) {
		$titleline=$line;
		$runs++;
		continue;
	}
	
	$splt = split("\t",$line);
	for ($i=0;$i<count($splt);$i++)
		$data[$i][$runs] = trim($splt[$i]);

    }
    fclose($handle);
}

//print_r($data);

$splt = split("\t",$titleline);
for ($i=0;$i<count($splt);$i++) {
	$col = trim($splt[$i]);
	$out .= $col."\t\"95ci ".str_replace('"','',$col)."\"\t";
}
echo trim($out)."\n";

$out='';
foreach ($data as $run=>$v2) {

	echo avg($v2)."\t".ci95($v2)."\n";
//die("\tbih\n");
}

// Function to calculate square of value - mean
function sd_square($x, $mean) { return pow($x - $mean,2); }

// Function to calculate standard deviation (uses sd_square)    
// square root of sum of squares devided by N-1
function sd($array) {return sqrt(array_sum(array_map("sd_square", $array, array_fill(0,count($array), (avg($array)) ) ) ) / (count($array)-1) );}

function avg($array) {return array_sum($array)/count($array);}

function ci95($array) {
	$t = array(-1,-1,4.3027,3.1824,2.7765,2.5706,2.4469,2.3646,2.3060,2.2622,2.2281,2.2010,2.1788,2.1604,2.1448,2.1315,2.1199,2.1098,2.1009,2.0930,2.0860,2.0796,2.0739,2.0687,2.0639,2.0595,2.0555,2.0518,2.0484,2.0452,2.0423,2.0395,2.0369,2.0345,2.0322,2.0301,2.0281,2.0262,2.0244,2.0227,2.0211,2.0195,2.0181,2.0167,2.0154,2.0141,2.0129,2.0117,2.0106,2.0096,2.0086,2.0076,2.0066,2.0057,2.0049,2.0040,2.0032,2.0025,2.0017,2.0010,2.0003,1.9996,1.9990,1.9983,1.9977,1.9971,1.9966,1.9960,1.9955,.9949,1.9944,1.9939,1.9935,1.9930,1.9925,1.9921,1.9917,1.9913,1.9908,1.9905,1.9901,1.9897,1.9893,1.9890,1.9886,1.9883,1.9879,1.9876,1.9873,1.9870,1.9867,1.9864,1.9861,1.9858,1.9855,1.9852,1.9850,1.9847,1.9845,1.9842,1.9840);
	$n = count($array);
	$sd = sd($array);
	$mean = avg($array);
	$se = $sd/sqrt($n);
	return $t[$n-1] * $se;
}
?>
