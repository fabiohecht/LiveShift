package net.liveshift.core;

import java.io.File;

import java.util.ArrayList;
import java.util.List;
import net.liveshift.configuration.Configuration.EncoderName;
import net.liveshift.configuration.Configuration.PlayerName;
import net.liveshift.core.LiveShiftApplication.ConnectionState;
import net.liveshift.gui.LiveShiftGUI;
import net.liveshift.incentive.IncentiveMechanism.IncentiveMechanismType;
import net.liveshift.time.Clock;
import net.liveshift.video.playbackpolicies.PlaybackPolicy.PlaybackPolicyType;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command Line Interface for the LiveShift application
 * 
 * Start the application and acts according to given command line arguments (e.g. bootstraps, etc.)
 * 
 * @author Kevin Leopold
 * 
 */
public class CommandLineInterface {

	// Init logger
	private static final Logger logger = LoggerFactory.getLogger(CommandLineInterface.class);

	private LiveShiftApplication liveShiftApplication;

	/**
	 * The command line options
	 */
	private Options lvOptions;

	/**
	 * The time when application first switched into a channel
	 */
	private long firstChannelSwitchTime = 0;

	private boolean isDone = false;
	private boolean isStatic;

	public CommandLineInterface(String[] args) {
		this(args, false);
	}

	public CommandLineInterface(String[] args, boolean isStatic) {

		String joined = "";
		for (String arg : args)
			joined += " " + arg;
		System.out.println(joined);

		this.isStatic = isStatic;
		this.initialize(args);
	}

