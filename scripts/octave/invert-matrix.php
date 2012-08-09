<?
if ($argv[1])
	$handle = fopen($argv[1],'r');
else
	$handle = fopen('php://stdin','r');

$percentiles = array(50,80,95);
$readingmatrix=false;

$ROW_MAX=60;
$COL_MAX=60;

$result = array();
$linenum=0;

if ($handle) {
    while (!feof($handle) && $linenum<$ROW_MAX) {
        $line = fgets($handle, 4096);

//echo $line;
	

	if ($line[0]=='#')
		if (!$readingmatrix)
			continue;
		else
			break;
	else if (!$readingmatrix)
		$readingmatrix=true;

	$row = split(' ',$line);

	for ($colnum=0;$colnum<count($row) && $colnum<$COL_MAX; $colnum++) {

//echo trim($row[$colnum]).":";


		if (trim($row[$colnum]))
			$final[$colnum][$linenum]=trim($row[$colnum]);

//if (trim($row[$colnum])>1)
//	echo "\n($linenum,$colnum) ".$row[$colnum]." in ".print_r($row,1)."\n";

	}
	$linenum++;

    }
    fclose($handle);
}

//print_r($final);

ksort($final);

foreach ($final as $colnum=>$linenums) {
	$out='';
	foreach ($linenums as $linenum=>$v) {
		$out.=$v.' ';
	}
	echo trim($out)."\n";
}


?>
