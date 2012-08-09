<?
#$1 name of the experiment (-upload do copy stuff)
#$2 $3 $4 parameters to be passed (optional)

if (!$argv[1])
	die ('you forgot to name the experiment.'."\n");

$GLOBALS['testing']=false;
$GLOBALS['verbose']=true;

$numpeercasters=6; //6
$numsuperpeers=4; //16
$numhomepeers=4;  //64
$length=300;
$runs=10;
$sleepbetweenpeersmillis=1000;

$args = $argv[2].' '.$argv[3].' '.$argv[4];
$iface="ifconfig|tr '\\n' '|'|egrep -o 'eth[0-9][^\\|]+\|[^\\|]+192\\.168\\.100'|grep -o 'eth[0-9]'";
$ip="ifconfig|tr '\\n' '|'|egrep -o 'eth[0-9][^\\|]+\|[^\\|]+192\\.168\\.100\\.[0-9]+'|egrep -o '[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+'|grep -v 192.168.100.255";

$javaline='/home/hecht/liveshift/java/java -jar /home/hecht/liveshift/dist/liveshift-2.0.jar ';
$simulationdir= '/home/hecht/liveshift/results/'.$argv[1];
$mypath_local=dirname($_SERVER['SCRIPT_NAME']);
$resultsdir_local='/home/fabio/liveshift/results/'.$argv[1];

if (file_exists($resultsdir_local))
	die ('Experiment already exists. In order to prevent accidental overwriting, delete it first.'."\n");

//reads hosts file
$file = file("$mypath_local/host-parameter");
if (!$file)
	die("file $mypath_local/host-parameter not found.\n");

foreach ($file as $line)
	if ($line[0]!='#') {
		$splitline = split("\t",$line);
		for ($i=0; $i<$splitline[1]; $i++)
			$hosts[]=$splitline[0];
	}
echo "hosts loaded, total capacity is ".count($hosts)." peers\n";


if ($argv[1]=='-upload') {
	echo ('uploading to hosts.'."\n");

	$commands=array();
	foreach (array_unique($hosts) as $host) {
		$commands[]="rsync --exclude '.svn' -avL ".(FALSE!==strpos($host,'-p')?preg_replace('/(-p\d+).*$/',"--rsh='ssh $1' ",$host):'')." /home/hecht/liveshift/dist /home/hecht/liveshift/scripts ".substr($host,strpos($host,' '),strlen($host)).':/home/hecht/liveshift';
	}
	run_in_background_simultaneously_block($commands);

	die("\nuploading done.\n");
}

echo "=> running simulation \"$argv[1]\" with arguments: ".$args."\n";

//peercasters
$p=1;
for ($i=1; $i<=$numpeercasters; $i++ && $p++)
	$params['c'.$p]="--peer-name=c$p --bootstrap=[bootstrappeerip]:10001 -pp2p ".(10000+$p)." -penc ".(20000+$i)." --delete-storage -gur 5 --publish=C$i -i [iface] $args";

//super peers
for ($i=1; $i<=$numsuperpeers; $i++ && $p++)
	$params['s'.$p]="--peer-name=s$p --bootstrap=[bootstrappeerip]:10001 -pp2p ".(10000+$p)." --delete-storage -gur 5 -i [iface] -rss $length $args";

//home peers
for ($i=1; $i<=$numhomepeers; $i++ && $p++)
	$params['h'.$p]="--peer-name=h$p --bootstrap=[bootstrappeerip]:10001 -pp2p ".(10000+$p)." --delete-storage -gur .5 -i [iface] -rss $length $args";

if (count($params)>count($hosts))
	die('you need more host capacity to run this simulation. Why would you want to run '.count($params).' peers in '.count($hosts)." hosts??\n");


//prepares hosts
$commands=array();
foreach (array_unique($hosts) as $host) {
	$commands[$host][]='killall -9 -u hecht -w java';
	$commands[$host][]="rm -rf $simulationdir";
	$commands[$host][]='sudo ntpdate 130.60.75.52';
	$ifaces[$host]=trim(run_remotely_get_output($host,$iface));
}
run_remotely_simultaneously_block($commands);


