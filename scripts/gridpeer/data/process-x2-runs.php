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
	$firstcolumn = $splt[0]."\t".$splt[1];  //INDEX IS FIRST AND SECOND COLUMNS!!

//print_r($splt);

	for ($i=2;$i<count($splt);$i++)
		$data[$firstcolumn][$i][$runs] = trim($splt[$i]);

    }
    fclose($handle);
}

//print_r($data);

$splt = split("\t",$titleline);
$out = $splt[0]."\t".$splt[1]."\t";
for ($i=2;$i<count($splt);$i++) {
	$col = trim($splt[$i]);
	$out .= $col."\t\"stddev ".str_replace('"','',$col)."\"\t";
}
echo trim($out)."\n";

foreach ($data as $t=>$v) {

	$out = "$t\t";

	foreach ($v as $run=>$v2) {

		$out.= avg($v2)."\t".sd($v2)."\t";
	}
	echo trim($out)."\n";
//die("\tbih\n");
}


// Function to calculate square of value - mean
function sd_square($x, $mean) { return pow($x - $mean,2); }

// Function to calculate standard deviation (uses sd_square)    
// square root of sum of squares devided by N-1
function sd($array) {return count($array)<=1?0:sqrt(array_sum(array_map("sd_square", $array, array_fill(0,count($array), (avg($array)) ) ) ) / (count($array)-1) );}

function avg($array) {return count($array)==0?0:array_sum($array)/count($array);}


?>