	/**
	 * Creates the command line options
	 * 
	 * @return
	 */
	@SuppressWarnings("static-access")
	private static Options createOptions() {
		Options lvOptions = new Options();

		lvOptions.addOption(OptionBuilder.withArgName("playbackpolicy").withDescription("playback policy (skipstall,rdt,retry,ratio,sretry,cu)").hasArg()
				.create("pp"));
		lvOptions.addOption(OptionBuilder.withArgName("playbackpolicyparameter")
				.withDescription("playback policy parameter (depends on the playback policy chosen)").hasArg().create("ppp"));
		lvOptions.addOption(OptionBuilder.withArgName("playbackpolicyparameter2")
				.withDescription("playback policy parameter 2 (depends on the playback policy chosen)").hasArg().create("ppp2"));
		lvOptions.addOption(OptionBuilder.withArgName("playbuffer").withDescription("initial playback buffer (in seconds, default 6)").hasArg().create("pb"));

		lvOptions.addOption(OptionBuilder.withArgName("bootstrap-node").withLongOpt("bootstrap").withDescription("the address of the bootstrap node to use")
				.hasArg().create("b"));
		lvOptions.addOption(OptionBuilder.withArgName("peer-name").withLongOpt("peer-name").withDescription("sets the peer name of this instance").hasArg()
				.create("n"));
		lvOptions.addOption(OptionBuilder.withArgName("channel-name").withLongOpt("publish")
				.withDescription("publishes a channel with given name as soon as peer is bootstrapped").hasArg().create("p"));
		lvOptions.addOption(OptionBuilder.withArgName("channel-name").withLongOpt("channel")
				.withDescription("the name of the channel to switch to after successful bootstrapping").hasArg().create("c"));
		lvOptions.addOption(OptionBuilder.withArgName("interface").withLongOpt("interface").withDescription("the network interface to use for communication")
				.hasArg().create("i"));

		lvOptions.addOption(OptionBuilder.withArgName("port-p2p").withLongOpt("port-p2p").withDescription("the local p2p (dht) port").hasArg().create("pp2p"));
		lvOptions.addOption(OptionBuilder.withArgName("port-encoder").withLongOpt("port-encoder").withDescription("the encoder port").hasArg().create("penc"));
		lvOptions.addOption(OptionBuilder.withArgName("port-player").withLongOpt("port-player").withDescription("the player port").hasArg().create("pplr"));
		lvOptions.addOption(OptionBuilder.withArgName("public-ip").withLongOpt("public-ip")
				.withDescription("the public IP, if connected via a NAT with P2P port properly forwarded").hasArg().create("pip"));

		lvOptions.addOption(OptionBuilder.withArgName("encode-multicast").withLongOpt("encode-multicast")
				.withDescription("sets the encoder to encode the stream received in the provided multicast group address").hasArg().create("encmulti"));

		lvOptions.addOption(OptionBuilder.withLongOpt("delete-storage")
				.withDescription("deletes / empties the local video storage folder first before starting LiveShift").create("d"));
		lvOptions.addOption(OptionBuilder.withLongOpt("freeride").withDescription("if set, this peer will act as a freerider and deny all block requests")
				.create("f"));

		lvOptions.addOption(OptionBuilder.withArgName("player").withLongOpt("player")
				.withDescription("the name of player to be used. " + "Three options available: Dummy, VLCJ [default], ExtVLC").hasArg().create("pl"));

		lvOptions.addOption(OptionBuilder.withArgName("encoder").withLongOpt("encoder")
				.withDescription("the name of encoder to be used. " + "Three options available: Dummy, VLCJ, ExtVLC-Hidden [default], ExtVLC").hasArg()
				.create("enc"));

		lvOptions.addOption(OptionBuilder.withArgName("num-subscribers").withLongOpt("num-subscribers")
				.withDescription("sets the maximum number of subscribers per slot").hasArg().create("nsubs"));
		lvOptions.addOption(OptionBuilder.withLongOpt("no-have-suppression").withDescription("disables have suppression").create("nohs"));

		lvOptions.addOption(OptionBuilder
				.withArgName("reputation-mechanism")
				.withLongOpt("reputation")
				.withDescription(
						"the name of the incentive / reputation mechanism to use. " + "Three options available: SDUC [default], FCFS, RANDOM, TFT, PSH")
				.hasArg().create("r"));
		lvOptions.addOption(OptionBuilder
				.withArgName("script-code")
				.withLongOpt("script")
				.withDescription(
						"script-code describes a script according to which LiveShift will react:                            "
								+ "c:channel-name   tune into channel (see option -c),                       "
								+ "p:channel-name   publish channel (see option -p),                         "
								+ "w:seconds   wait for a given amount of seconds,                           "
								+ "s:switch    switch to a certain time, if negative relative to current time"
								+ "q   quits the application                                                 "
								+ "the above orders are seperated by '|' ,                                   "
								+ "E.g.: 'w:5|c:MyChannel|w:300|c:MyOtherChannel',                           "
								+ "if this option is set, options -c, -p and -rss are ignored.").hasArg().create("s"));
		lvOptions.addOption(OptionBuilder
				.withArgName("random-switch-arguments")
				.withLongOpt("random-switch-scenario")
				.withDescription(
						"this parameter will start a scenario where the peer will switch a channel with given probability(p)"
								+ "after each given amount of time(ts) in seconds, and will quit the application"
								+ "after another given amount of time(tq) in seconds. The scenario will start"
								+ "after an initial delay (id) in seconds.                                   "
								+ "random-switch-arguments looks like the following: id|p|ts|tq,             "
								+ "e.g.: 80|0.5|60|360                                                       "
								+ "if this option is set, options -c and -p are ignored.").hasArg().create("rss"));

		lvOptions.addOption(OptionBuilder.withArgName("gur").withDescription("global upload rate (float), in substreams/second").hasArg().create("gur"));

		lvOptions.addOption("churn", true, "churn probability 0..1");

		lvOptions.addOption("nodus", false, "dectivates dynamic upload slots");
		lvOptions.addOption("vh", false, "activates sending vector haves instead of scalar haves");

		lvOptions.addOption("g", "gui", false, "shows a graphical user interface. if set, options -b, -c, -s, -rss and -p are ignored.");
		lvOptions.addOption("h", "help", false, "shows this usage help");

		return lvOptions;
	}

