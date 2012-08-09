<?
# $1 name of the experiment (-upload do only copy stuff)
# $2 s1 s2 s3 scenarios, or anything
# $3 $4 $5 $6 $7 $8 $9 $10 $11 $12 parameters to be passed to all peers, including peercasters (optional)

if (!$argv[1])
	die ('you forgot to name the experiment.'."\n");

$experimentName=$argv[1];
$GLOBALS['testing']=false;
$GLOBALS['verbose']=true;

if (file_exists('/tmp/.liveshift.exp.lock')) {
	echo 'lock file exists. not running multiple concurrent experiments.'."\n";
	die(1);
}
touch('/tmp/.liveshift.exp.lock');


$numpeercasters=6;
$randomjoin=FALSE;
switch ($argv[2]) {

	case 's0':
		//ANDREAS
		$argv[1].='-'.$argv[2];
		$numsuperpeers=5;
		$numhomepeers=30;
	                
		$numsybilmasters=0;
		$sybilmastercapacity=0;
		$numcolludingsybils=0;

		$numcolluders=5;
		$colludercapacity=0;

		$length=600;
		$runs=10;
		$numpeercasters=1;
		$randomjoin=TRUE;   
		break;


	case 's1':
		$argv[1].='-'.$argv[2];
		$numsuperpeers=15;
		$numhomepeers=60;
		$length=1200;
		$runs=5;
		break;
	case 's2':
		$argv[1].='-'.$argv[2];
		$numsuperpeers=15;
		$numhomepeers=90;
		$length=1200;
		$runs=5;
		break;
	case 's3':
		$argv[1].='-'.$argv[2];
		$numsuperpeers=15;
		$numhomepeers=120;
		$length=1200;
		$runs=5;
		break;
	case 's4':
		$argv[1].='-'.$argv[2];
		$numsuperpeers=15;
		$numhomepeers=150;
		$length=1200;
		$runs=5;
		break;

	case '-';
		break;
	default:
		die ("specify a valid scenario!\n");

}

//configuration
$hostuser=array();

$sleepbetweenpeersmillis=1000;

$args = $argv[3].' '.$argv[4].' '.$argv[5].' '.$argv[6].' '.$argv[7].' '.$argv[8].' '.$argv[9].' '.$argv[10].' '.$argv[11].' '.$argv[12];
$iface="/sbin/ifconfig|tr '\\n' '|'|egrep -o '(eth|br)[0-9][^\\|]+\|[^\\|]+[ip]'|egrep -o '(eth|br)[0-9]'";
//$ip="ifconfig|tr '\\n' '|'|egrep -o '(eth|br)[0-9][^\\|]+\|[^\\|]+192\\.168\\.100\\.[0-9]+'|egrep -o '[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+'|grep -v 192.168.100.255";

$javaline='/home/[USER]/liveshift/java/java -jar /home/[USER]/liveshift/dist/liveshift-0.3.jar ';//-Djava.net.preferIPv4Stack=true 
$simulationdir= '/home/[USER]/liveshift/results/'.$argv[1];
$mypath_local=dirname($_SERVER['SCRIPT_NAME']);
$resultsdir_host='192.41.135.214';
$hostuser[$resultsdir_host]='hecht';
$resultsdir='/tmp/results-all/'.$argv[1];
$hostsfile = "$mypath_local/user-host-parameter-local";
$localuser=trim(`id | sed 's/uid=[0-9][0-9]*(\([^)]*\)).*/\\1/'`);
$ntpserver='192.41.135.214';
$gateway='192.41.135.236';
$hostuser[$gateway]='hecht';
$loadthreshold=1;

$do_NAT=false;

$context=$argv[1];
$ts=microtime(true)*10000;
$outputstream=str_replace(array(" ","/","*"),"-","out-$ts-$context");
$errorstream=str_replace(array(" ","/","*"),"-","err-$ts-$context");


//reads hosts file
$file = file($hostsfile);

if (!$file)
	die("file ($hostsfile) not found.\n");
if (run_remotely_get_output($resultsdir_host, "ls $resultsdir"))
	die ('Experiment dir already exists locally. In order to prevent accidental overwriting, delete it first.'."\n");

$uploadcommands=array();


foreach ($file as $line)
	if ($line && $line[0]!='#') {
		$splitline = split("\t",$line);
		$user = trim($splitline[0]);
		$host = trim($splitline[1]);
		if (!$user)
			continue;

		$hostuser[$host] = $user;
		$uploadcommands[]="rsync --exclude '.svn' -avL /home/$localuser/liveshift/dist /home/$localuser/liveshift/scripts $user@$host:/home/$user/liveshift";
		for ($i=0; $i<$splitline[2]; $i++)
			$hosts[]=$host;
		echo "Testing $user@$host";
		$load = can_reach($host,$resultsdir_host);
		if (!$load)
			die('Host '.$host.' is not responding, not running simulation');
//		if ($load>$loadthreshold)
//			die('Host '.$host.' is too loaded ('."$load>$loadthreshold".'), not running simulation');
	}


