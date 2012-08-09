<?

$GLOBALS['testing']=0;
$GLOBALS['verbose']=1;

$commands=array();
$commands['fabio@192.168.1.163'][]='killall -9 -u fabio -w java vlc';
$commands['fabio@192.168.1.163'][]='sudo ntpdate 130.60.75.52';
run_remotely_simultaneously_block($commands);

run_remotely_in_background('fabio@192.168.1.163','vlc /home/fabio/doc/tmp/F1.avi --sout \"#std{access=udp,mux=ts,dst=127.0.0.1:20001}\" -I dummy');

run_remotely_in_background('fabio@192.168.1.163',"java -jar ~/liveshift/dist/liveshift-2.0.jar -n c1 --bootstrap=192.168.1.163:10000 -pp2p ".(10000)." -i eth1 -penc ".(20001)." --delete-storage -gur 5 --publish=C1 -dus");
sleep (2);

$p=1;
run_remotely_in_background('fabio@192.168.1.163',"java -jar ~/liveshift/dist/liveshift-2.0.jar -n h$p --bootstrap=192.168.1.163:10000 -pp2p ".(10000+$p)." -i eth1 --delete-storage -gur 1 -c C1 -dus");
sleep(2);

$p++;
run_remotely_in_background('fabio@192.168.1.124',"java -jar ~/liveshift/dist/liveshift-2.0.jar -n h$p --bootstrap=192.168.1.163:10000 -pp2p ".(10000+$p)." -i eth0 --delete-storage -gur 1 -c C1 -dus");
sleep(2);

$p++;
run_remotely_in_background('fabio@192.168.1.163',"java -jar ~/liveshift/dist/liveshift-2.0.jar -n h$p --bootstrap=192.168.1.163:10000 -pp2p ".(10000+$p)." -i eth1 --delete-storage -gur 1 -c C1 -dus");
sleep(2);

$p++;
run_remotely_in_background('fabio@192.168.1.163',"java -jar ~/liveshift/dist/liveshift-2.0.jar -n h$p --bootstrap=192.168.1.163:10000 -pp2p ".(10000+$p)." -i eth1 --delete-storage -gur 1 -c C1 -dus");
sleep(2);

$p++;
run_remotely_in_background('fabio@192.168.1.124',"java -jar ~/liveshift/dist/liveshift-2.0.jar -n h$p --bootstrap=192.168.1.163:10000 -pp2p ".(10000+$p)." -i eth0 --delete-storage -gur 1 -c C1 -dus");

sleep(2);

$p++;
run_remotely_in_background('fabio@192.168.1.163',"java -jar ~/liveshift/dist/liveshift-2.0.jar -n h$p --bootstrap=192.168.1.163:10000 -pp2p ".(10000+$p)." -i eth1 --delete-storage -gur 1 -c C1 -dus");

sleep(2);

$p++;
run_remotely_in_background('fabio@192.168.1.163',"java -jar ~/liveshift/dist/liveshift-2.0.jar -n h$p --bootstrap=192.168.1.163:10000 -pp2p ".(10000+$p)." -i eth1 --delete-storage -gur 1 -c C1 -dus");

echo "all launched.\n";

/**
 * receives an array with hosts and several commands
 * runs them in order on each host, but in parallel among hosts
 * waits until all hosts have returned before returning
 */
function run_remotely_simultaneously_block($commands) {
	foreach($commands as $host => $hostcommands) {

		$commandline = "ssh $host \"".join(';', $hostcommands)."\"";
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
	$commandline = "ssh $host \"$command 1&> $outputstream 2&> $errorstream\"";
	if ($GLOBALS['testing']||$GLOBALS['verbose'])
		echo "\n".$commandline."\n";
	if (!$GLOBALS['testing'])
		return run_in_background($commandline);
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
	$stream = "nohup $Command & echo $!";
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


?>

