<?
error_reporting(E_ERROR | E_WARNING | E_PARSE);

if ($argv[1])
	$handle = fopen($argv[1],'r');
else
	$handle = fopen('php://stdin','r');

$sum_all = 0;
$sum_dupes = 0;

if ($handle) {
    while (!feof($handle)) {
        $line = fgets($handle, 4096);
  
      if (strpos($line,'tuning to channel')!==FALSE)
            $sum_all ++;
      if (strpos($line,'Playback failed')!==FALSE)
            $sum_fail ++;
      
    }
    fclose($handle);
}
//asort($sum_size);

//echo "\"DHT/DT Operation\"\t\"avg # of messages sent per peer per second\"\tsize\n";
echo "\"% failed playback\"\n";
echo $sum_fail*100/$sum_all;
echo "\n"

?>
