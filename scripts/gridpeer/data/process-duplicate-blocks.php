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
  
      if (strpos($line,'in putSegmentBlock')!==FALSE)
            $sum_all ++;
      if (strpos($line,'duplicate block received')!==FALSE)
            $sum_dupes ++;
      
    }
    fclose($handle);
}
//asort($sum_size);

//echo "\"DHT/DT Operation\"\t\"avg # of messages sent per peer per second\"\tsize\n";
echo "\"% duplicated blocks received\"\n";
echo $sum_dupes*100/$sum_all;
echo "\n"

?>
