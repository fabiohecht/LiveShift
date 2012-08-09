<?
error_reporting(E_ERROR | E_WARNING | E_PARSE);

if ($argv[1])
	$handle = fopen($argv[1],'r');
else
	$handle = fopen('php://stdin','r');

$skipped=0;

if ($handle) {
    while (!feof($handle)) {
        $line = fgets($handle, 4096);
	
	if (preg_match('/SKIPPED ([0-9\.E-]+)/',$line,$m)) {
		$skipped += $m[1]*100;
		$samples++;
	}


    }
    fclose($handle);
}


echo "\"skipped blocks %\"\n";
echo ($skipped/$samples)."\n";

?>
