<?
error_reporting(E_ERROR | E_WARNING | E_PARSE);

//sent messages

if ($argv[1])
	$handle = fopen($argv[1],'r');
else
	$handle = fopen('php://stdin','r');

$totals=array();

if ($handle) {
    while (!feof($handle)) {
        $line = fgets($handle, 4096);

		if (preg_match('/^([^:]+):([^\|]+)\|.*processQueue\|(?:sending (?:message|reply)|trying to send message) ([\w]+)Message.*size=([0-9]+)/',$line,$m)) {
		
			$type = $m[3];

			switch ($type) {
				case 'BlockReply':
					preg_match('/brc:([^ ]+)/',$line,$m2);
					$totals[$m2[1]] += 1;
					$total+=1;
			}
		}
    }
    fclose($handle);
}

echo "\"BlockReply SubType\"\t\"# of messages\"\t%\n";

$count=count($peer);
foreach ($totals as $st=>$c) {
	echo $st."\t";
	echo $c."\t";
	echo (100*$c/$total)."\n";
}


?>
