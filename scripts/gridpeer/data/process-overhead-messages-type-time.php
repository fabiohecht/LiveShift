<?
error_reporting(E_ERROR | E_WARNING | E_PARSE);

//sent messages

if ($argv[1])
	$handle = fopen($argv[1],'r');
else
	$handle = fopen('php://stdin','r');

$grain=10000;
$num=0;

if ($handle) {
    while (!feof($handle)) {
        $line = fgets($handle, 4096);

	if (preg_match('/([^:]+)\|.*\|sending message \(([\w]+)Message/',$line,$m)) {
		$sum_type[round(getTime($m[1])/$grain)][$m[2]] += 1;
if ($counter++>1000) break;
	}
    }
    fclose($handle);
}

ksort($sum_type);

echo "\"Message Type\"\tTime\t\"# of messages\"\n";

foreach ($sum_type as $k=>$v) {
	//echo $k."\t";

	$acc=0;
	foreach ($v as $k2=>$v2)
		echo $k*$grain."\t$k2\t".($acc+=$v2)."\n";
	//echo $v."\n";
}

function getTime($strTime) {
	$a = preg_split('/\.|:/',$strTime);
	$out = $a[0];
	$out = $out*60 + $a[1];
	$out = $out*60 + $a[2];
	$out = $out*1000 + $a[3];

	return $out;
}

?>