//runs simulation
for ($run=1; $run<=$runs; $run++) {

	echo 'on run '.$run."\n";
	$rundir=$simulationdir.'/'.$run;

	//prepares folders
	echo "initializing hosts\n";
	$commands=array();
	foreach (array_unique($hosts) as $host) {
		//initializes host
		$commands[$host][]="rm -rf /tmp/LiveShift";
		$commands[$host][]="mkdir -p /tmp/LiveShift/log $rundir";
		$commands[$host][]="nohup ../../scripts/gridpeer/monitorload.sh /tmp/LiveShift/load";
	}
	run_remotely_simultaneously_block($commands);

	//it's showtime!
	echo 'launching peers: ';
	$lefthosts = $hosts;
	shuffle($lefthosts);
	$starttime=time();
	$bootstrappeerip='';
	foreach ($params as $peername=>$param) {
		echo $peername.' ';
		$host = array_pop($lefthosts);

		if (!$bootstrappeerip)
			$bootstrappeerip=trim(run_remotely_get_output($host,$ip));

		run_remotely_in_background($host,$javaline.str_replace(array('[bootstrappeerip]','[iface]'),array($bootstrappeerip,$ifaces[$host]),$param),$rundir.'/output.'.$peername,$rundir.'/error.'.$peername);

		usleep($sleepbetweenpeersmillis*1000);

		$usedhosts[$host]=1;
	}

	$sleepytimetime=$length-(time()-$starttime);
	echo "\nwaiting $sleepytimetime s for run $run to finish\n";
	sleep($sleepytimetime);

	echo 'run '.$run." finished, compressing data";

	//finishes run, compresses data
	$commands=array();
	foreach (array_keys($usedhosts) as $host) {

		$commands[$host][]="killall -w -u hecht java nohup";

		$commands[$host][]="egrep -H 'called|DELAY|achieved|((sent|received|dht)MessagesAverage)|sending message' /tmp/LiveShift/log/* > $rundir/pass1";
		$commands[$host][]="php -f /home/hecht/liveshift/scripts/gridpeer/data/delete-remainings.php $rundir/pass1 $length > $rundir/pass2";
		$commands[$host][]="tar czf $rundir/log.tar.gz /tmp/LiveShift/log /tmp/LiveShift/load";
/*
		$commands[$host][]="tar czf $rundir/errors.tar.gz $rundir/error.*";
		$commands[$host][]="tar czf $rundir/pass.tar.gz $rundir/pass[12]";
		$commands[$host][]="rm -rf $rundir/error.* $rundir/pass[12]";
*/

	}
	run_remotely_simultaneously_block($commands);

	//downloads results
	echo ", downloading data\n";
	$commands=array();
	foreach (array_keys($usedhosts) as $host) {
		$commands[]="mkdir -p $resultsdir_local/$run/".str_replace(array('-',' ','@'),'',$host);
		$commands[]='scp -C -r '.str_replace('-p','-P',$host).":$rundir/* $resultsdir_local/$run/*".str_replace(array('-',' ','@'),'',$host);
	}
	run_in_background_simultaneously_block($commands);
}


echo "all runs done. analyzing data.\n";
$commands=array("$mypath_local/data/makegraphs-runs-distributed.sh $resultsdir_local $length");
run_in_background_simultaneously_block($commands);


echo "all done. good luck!\n";

/**
 * receives an array with hosts and several commands
 * runs them in order on each host, but in parallel among hosts
 * waits until all hosts have returned before returning
 */
function run_remotely_simultaneously_block($commands) {
	foreach($commands as $host => $hostcommands) {

		$commandline = "ssh $host \"".join(';', $hostcommands)." \" &";
		if ($GLOBALS['testing']||$GLOBALS['verbose'])
			echo "\n".$commandline."\n";
		if (!$GLOBALS['testing'])
			shell_exec($commandline); //; echo $? to return status, but then no background :(
	}
	shell_exec('wait');
}

function run_remotely_in_background($host,$command,$outputstream,$errorstream) {
	$commandline = "ssh $host \"$command 1&> $outputstream 2&> $errorstream &\" &";
	if ($GLOBALS['testing']||$GLOBALS['verbose'])
		echo "\n".$commandline."\n";
	if (!$GLOBALS['testing'])
		shell_exec($commandline);
}
function run_remotely_get_output($host,$command) {
	$commandline = "ssh $host \"$command\"";
	if ($GLOBALS['testing']||$GLOBALS['verbose'])
		echo "\n".$commandline."\n";
	if (!$GLOBALS['testing'])
		return shell_exec($commandline);
}
function run_in_background_simultaneously_block($commands) {
	foreach($commands as $hostcommands) {

		$commandline = $hostcommands." &";
		if ($GLOBALS['testing']||$GLOBALS['verbose'])
			echo "\n".$commandline."\n";
		if (!$GLOBALS['testing'])
			$output = shell_exec($commandline); //; echo $? to return status, but then no background :(
		if ($GLOBALS['verbose'])
			echo $output;

	}

	shell_exec('wait');
}
?>

