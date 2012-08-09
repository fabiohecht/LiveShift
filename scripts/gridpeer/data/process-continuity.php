<?
error_reporting(E_ERROR | E_WARNING | E_PARSE);

if ($argv[1])
	$handle = fopen($argv[1],'r');
else
	$handle = fopen('php://stdin','r');

$continuity=0;

if ($handle) {
    while (!feof($handle)) {
        $line = fgets($handle, 4096);
	
	if (preg_match('/CNT ([0-9\.E-]+)/',$line,$m)) {
		$continuity += $m[1];
		$samples++;
	}


    }
    fclose($handle);
}


echo "\"continuity\"\n";
echo ($continuity/$samples)."\n";

?>