echo "hosts loaded from ($hostsfile), total capacity is ".count($hosts)." peers\n";

echo ("uploading to hosts.\n");
run_in_background_simultaneously_block($uploadcommands);
echo("\nuploading done.\n");

if ($argv[1]=='-upload')
	die ();

echo "=> running simulation \"$argv[1]\" with arguments: ".$args."\n";

//peercasters
$p=1;
for ($i=1; $i<=$numpeercasters; $i++ && $p++)
	$params['c'.$p]="-n c$p --bootstrap=[bootstrappeerip] -pp2p [pp2p] -i [iface] -pip [pip] -penc ".(9000+$i)." --delete-storage -gur 5 --publish=C$i -pl dummy $args";

//super peers
for ($i=1; $i<=$numsuperpeers; $i++ && $p++)
	$params['s'.$p]="-n s$p --bootstrap=[bootstrappeerip] -pp2p [pp2p] -i [iface] -pip [pip] --delete-storage -gur 5 -rss $length -pl dummy $args";

//home peers
for ($i=1; $i<=$numhomepeers; $i++ && $p++)
	$params['h'.$p]="-n h$p --bootstrap=[bootstrappeerip] -pp2p [pp2p] -i [iface] -pip [pip] --delete-storage -gur 0.5 -rss $length -pl dummy $args";

//sybil masters
for ($i=1; $i<=$numsybilmasters; $i++ && $p++)
	$params['a'.$p]="-n a$p --bootstrap=[bootstrappeerip] -pp2p [pp2p] -i [iface] -pip [pip] --delete-storage -gur $sybilmastercapacity -rss $length -pl dummy $args -nsyb $numcolludingsybils";

//colluders
for ($i=1; $i<=$numcolluders; $i++ && $p++)
	$params['b'.$p]="-n b$p --bootstrap=[bootstrappeerip] -pp2p [pp2p] -i [iface] -pip [pip] -delete-storage -gur $colludercapacity -rss $length -pl dummy $args";

if (count($params)>count($hosts))
	die('you need more host capacity to run this simulation. Why the hell would you want to run '.count($params).' peers with capacity '.count($hosts)."??\n");


//prepares hosts
$commands=array();
foreach (array_unique($hosts) as $host) {
	$commands[$host][]='killall -9 -u '.$hostuser[$host].' -w java';
	$commands[$host][]='rm -rf '.str_replace('[USER]',$hostuser[$host],$simulationdir);
	$ifaces[$host]=trim(run_remotely_get_output($host,str_replace('[ip]',substr($host,strpos($host,'@')+1),$iface)));
	if (!$ifaces[$host]) $ifaces[$host]='eth0';  //hope it's like this for emanicslab
}

run_remotely_simultaneously_block($commands);

if ($argv[1]=='-justkill')
	die("justkilledthem\n");

