<?
if ($argv[1])
	$handle = fopen($argv[1],'r');
else
	$handle = fopen('php://stdin','r');

$percentiles = array(50,80,95);
$readingmatrix=false;

$result = array();
$linenum=0;

if ($handle) {
    while (!feof($handle)) {
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

//echo "\n|".$linenum."|=".count($row)."\n";
	for ($colnum=0;$colnum<count($row); $colnum++) {
//echo $colnum.':';

		foreach ($percentiles as $perc) {
			if (!$result[$perc][$linenum] && $row[$colnum]>$perc/100) {
//echo "$colnum/$linenum/$perc\n";
				$result[$perc][$linenum] = $colnum;
				$final[$colnum][$linenum][$perc]=1;
			}

			if (!$result2[$perc][$colnum] && $row[$colnum]>$perc/100) {
//echo "$colnum/$linenum/$perc\n";
				$result2[$perc][$colnum] = $linenum;
				$final[$colnum][$linenum][$perc]=1;
			}

//echo "\n";
		}
	}

////if ($counttt++==100) break;
	$linenum++;

    }
    fclose($handle);
}

//print_r($final);

ksort($final);

echo "colnum\tp".join("%\tp",$percentiles)."%\n";

foreach ($final as $colnum=>$linenums) {
	krsort($linenums);

	foreach ($linenums as $linenum=>$percs) {
		echo "$colnum\t";
		$out='';
		foreach ($percentiles as $perc)
			if ($percs[$perc])
				$out.= $linenum."\t";
			else
				$out.= "-\t";
		echo $out."\n";
	}
}


?>
