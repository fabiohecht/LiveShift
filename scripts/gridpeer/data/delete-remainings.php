<?
error_reporting(E_ERROR | E_WARNING | E_PARSE);

$lengthMillis = $argv[1]*1000;

if (isset($argv[2]))
	$handle = fopen($argv[2],'r');
else
	$handle = fopen('php://stdin','r');

if ($handle) {
    $first =0;
    while (!feof($handle)) {
        $line = fgets($handle, 4096);
	
	if (preg_match('/liveshift\.log\.([^:]+).*called (\w\d+)$/',$line,$m)) {
		//print_r($m);
		$name[$m[1]] = $m[2];
	}
	if (preg_match('/liveshift\.log\.([^:]+):([^\|]+)\|/',$line,$m)) {
		if (!$first)
			$first = getTime($m[2]);
		if (getTime($m[2]) < $first+$lengthMillis)
			echo preg_replace('/^.*liveshift\.log\.([^:]+):/',$name[$m[1]].':',$line);
	}
    }
    fclose($handle);

}
else
	die('error opening file: '.$argv[2]."\n");


function getTime($strTime) {
	$a = preg_split('/\.|:/',$strTime);
	$out = $a[0];
	$out = $out*60 + $a[1];
	$out = $out*60 + $a[2];
	$out = $out*1000 + $a[3];

	return $out;
}
?>