//runs simulation
for ($run=1; $run<=$runs; $run++) {

	while (file_exists('/tmp/.liveshift.wait.'.$experimentName) || file_exists('/tmp/.liveshift.wait.all')) {
                echo "waiting until wait file /tmp/.liveshift.wait is deleted\n";
                sleep(5);
        }

	echo 'on run '.$run."\n";

	//clears NAT
	echo "clearing NAT rules\n";
	$commands[$gateway][]="sudo iptables -F";
	$commands[$gateway][]="sudo iptables -t nat -F";
	if ($do_NAT)
		$commands[$gateway][]="sudo iptables -t nat -A POSTROUTING -o br0 -j MASQUERADE";
	run_remotely_simultaneously_block($commands);

	//prepares folders
	echo "initializing hosts\n";
	$commands=array();
	foreach (array_unique($hosts) as $host) {
		$rundir=str_replace('[USER]',$hostuser[$host],$simulationdir).'/'.$run;
		//initializes host
		$commands[$host][]="rm -rf /tmp/LiveShift/storage* /tmp/LiveShift/log";
		$commands[$host][]="mkdir -p /tmp/LiveShift/log $rundir";
		$commands[$host][]="sudo ntpdate $ntpserver";
		$load_monitor_pid[$host]=trim(run_remotely_in_background($host,'/home/'.$hostuser[$host]."/liveshift/scripts/gridpeer/monitorload.sh /tmp/LiveShift/log/load.$host"));
	}
	run_remotely_simultaneously_block($commands);
	
	//it's showtime!
	echo 'launching peers: ';
	$lefthosts = $hosts;
	shuffle($lefthosts);
	$starttime=time();
	$bootstrappeerip='';
	$outport=20001;
	$commandlines=array();
	$commands=array();
	foreach ($params as $peername=>$param) {
		echo $peername.' ';
		$host = array_pop($lefthosts);
		$pp2p=$outport++;
		$rundir=str_replace('[USER]',$hostuser[$host],$simulationdir).'/'.$run;
		
		if ($do_NAT && (FALSE!==strpos($host,'192.168.100.'))) { // a private ip address (in our testbed)

			$commands[$gateway][]="sudo iptables -t nat -A PREROUTING -p tcp -i eth1.151 --dport $pp2p -j DNAT --to $host";
			$commands[$gateway][]="sudo iptables -t nat -A PREROUTING -p udp -i eth1.151 --dport $pp2p -j DNAT --to $host";

			if (!$bootstrappeerip)  //with private IP
				$bootstrappeerip=$gateway.':'.$pp2p;

			$pip=$gateway;
		}
		else	//public IP address
			$pip=$host;

		if (!$bootstrappeerip)  //with public IP
			$bootstrappeerip=$host.':'.$pp2p;
		
		$commandline = array($host,str_replace('[USER]',$hostuser[$host],$javaline).str_replace(array('[bootstrappeerip]','[iface]','[pp2p]','[pip]'),array($bootstrappeerip,$ifaces[$host],$pp2p,$pip),$param),$rundir.'/output.'.$peername,$rundir.'/error.'.$peername);

		$commandlines[] = $commandline;

		$usedhosts[$host]=1;
	}
	if ($do_NAT)
		run_remotely_simultaneously_block($commands);
	
	//actually launches peer
	if($randomjoin) {
		run_remotely_in_background($commandlines[0][0], $commandlines[0][1], $commandlines[0][2], $commandlines[0][3]);
		usleep($sleepbetweenpeersmillis*1000);
		unset($commandlines[0]);
		$commandlines = array_values($commandlines);
		shuffle($commandlines);
	}

	$capacityleft = 5;
	$entriesleft = true;
	while($entriesleft)
	{
		$currentcapacity = 0;
		$pos = strpos($commandlines[0][1], '-gur 0.5');
		if($pos === false) 
		{
			$pos = strpos($commandlines[0][1], '-gur 5');
			if($pos !== false)
			{
				$currentcapacity = 4;
			}
		}
		else
		{
			$currentcapacity = -0.5;
		}
		
		if($capacityleft+$currentcapacity>=5)
		{	
			$capacityleft = $capacityleft + $currentcapacity;
			run_remotely_in_background($commandlines[0][0], $commandlines[0][1], $commandlines[0][2], $commandlines[0][3]);
			usleep($sleepbetweenpeersmillis*1000);
			unset($commandlines[0]);
			$commandlines = array_values($commandlines);
			if(count($commandlines) == 0)
			{
				$entriesleft = false;
			}
		} 
		else
		{
			shuffle($commandlines);
		}
	}	

#	foreach ($commandlines as $commandline) {
#		
#		run_remotely_in_background($commandline[0],$commandline[1],$commandline[2],$commandline[3]);
#		
#		usleep($sleepbetweenpeersmillis*1000);
#	}
	
	$sleepytimetime=$length-(time()-$starttime);
	$ticks=80;
	$tts = 1000000*$sleepytimetime/$ticks;
	echo "\nwaiting $sleepytimetime s for run $run to finish\n";
	echo str_repeat('_',$ticks+1)."\n|";
	for ($i=0;$i<$ticks;$i++) {
		usleep($tts);
		if (!(($i+1)%($ticks/4)))
			echo '|';
		else
			echo '=';
	}

	echo "\nrun $run finished, compressing data\n";

	$commands=array();
	foreach (array_keys($usedhosts) as $host) {
		$rundir=str_replace('[USER]',$hostuser[$host],$simulationdir).'/'.$run;

		$commands[$host][]="rm /tmp/LiveShift/load.lock";
		$commands[$host][]='killall -u '.$hostuser[$host].' -w java';
		$commands[$host][]="egrep -H 'INFO|LAG|called|DELAY|achieved|((sent|received|dht)MessagesAverage)|dhtOperation |in putSegmentBlock|duplicate block received|Playback failed|tuning' /tmp/LiveShift/log/liveshift.log.* | php -f /home/".$hostuser[$host]."/liveshift/scripts/gridpeer/data/delete-remainings.php $length > /tmp/LiveShift/log/pass2";
		$commands[$host][]="cd /tmp/LiveShift/log";
		$commands[$host][]="tar c liveshift.log.*|pigz > log.tar.gz";
		$commands[$host][]="cd $rundir";
		$commands[$host][]="tar c error.*|pigz > errors.tar.gz";
		$commands[$host][]="rm -rf $rundir/error.*";
	}
	run_remotely_simultaneously_block($commands);

	//collects results
	echo "collecting data\n";
	$commands=array();
	$commandslocal=array();
	foreach (array_keys($usedhosts) as $host) {
		$rundir=str_replace('[USER]',$hostuser[$host],$simulationdir).'/'.$run;

		$commands[$resultsdir_host][]="mkdir -p $resultsdir/$run/".str_replace(array('-',' ','@','.'),'_',$host);
		$commandslocal[]="scp -C -r ".$hostuser[$host]."@$host:$rundir/* $resultsdir_host:$resultsdir/$run/".str_replace(array('-',' ','@','.'),'_',$host);
		$commandslocal[]="scp -C -r ".$hostuser[$host]."@$host:\"/tmp/LiveShift/log/{pass2,log.tar.gz,load.$host}\" $resultsdir_host:$resultsdir/$run/".str_replace(array('-',' ','@','.'),'_',$host);
	}
	run_remotely_simultaneously_block($commands);
	run_in_background_simultaneously_block($commandslocal);
}


