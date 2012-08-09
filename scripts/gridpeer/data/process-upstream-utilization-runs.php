<?
error_reporting(E_ERROR | E_WARNING | E_PARSE);

//input (stdin or filename in $1) must be a joined upstream-utilization.data file

if ($argv[1])
	$handle = fopen($argv[1],'r');
else
	$handle = fopen('php://stdin','r');


$runs=0;
$peers=array();
if ($handle) {
    while (!feof($handle)) {
        $line = trim(fgets($handle, 4096));
	//echo $line;

	if (strpos($line,"time\taverage")===0) {
		$header = split("\t",$line);
		
		$peers = array_unique(array_merge($peers, array_slice(split("\t",$line),2)));
		$runs++;

		continue;
	}

	$splt = split("\t",$line);

	for ($i=2; $i<count($splt); $i++)
		$data[$splt[0]][trim($header[$i])][$runs] = trim($splt[$i]);
	
    }
    fclose($handle);
}

echo "time\taverage\t".join("\t",$peers);

foreach ($data as $k=>$v) {
	echo $k."\t";

	$totalavg=0;
	$out='';
	foreach ($peers as $peer) {
		foreach ($v[$peer] as $v3) {
			$out .= ($v3/$runs)."\t";
			$totalavg += $v3/$runs;
		}
	}
	echo $totalavg/(count($header)-2)."\t";
	echo ($out?$out:0)."\n";
}
?>