	/**
	 * Main method
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		CommandLineInterface cli = new CommandLineInterface(args, true);
	}

	public void initialize(String[] args) {

		// Create command line options
		lvOptions = createOptions();

		// Parse command line arguments
		CommandLine cmd = null;
		try {
			cmd = new PosixParser().parse(lvOptions, args);
		}
		catch (ParseException e1) {
		}

		// Print help if needed
		if (cmd == null || cmd.hasOption("h")) {
			printUsage();
			return;
		}

		// Get command line arguments
		boolean hasBootstrap = cmd.hasOption("b");
		final String bootstrapAddress = cmd.getOptionValue("b");
		boolean switchToChannel = cmd.hasOption("c");
		String channelName = cmd.getOptionValue("c");
		boolean hasPeerName = cmd.hasOption("n");
		String peerName = cmd.getOptionValue("n");
		boolean publish = cmd.hasOption("p");
		String publishChannelName = cmd.getOptionValue("p");
		boolean hasReputationMechanism = cmd.hasOption("r");
		String reputationMechanism = cmd.getOptionValue("r");
		boolean hasPlayer = cmd.hasOption("pl");
		String player = cmd.getOptionValue("pl");
		boolean hasEncoder = cmd.hasOption("enc");
		String encoder = cmd.getOptionValue("enc");

		boolean hasScript = cmd.hasOption("s");
		String script = cmd.getOptionValue("s");
		boolean hasRandomSwitchScenario = cmd.hasOption("rss");
		String randomSwitchScenario = cmd.getOptionValue("rss");

		boolean useInterface = cmd.hasOption("i");
		String networkInterface = cmd.getOptionValue("i");
		boolean deleteStorage = cmd.hasOption("d");
		boolean freeride = cmd.hasOption("f");

		boolean hasPortP2P = cmd.hasOption("pp2p");
		String portP2P = cmd.getOptionValue("pp2p");

		boolean hasPortEncoder = cmd.hasOption("penc");
		String portEncoder = cmd.getOptionValue("penc");
		boolean hasPortPlayer = cmd.hasOption("pplr");
		String portPlayer = cmd.getOptionValue("pplr");

		boolean hasPublicIp = cmd.hasOption("pip");
		final String publicIp = cmd.getOptionValue("pip");

		boolean hasEncodeMulticast = cmd.hasOption("encmulti");
		String encodeMulticast = cmd.getOptionValue("encmulti");

		boolean hasGlobalUploadRate = cmd.hasOption("gur");
		String globalUploadRate = cmd.getOptionValue("gur");

		boolean hasChurnProbability = cmd.hasOption("churn");
		final String churnProbability = cmd.getOptionValue("churn");

		boolean hasNoDynamicUsAllocation = cmd.hasOption("nodus");
		boolean hasSendVectorHaves = cmd.hasOption("vh");

		boolean gui = cmd.hasOption("g");

		boolean hasNumSubscribers = cmd.hasOption("num-subscribers");
		String numSubscribers = cmd.getOptionValue("num-subscribers");
		boolean hasNoHaveSuppression = cmd.hasOption("no-have-suppression");

		boolean hasPlaybackPolicy = cmd.hasOption("pp");
		String playbackPolicy = cmd.getOptionValue("pp");
		boolean hasPlaybackPolicyParameter = cmd.hasOption("ppp");
		String playbackPolicyParameter = cmd.getOptionValue("ppp");
		boolean hasPlaybackPolicyParameter2 = cmd.hasOption("ppp2");
		String playbackPolicyParameter2 = cmd.getOptionValue("ppp2");
		boolean hasPlaybackBuffering = cmd.hasOption("pb");
		String playbackBuffering = cmd.getOptionValue("pb");

		String options = "";
		for (String string : args) {
			options += " " + string;
		}
		logger.info("Command-line options passed: " + options);

		// Create Application
		liveShiftApplication = new LiveShiftApplication();

		// Set default network interface
		if (useInterface) {
			logger.info("Using interface " + networkInterface);
			liveShiftApplication.getConfiguration().setNetworkInterface(networkInterface);
		}

		// set peer name
		if (hasPeerName) {
			liveShiftApplication.getConfiguration().setPeerName(peerName);
			liveShiftApplication.getConfiguration().setStorageDir(System.getProperty("java.io.tmpdir") + File.separatorChar + "LiveShift" + File.separatorChar + "storage-" + peerName);
		}

		// sets ports
		if (hasPortP2P)
			liveShiftApplication.getConfiguration().setP2pPort(Integer.parseInt(portP2P));

		if (hasPortEncoder)
			liveShiftApplication.getConfiguration().setEncoderPort(Integer.parseInt(portEncoder));
		if (hasPortPlayer)
			liveShiftApplication.getConfiguration().setPlayerPort(Integer.parseInt(portPlayer));

		if (hasGlobalUploadRate)
			liveShiftApplication.getConfiguration().setUploadRate(Integer.parseInt(globalUploadRate));

		liveShiftApplication.getConfiguration().noDynamicUploadSlotManagement = hasNoDynamicUsAllocation;
		liveShiftApplication.getConfiguration().vectorHavesEnabled = hasSendVectorHaves;

		// Delete Video storage
		if (deleteStorage) {
			boolean success = deleteFolder(new File(liveShiftApplication.getConfiguration().getStorageDirectory()));
			logger.info("Deleting local video storage: " + success);
		}
		// Set Freerider
		if (freeride) {
			liveShiftApplication.getConfiguration().freeride = true;
			logger.info("Acting as a freerider");
		}

		// have supprresion, number of subscribers
		if (hasNoHaveSuppression)
			liveShiftApplication.getConfiguration().noHaveSuppression = true;
		if (hasNumSubscribers) {
			try {
				liveShiftApplication.getConfiguration().numSubscribers = Integer.valueOf(numSubscribers);
			}
			catch (NumberFormatException e) {
				System.err.println("invalid argument passed as number of subscribers - must be a number");
				e.printStackTrace();
				System.exit(1);
			}
		}
		else {
			liveShiftApplication.getConfiguration().numSubscribers = 5;
		}

		// Set incentive mechanism
		if (hasReputationMechanism) {
			reputationMechanism = reputationMechanism.toLowerCase();
			if (reputationMechanism.equals("tft")) {
				liveShiftApplication.getConfiguration().setIncentiveMechanismType(IncentiveMechanismType.TFT);
				logger.info("Using reputation / incentive mechanism: TFT");
			}
			else if (reputationMechanism.equals("psh")) {
				liveShiftApplication.getConfiguration().setIncentiveMechanismType(IncentiveMechanismType.PSH);
				logger.info("Using reputation / incentive mechanism: PSH");
			}
			else if (reputationMechanism.equals("random")) {
				liveShiftApplication.getConfiguration().setIncentiveMechanismType(IncentiveMechanismType.RANDOM);
				logger.info("Using reputation / incentive mechanism: RANDOM");
			}
			else if (reputationMechanism.equals("fcfs")) {
				liveShiftApplication.getConfiguration().setIncentiveMechanismType(IncentiveMechanismType.FCFS);
				logger.info("Using reputation / incentive mechanism: FCFS");
			}
			else {
				liveShiftApplication.getConfiguration().setIncentiveMechanismType(IncentiveMechanismType.SDUC);
				logger.info("Using reputation / incentive mechanism: SDUC");
			}
		}
		else {
			liveShiftApplication.getConfiguration().setIncentiveMechanismType(IncentiveMechanismType.SDUC);
			logger.info("Using reputation / incentive mechanism: SDUC");
		}

		// playback policy
		PlaybackPolicyType playbackPolicyType = null;
		if (hasPlaybackPolicy) {
			if (playbackPolicy.equals("skipstall"))
				playbackPolicyType = PlaybackPolicyType.SKIPSTALL;
			else if (playbackPolicy.equals("rdt"))
				playbackPolicyType = PlaybackPolicyType.RDT;
			else if (playbackPolicy.equals("retry"))
				playbackPolicyType = PlaybackPolicyType.RETRY;
			else if (playbackPolicy.equals("ratio"))
				playbackPolicyType = PlaybackPolicyType.RATIO;
			else if (playbackPolicy.equals("sretry"))
				playbackPolicyType = PlaybackPolicyType.SMART_RETRY;
			else if (playbackPolicy.equals("catchup"))
				playbackPolicyType = PlaybackPolicyType.CATCHUP;
			else {
				System.err.println("Unrecognized playback policy: " + playbackPolicy);
				System.exit(1);
			}
		}
		else
			playbackPolicyType = PlaybackPolicyType.RATIO;

		liveShiftApplication.getConfiguration().setPlaybackPolicy(playbackPolicyType);

		if (hasPlaybackPolicyParameter)
			liveShiftApplication.getConfiguration().setPlaybackPolicyParameter(Float.parseFloat(playbackPolicyParameter));
		if (hasPlaybackPolicyParameter2)
			liveShiftApplication.getConfiguration().setPlaybackPolicyParameter2(Float.parseFloat(playbackPolicyParameter2));

		if (hasPlaybackBuffering)
			liveShiftApplication.getConfiguration().setPlaybackBuffering(Integer.parseInt(playbackBuffering));
		else
			liveShiftApplication.getConfiguration().setPlaybackBuffering(6);

		// Set encoder
		if (hasEncoder) {
			encoder = encoder.toLowerCase();
			if (encoder.equals("dummy")) {
				liveShiftApplication.getConfiguration().setEncoderName(EncoderName.Dummy);
			}
			else if (encoder.equals("extvlc")) {
				liveShiftApplication.getConfiguration().setEncoderName(EncoderName.ExtVlc_hidden);
			}
			logger.info("Using encoder: " + liveShiftApplication.getConfiguration().getEncoderName().name());
		}

		// Set player
		if (hasPlayer) {
			player = player.toLowerCase();
			if (player.equals("dummy")) {
				liveShiftApplication.getConfiguration().setPlayerName(PlayerName.Dummy);
			}
			else if (player.equals("vlcj")) {
				liveShiftApplication.getConfiguration().setPlayerName(PlayerName.Vlcj);
			}
			else if (player.equals("extvlc")) {
				liveShiftApplication.getConfiguration().setPlayerName(PlayerName.ExtVlc);
			}
			else {
				liveShiftApplication.getConfiguration().setPlayerName(PlayerName.Vlcj);
			}
		}
		else {
			liveShiftApplication.getConfiguration().setPlayerName(PlayerName.Vlcj);
		}
		logger.info("Using player: " + liveShiftApplication.getConfiguration().getPlayerName().name());

		// set bootstrap node
		if (hasBootstrap) {
			liveShiftApplication.getConfiguration().setBootstrapAddress(bootstrapAddress);
		}

		// Show GUI if set
		if (gui) {
			LiveShiftGUI liveShiftGUI = new LiveShiftGUI(liveShiftApplication);
			
		}
		else {
			// Check arguments
			String errorMessage = "";
			if (hasBootstrap) {
				if (!hasRandomSwitchScenario && !hasScript && !switchToChannel && !publish) {
					errorMessage = "Either script, random-switch-(timeshift-)scenario, channel to switch to or channel to publish must be set.";
				}
			}
			else {
				if (hasRandomSwitchScenario || hasScript || switchToChannel || publish) {
					errorMessage = "Bootstrap node must be set.";
				}
				if (!hasRandomSwitchScenario && !hasScript && !switchToChannel && !publish) {
					// errorMessage =
					logger.info("Nothing to do, starting GUI.");
					
					LiveShiftGUI liveShiftGUI = new LiveShiftGUI(liveShiftApplication);

					return;
				}
			}
			if (!errorMessage.isEmpty()) {
				quitError(errorMessage);
			}

			// Check for script and prepare script
			List<String> scriptOrders = new ArrayList<String>();
			if (hasScript) {
				String[] scriptOrderArr = script.split(",");
				for (String order : scriptOrderArr) {
					if (!order.trim().isEmpty())
						scriptOrders.add(order.trim());
				}
				if (scriptOrders.isEmpty()) {
					quitError("Script has no orders to execute.");
				}
			}

			// Try to connect to given bootstrap node
			boolean successConnect = false;

			while (!successConnect) {
				logger.info("Bootstrapping to " + bootstrapAddress);
				try {
					successConnect = liveShiftApplication.connect(bootstrapAddress, hasPublicIp ? publicIp : null);
				}
				catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();

					successConnect = false;
				}

				if (!successConnect) {
					logger.warn("Bootstrapping failed. Waiting to try again.");
					try {
						Thread.sleep(1000);
					}
					catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}

			// Work through script, if script given
			if (hasScript) {
				for (String order : scriptOrders) {
					logger.info(order);

					try {
						// Parse script
						String action = order.substring(0, 1);
						String argument = "";
						try {
							argument = order.substring(1);
						}
						catch (Exception e) {
						}

						if (action.equals("w")) {
							long waitFor = Long.parseLong(argument);
							waitAndCheckPlayback(liveShiftApplication, waitFor);
						}
						else if (action.equals("c")) {
							tuneIntoChannel(argument, liveShiftApplication);
						}
						else if (action.equals("s")) {
							tuneIntoChannel(/* channel, */argument, liveShiftApplication);
						}
						else if (action.equals("p")) {
							publishChannel(argument, liveShiftApplication, encodeMulticast);
						}
						else if (action.equals("q")) {
							quitApplication(0, liveShiftApplication);
						}
					}
					// Go on if an order was not understood
					catch (Exception e) {
						System.err.println("Parameter " + order + " of script parameter was not understood and skipped.");
						logger.error("Error parsing script argument", e);
					}
				}
			}
			// Run random switch scenario, if given
			else if (hasRandomSwitchScenario) {
				String[] randomSwitchArguments = randomSwitchScenario.split("\\|");
				try {
					final int quitTime = Integer.parseInt(randomSwitchArguments[0]);

					final float churnProbabilityFloat;
					if (hasChurnProbability)
						churnProbabilityFloat = Float.parseFloat(churnProbability);
					else
						churnProbabilityFloat = 0;

					if (quitTime > 0) {
						// Create thread that performs the randomSwitchScenario
						// and returns
						Thread randomSwitchThread = new Thread(new Runnable() {
							@Override
							public void run() {
								try {
									randomSwitchScenario(quitTime, liveShiftApplication, churnProbabilityFloat, bootstrapAddress, publicIp);
								}
								catch (Exception e) {
									System.err.println("Exception during randomSwitchScenario:" + e.getMessage());
									e.printStackTrace();
									quitError("Exception during randomSwitchScenario:" + e.getMessage());
								}
							}
						});
						randomSwitchThread.start();

					}
					else {
						quitError("quitTime parameter random-switch-arguments must be greater than zero.");
					}
				}
				catch (Exception e) {
					quitError("Parameter random-switch-arguments (" + randomSwitchScenario + ") is malformed: " + e.getMessage());
				}
			}
			// Otherwise do action as given
			else {
				// Try to switch to given channel
				if (switchToChannel) {
					tuneIntoChannel(channelName, liveShiftApplication);
				}
				// Try to publish channel
				if (publish) {
					publishChannel(publishChannelName, liveShiftApplication, encodeMulticast);
				}
			}

		}

	}

	/**
	 * Deletes a folder and all its contents recursively
	 * 
	 * @param folder
	 *            the folder to delete
	 * @return true if deleted successfully, false otherwise
	 */
	private boolean deleteFolder(File folder) {
		if (folder.isDirectory()) {
			String[] children = folder.list();
			for (int i = 0; i < children.length; i++) {
				boolean success = deleteFolder(new File(folder, children[i]));
				if (!success) {
					return false;
				}
			}
		}
		return folder.delete();
	}

	/**
	 * Runs a scenario where the peer will switch a channel or timeshift with given probability after a given amount of time in seconds, and will quit the
	 * application after another given amount of time in seconds.
	 * 
	 * @param initialDelay
	 * @param switchTime
	 * @param switchProbability
	 * @param quitTime
	 * @param bootstrapAddress 
	 * @param publicIp) 
	 * @param timeshift
	 *            if true, scenario will switch channels or timeshift, else it will only switch channels
	 */
	private void randomSwitchScenario(final int quitTime, final LiveShiftApplication liveShiftApplication, final float churnProbability, String bootstrapAddress, String publicIp) {

		// Create thread that quits application after given time
		Thread quitThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(quitTime * 1000);
					quitApplication(0, liveShiftApplication);
				}
				catch (InterruptedException e) {
				}
			}
		});
		quitThread.start();

		// Create Thread that switches channels each switchTime seconds
		logger.info("Starting the random-switch-scenario.");

		// int peerId = Integer.parseInt(liveShiftApplication.getDht().getMyId().getName().substring(1))-6; //s7 is peer 1 and so on
		PeerBehavior peerBehavior = new ProbabilisticPeerBehavior();

		while (true) {
			try {

				boolean churn = peerBehavior.shouldChurn(churnProbability);

				long holdingTimeS = peerBehavior.getHoldingTimeS();

				Channel channel = null;
				if (churn) {
					logger.info("churning out and holding for [" + holdingTimeS + "] s");

					if (liveShiftApplication.getConnectionState() == ConnectionState.CONNECTED)
						liveShiftApplication.disconnect();
				}
				else {
					if (liveShiftApplication.getConnectionState() == ConnectionState.DISCONNECTED)
						liveShiftApplication.connect(bootstrapAddress, publicIp);

					int channelNumber = peerBehavior.getChannelNumber();
					logger.info("finding channel [C" + channelNumber + "]");
					while (channel == null) {
						channel = getChannel(channelNumber, liveShiftApplication.getChannelSet());

						if (channel == null) {
							Thread.sleep(250);
							liveShiftApplication.refreshAndGetChannelSet();
						}
					}
					long currentTimeMillis = Clock.getMainClock().getTimeInMillis(false);
					long timeshift = peerBehavior.getTimeShiftRange(channel.getStartupTimeMillis() + 1000, currentTimeMillis);

					logger.info("tuning to channel [C" + channelNumber + "] at [" + timeshift + "] ms (ts:" + (currentTimeMillis - timeshift)
							+ " ms) and holding for [" + holdingTimeS + "] s");

					liveShiftApplication.switchChannelUtc(channel, timeshift);
				}

				waitAndCheckPlayback(liveShiftApplication, holdingTimeS);

			}
			catch (Exception e) {
				System.err.println("Error switching channel: " + e.getMessage());
				logger.error("Error switching channel: ", e);
				e.printStackTrace();

				try {
					Thread.sleep(500); // to give the machine a break
				}
				catch (InterruptedException e1) {
					logger.error("interrupted in the middle of the sleep");
					e1.printStackTrace();
				}
			}

			peerBehavior.next();
		}
	}

	/**
	 * Makes the application publish a new channel with given name
	 * 
	 * @param publishChannelName
	 */
	private void publishChannel(String publishChannelName, LiveShiftApplication liveShiftApplication, String multicastGroup) {
		logger.info("Publishing Channel " + publishChannelName);

		// Tell application to publish channel
		try {

			liveShiftApplication.publishChannelNetwork(publishChannelName, (byte)1, publishChannelName + " channel", multicastGroup);
			//TODO ^^ as desired, specify number of substreams in command line so it doesn't use a hardcoded 1

		}
		catch (Exception e) {
			logger.error("Could not publish channel", e);
		}
	}

	private Channel getChannel(int channelNumber, ChannelSet channels) throws InterruptedException {

		for (Channel channel : channels.getChannelIdMap().values())
			if (channel.getName().equals("C" + channelNumber))
				return channel;

		return null;
	}

	/**
	 * Waits for a given amount of seconds
	 * 
	 * @param liveShiftApplication2
	 * 
	 * @param seconds
	 */
	private void waitAndCheckPlayback(final LiveShiftApplication liveShiftApplication2, final long seconds) {
		logger.info("Waiting for " + seconds + " seconds, checking playback.");
		try {
			long startTime = Clock.getMainClock().getTimeInMillis(false);
			while (startTime + seconds * 1000 > Clock.getMainClock().getTimeInMillis(false)) {
				Thread.sleep(1000);
				if (liveShiftApplication.isPlaybackFailed()) {
					logger.info("Playback failed. ");
					return;
				}
			}

		}
		catch (InterruptedException e) {
			if (logger.isDebugEnabled())
				logger.debug("interrupted");
		}
	}

	/**
	 * Shuts the application down
	 */
	private void quitApplication(int code, LiveShiftApplication liveShiftApplication) {
		logger.info("Shutting down application");

		// Disconnect
		try {
			liveShiftApplication.shutdown();
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		// Kill Application
		if (this.isStatic)
			LiveShiftApplication.quit("Shutting down application");
		else
			this.isDone = true;
	}

	/**
	 * Prints the usage help
	 * 
	 * @param options
	 *            the options the command line parser will extract
	 */
	private void printUsage() {
		System.out.println("");
		System.out.println("LiveShift Command Line Interface");
		System.out.println("Copyright (C) 2012 CGG@IFI Fabio Hecht et al.");
		System.out.println("");
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("java " + CommandLineInterface.class.getSimpleName(), lvOptions);
	}

	/**
	 * Stopps the application with an error message
	 * 
	 * @param errorMessage
	 */
	private void quitError(String errorMessage) {
		System.err.println(errorMessage);
		System.err.println("Use Option --help for usage details.");
		logger.error(errorMessage);

		if (this.isStatic)
			LiveShiftApplication.quit(errorMessage);
		else
			this.isDone = true;
	}

	public boolean isDone() {
		return this.isDone;
	}

	/**
	 * Switches into given channel
	 * 
	 * @param channel
	 */
	private boolean tuneIntoChannel(Channel channel, LiveShiftApplication liveShiftApplication) {
		logger.info("Tuning into Channel " + channel);

		// Make application switch to channel
		try {
			liveShiftApplication.switchChannelLocalTz(channel, 0);
			firstChannelSwitchTime = Clock.getMainClock().getTimeInMillis(false);
			return true;
		}
		catch (Exception e) {
			logger.error("Error tuning into channel " + channel, e);
			return false;
		}
	}

	private void tuneIntoChannel(Channel channel, String timeString, LiveShiftApplication liveShiftApplication) {
		logger.info("Switching to channel time " + timeString);

		long time = Long.valueOf(timeString);
		long absoluteTimeMS = 0;
		// relative time
		if (time < 0) {
			absoluteTimeMS = Clock.getMainClock().getTimeInMillis(false) + time * 1000;
		}
		else { // abs time
			absoluteTimeMS = time * 1000;
		}

		logger.info("Switching to channel " + channel + " abs time " + absoluteTimeMS);

		// Make application switch to channel
		// Channel channel = application.getTuner().getChannel();
		timeshift(channel, absoluteTimeMS, liveShiftApplication);
	}

	/**
	 * Makes the application tune into a channel given by its name
	 * 
	 * @param channelName
	 */
	private void tuneIntoChannel(String channelName, LiveShiftApplication liveShiftApplication) {
		logger.info("Tuning into Channel with name " + channelName);

		// Refresh channel list

		// Get all channels
		ChannelSet channels = liveShiftApplication.refreshAndGetChannelSet();

		// Check if channel we want to tune into was found in channel list
		Channel channel = null;
		String foundChannels = "";
		for (Channel candidateChannel : channels.getChannelIdMap().values()) {
			String channelNameFound = candidateChannel.getName();
			foundChannels += channelNameFound + ", ";
			if (channelNameFound.equals(channelName)) {
				channel = candidateChannel;
				break;
			}
		}
		logger.info("Found " + channels.getChannelIdMap().size() + " Channels: " + foundChannels);

		// Channel was found
		if (channel != null) {
			logger.info("Channel " + channel + " was found.");
			tuneIntoChannel(channel, liveShiftApplication);
		}
		// Channel was not found
		else {
			System.err.println("Given channel '" + channelName + "' was not found. Stopping LiveShift now.");
			logger.error("Given channel '" + channelName + "' was not found. Stopping LiveShift now.");
			quitApplication(1, liveShiftApplication);
		}
	}

	/**
	 * Timeshifts a given amount of seconds back on a given channel
	 * 
	 * @param channel
	 * @param secondsBack
	 * @return
	 */
	private boolean timeshift(Channel channel, long timeshiftToMS, LiveShiftApplication liveShiftApplication) {
		logger.info("Timeshifting to " + timeshiftToMS + " on Channel " + channel + ".");
		try {
			liveShiftApplication.switchChannelLocalTz(channel, timeshiftToMS);
			return true;
		}
		catch (Exception e) {
			logger.error("Error tuning into channel " + channel, e);
			return false;
		}
	}
}