echo "all runs done. analyzing data and graphing.\n";

$commands[$resultsdir_host]=array("/home/".$hostuser[$resultsdir_host]."/liveshift/scripts/gridpeer/data/makegraphdatasets-runs-distributed.sh $resultsdir $length");
$commands[$resultsdir_host][]='/home/'.$hostuser[$resultsdir_host].'/liveshift/scripts/gridpeer/data/makegraphs-runs-distributed.sh '.$resultsdir;
run_remotely_simultaneously_block($commands);


echo "all done. good luck!\n";

/**
 * receives an array with hosts and several commands
 * runs them in order on each host, but in parallel among hosts
 * waits until all hosts have returned before returning
 */
function run_remotely_simultaneously_block($commands) {
	global $hostuser;

	foreach($commands as $host => $hostcommands) {

		$commandline = "ssh ".$hostuser[$host]."@$host \"".join(';', $hostcommands)."\"";
		if ($GLOBALS['testing']||$GLOBALS['verbose'])
			echo "\n".$commandline."\n";
		if (!$GLOBALS['testing'])
			$pid[]=run_in_background($commandline); //; echo $? to return status, but then no background :(
	}
	while (1) {
		$alldone=true;
		foreach ($pid as $v)
			$alldone &= !is_process_running($v);
		if ($alldone)
			break;
		sleep(1);
	}
}

function run_remotely_in_background($host,$command,$outputstream='/dev/null',$errorstream='/dev/null') {
	global $hostuser,$argv;
	
	$commandline = "ssh ".$hostuser[$host]."@$host \"$command 1> $outputstream 2> $errorstream\"";
	if ($GLOBALS['testing']||$GLOBALS['verbose'])
		echo "\n".$commandline."\n";
	if (!$GLOBALS['testing'])
		return run_in_background($commandline);
}
function run_remotely_get_output($host,$command) {
	global $hostuser;

	$commandline = "ssh ".$hostuser[$host]."@$host \"$command\"";
	if ($GLOBALS['testing']||$GLOBALS['verbose'])
		echo "\n".$commandline."\n";
	if (!$GLOBALS['testing'])
		return shell_exec($commandline);
}
function run_in_background_simultaneously_block($commands) {
	foreach($commands as $hostcommands) {

		$commandline = $hostcommands;
		if ($GLOBALS['testing']||$GLOBALS['verbose'])
			echo "\n".$commandline."\n";
		if (!$GLOBALS['testing'])
			$pid[]=run_in_background($commandline);
	}

	while (1) {
		$alldone=true;
		foreach ($pid as $v)
			$alldone &= !is_process_running($v);
		if ($alldone)
			break;
		sleep(1);
	}
}
  function run_in_background($Command)
   {
   	global $outputstream,$errorstream;
   
	$stream = "nohup $Command  1>> $outputstream 2>> $errorstream & echo $!";
	if ($GLOBALS['testing']||$GLOBALS['verbose'])
		echo $stream."\n";
	$h=popen($stream, 'r');
	$PID=fread($h, 2096);
	pclose($h);

       return($PID);
   }


   function is_process_running($PID)
   {
       exec("ps $PID", $ProcessState);
       return(count($ProcessState) >= 2);
   }

function get_load($host) {
	global $hostuser;
	$load=run_remotely_get_output($host,'cat /proc/loadavg');
	return substr($load,0,strpos($load,' '));
}

function can_reach($host,$otherhost) {
        global $hostuser;
        $load=run_remotely_get_output($host,'ping '.$otherhost.' -c 1');
        return strpos($load,'64 bytes from')!==FALSE;
}


unlink('/tmp/.liveshift.exp.lock');

?>

