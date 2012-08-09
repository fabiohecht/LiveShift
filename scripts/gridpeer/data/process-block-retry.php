<?
error_reporting(E_ERROR | E_WARNING | E_PARSE);

if ($argv[1])
	$handle = fopen($argv[1],'r');
else
	$handle = fopen('php://stdin','r');


if ($handle) {
    while (!feof($handle)) {
        $line = fgets($handle, 4096);
	
		if (strpos($line,'holding')!==FALSE) {
			//changed channgel, clears everything (measurements useless since user changed interest -- há controvérsias, pq pode ser q manteve interesse se o delta TS foi pequeno...)
			
			$neigh=array();
			$time=array();
			//echo "reset\n";
		}
        else if (preg_match('/^([^:]+):([^\|]+)\|.*\|si:C(\d+),(\d+):0 b#(\d+) is held by (\d+) USG and (\d+) neighbors/',$line,$m)) {
			
			$class = $m[1][0]=='s'?'super':($m[1][0]=='h'?'home':'peercaster');
			
			$neigh[$m[4].'#'.$m[5]][] = $m[7];
			$time[$m[4].'#'.$m[5]][] = getTime($m[2]);
			
			//echo $line;
			//echo "set ".$m[4].'#'.$m[5].' #n='.$m[7].' t='.$m[2]." ".getTime($m[2])."\n";
		}
		else if (preg_match('/^([^:]+):([^\|]+)\|.*\|playback policy: PLAY .* segment (\d+) b#(\d+)/',$line,$m)) {
			
			//echo $line;
			//echo "report? ".$m[3].'#'.$m[4]." ".getTime($m[2])."\n";
			
			$key1=$m[3].'#'.$m[4];
			
			if (isset($neigh[$key1])) {
				
				$t = getTime($m[2]);
				foreach ($neigh[$key1] as $key2=>$report) {
					//echo $report.' '.($t-$time[$key1][$key2])."\n";
					$data[$report][]=$t-$time[$key1][$key2];
					$success[$report]['yes']+=1;
				}
				
				$neigh=array();
				$time=array();
			}
		}
		else if (preg_match('/^([^:]+):([^\|]+)\|.*\|playback policy: SKIP .* segment (\d+) b#(\d+)/',$line,$m)) {
			//gave up retrying block
			
			$key1=$m[3].'#'.$m[4];
			
			if (isset($neigh[$key1])) {
				foreach ($neigh[$key1] as $key2=>$report) {
					//echo $report." infinite\n";
					$success[$report]['no']+=1;
				}
			}
			
			$neigh=array();
			$time=array();
			//echo "reset\n";
		}

    }
    fclose($handle);
}


ksort($data);



echo "\"#neighbors\"\t\"p (coming in 10)\"\n";

foreach ($success as $nneigh=>$data1) {

	$came=$data1['yes'];
	$didnt=$data1['no'];
	
	echo $nneigh."\t".($came/($came+$didnt))."\n";
}


echo "\"#neighbors\"\t\"waiting time (avg)\"\t\"waiting time (sd)\"\n";

foreach ($data as $nneigh=>$data1) {

	$avg=avg($data1);
	$sd=sd($data1);
	
	echo $nneigh."\t".$avg."\t".$sd."\n";
}

function getTime($strTime) {
	$a = preg_split('/\.|:/',$strTime);
	$out = $a[0];
	$out = $out*60 + $a[1];
	$out = $out*60 + $a[2];
	$out = $out*1000 + $a[3];

	return $out;
}


// Function to calculate square of value - mean
function sd_square($x, $mean) { return pow($x - $mean,2); }

// Function to calculate standard deviation (uses sd_square)    
// square root of sum of squares devided by N-1
function sd($array) {return count($array)<=1?0:sqrt(array_sum(array_map("sd_square", $array, array_fill(0,count($array), (avg($array)) ) ) ) / (count($array)-1) );}

function avg($array) {return count($array)==0?0:array_sum($array)/count($array);}

?>
