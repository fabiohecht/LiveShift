<?
error_reporting(E_ERROR | E_WARNING | E_PARSE);

if ($argv[1])
	$handle = fopen($argv[1],'r');
else
	$handle = fopen('php://stdin','r');

$liveness=0;

if ($handle) {
    while (!feof($handle)) {
        $line = fgets($handle, 4096);
	
	if (preg_match('/LAG ([0-9\.E-]+)/',$line,$m)) {
		$liveness += $m[1];
		$samples++;
	}


    }
    fclose($handle);
}


echo "\"liveness\"\n";
echo (1-$liveness/$samples)."\n";

?>
