/*

 * 
 * Copyright (c) 2008, CSG@IFI
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the organization nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY CSG@IFI ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL CSG@IFI BE LIABLE FOR ANY
 *  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package net.liveshift.core;

import java.awt.Canvas;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.MemoryHandler;

import net.liveshift.configuration.Configuration;
import net.liveshift.configuration.PublishConfiguration;
import net.liveshift.configuration.Configuration.EncoderName;
import net.liveshift.core.LiveShiftApplication.PlaybackStatus;
import net.liveshift.download.Tuner;
import net.liveshift.encoder.DummyEncoder;
import net.liveshift.encoder.Encoder;
import net.liveshift.encoder.ExtVLCEncoder;
import net.liveshift.encoder.VLCJEncoder;
import net.liveshift.incentive.IncentiveMechanism;
import net.liveshift.logging.LiveShiftMemoryHandler;
import net.liveshift.logging.LogClient;
import net.liveshift.p2p.DHTInterface;
import net.liveshift.p2p.DirectMessageSender;
import net.liveshift.p2p.Peer;
import net.liveshift.player.DummyPlayer;
import net.liveshift.player.ExtVLCPlayer;
import net.liveshift.player.Player;
import net.liveshift.player.VLCJPlayer;
import net.liveshift.signaling.DefaultIncomingMessageHandler;
import net.liveshift.signaling.IncomingMessageHandler;
import net.liveshift.signaling.MessageListener;
import net.liveshift.signaling.MessageSender;
import net.liveshift.signaling.PshIncomingMessageHandler;
import net.liveshift.signaling.PshVideoSignaling;
import net.liveshift.signaling.SducIncomingMessageHandler;
import net.liveshift.signaling.SducVideoSignaling;
import net.liveshift.signaling.VideoSignaling;
import net.liveshift.signaling.messaging.MessageFactory;
import net.liveshift.storage.SegmentAssembler;
import net.liveshift.storage.SegmentStorage;
import net.liveshift.time.Clock;
import net.liveshift.util.ExecutorPool;
import net.liveshift.util.Utils;
import net.liveshift.video.EncoderReceiver;
import net.liveshift.video.UdpEncoderReceiver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.sun.jna.NativeLibrary;

/**
 * @author Fabio Victora Hecht, draft
 */

public class LiveShiftApplication {
	
	final private static Logger logger = LoggerFactory.getLogger(LiveShiftApplication.class);

	public enum ConnectionState {
		CONNECTED, DISCONNECTED, CONNECTING
	};

	public enum PlayerStatus {
		PLAYING, PAUSED, STOPPED
	};

	public enum PlaybackStatus {
		NOT_PLAYING, PLAYING, STALLED
	};

	private Configuration configuration;

	private long playTimeMs = -1;

	private DHTInterface dht;
	private Encoder encoder;
	private Player player;
	private ConnectionState connectionState = ConnectionState.DISCONNECTED;

	private PlayerStatus playerStatus = PlayerStatus.STOPPED;
	private PlaybackStatus playbackStatus = PlaybackStatus.NOT_PLAYING;
	private boolean publishState = false;
	private VideoSignaling videoSignaling;
	private EncoderReceiver encoderReceiver;
	private SegmentStorage segmentStorage;
	private SegmentAssembler segmentAssembler;
	private Tuner tuner;

	private PublishConfiguration publishConfiguration;

	private Set<UiListener> uiListeners;

	private Stats stats;

	private Channel encoderChannel;
	private Canvas videoCanvas;

	private IncentiveMechanism incentiveMechanism;

	private LogClient logClient;
	private LiveShiftMemoryHandler usageLogger;

	static {
		try {
			if (System.getProperty("os.name").contains("Windows")) {
				LogManager.getLogManager().readConfiguration(LiveShiftApplication.class.getResourceAsStream("/jdklog_windows.properties"));
				new File("c:/temp/liveshift").mkdir();
				new File("c:/temp/liveshift/log").mkdir();
			}
			else {
				LogManager.getLogManager().readConfiguration(LiveShiftApplication.class.getResourceAsStream("/jdklog.properties"));
				new File("/tmp/LiveShift").mkdir();
				new File("/tmp/LiveShift/log").mkdir();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	public LiveShiftApplication() {
		this(new Configuration());
	}
	
	public LiveShiftApplication(Configuration configuration) {


		Runtime.getRuntime().addShutdownHook(new Thread() {
		      public void run() {
		    	  shutdown();
		      }
		    });
		
		// configures
		if (this.publishConfiguration == null)
			this.publishConfiguration = new PublishConfiguration();
		
		this.configuration = configuration;


		this.uiListeners = new HashSet<UiListener>();

		// windows libvlc fix
		// TODO: needs to be changeable by user / config
		if (System.getProperty("os.name").contains("Windows")) {
			NativeLibrary.addSearchPath("libvlc", "c:\\program files\\videolan\\vlc\\");
		}
		
	}

	public Configuration getConfiguration() {
		return this.configuration;
	}

	public void addStatListener(Stats listener) {
		this.stats = listener;
	}

	public void removeStatListener() {
		this.stats = null;
	}

	public Stats getStats() {
		if (this.stats == null)
			this.stats = new DummyStats();
		return this.stats;
	}

	public PublishConfiguration getPublishConfiguration() {
		return publishConfiguration;
	}

	public void setPublishTabBean(PublishConfiguration publishTabBean) {
		this.publishConfiguration = publishTabBean;
	}

	public boolean connect(String bootstrapAddress, String publicIpAddress) throws PlayerInitializationException, DHTConnectionException {
		
		// if reconnecting
		if (this.connectionState == ConnectionState.CONNECTED || this.connectionState == ConnectionState.CONNECTING)
			this.disconnect();

		// parses the bootstrapAddress (host:port)
		int colonPos, remotePort;
		InetSocketAddress bootstrapSocketAddress = null;
		if (!bootstrapAddress.equals("")) {
			
			String host;
			if (-1 != (colonPos = bootstrapAddress.lastIndexOf(':'))) {
				try {
					remotePort = Integer.parseInt(bootstrapAddress.substring(colonPos + 1));
					host = bootstrapAddress.substring(0, colonPos);
				}
				catch (NumberFormatException e) {
					remotePort = this.configuration.getP2pPort();
					host = bootstrapAddress;
				}
			}
			else {
				remotePort = this.configuration.getP2pPort();
				host = bootstrapAddress;
			}
			bootstrapSocketAddress = new InetSocketAddress(host, remotePort);
		}
		
		this.setConnectionState(ConnectionState.CONNECTING);

		boolean success = false;
		try {
			InetAddress publicIpInetAddress = publicIpAddress == null ? null : InetAddress.getByName(publicIpAddress);

			success = this.connect(bootstrapSocketAddress, publicIpInetAddress);
		}
		catch (PlayerInitializationException e) {
			success = false;
			
			throw e;
		}
		catch (DHTConnectionException e) {
			success = false;

			throw e;
		}
		catch (Exception e) {
			success = false;
			e.printStackTrace();
			logger.warn("Error connecting: " + e.getMessage());
		}
		finally {		
			if (success) {
				this.setConnectionState(ConnectionState.CONNECTED);
			}
			else {
				this.setConnectionState(ConnectionState.DISCONNECTED);
			}
		}
		
		return success;
	}

	private boolean connect(final InetSocketAddress bootstrapSocketAddress, final InetAddress publicIpAddress) throws PlayerInitializationException, DHTConnectionException {

		synchronized (this.connectionState) {

			// syncs clock
			if (this.configuration.isNtpEnabled()) {
				try {
					Clock.getMainClock().syncNtp(this.configuration.getNtpServer());
				}
				catch (IOException e) {
					logger.warn("Clock sync failed: clock is out of sync");
					// System.exit(1);
				}
			}
			else
				logger.warn("Clock syncing disabled, make sure the system clock of peers are synced, otherwise it ain't gonna work.");

			logger.info("I'm called " + this.configuration.getPeerName());

			//measures available upload bandwidth sends usage data
			if (this.logClient==null) {
				this.initLogClient();
				
				if (this.configuration.getUploadRate()==null) {
					this.configuration.setUploadRate(testUploadRateKbytePerSecond());
				}
					 
			}
			
			// creates objects
			if (this.dht==null) {
				Peer peer = new Peer();
				setDht(peer);
			}
			setSegmentAssembler(new SegmentAssembler());
			setSegmentStorage(new SegmentStorage(this.getDht(), this.configuration.getStorageDir()));

			//new player
			switch (this.configuration.getPlayerName()) {
				case ExtVlc:
					setPlayer(new ExtVLCPlayer(this.configuration.getVlcPath(), this.configuration.getPlayerPort()));
					break;
				case Vlcj:
					setPlayer(new VLCJPlayer(this.configuration.getFoundVlcLibPaths()));
					break;
				case Dummy:
					setPlayer(new DummyPlayer());
			}
			
			// boostraps on DHT
			if (logger.isDebugEnabled())
				logger.debug("Connect to " + bootstrapSocketAddress + ", local port " + this.configuration.getP2pPort() + ", iface "
						+ this.configuration.getNetworkInterface());
			boolean connected = false;
			try {
				if (publicIpAddress == null)
					connected = getDht().connect(new InetSocketAddress(InetAddress.getLocalHost(), this.configuration.getP2pPort()),
							bootstrapSocketAddress, this.configuration.getNetworkInterface(),
							this.configuration.getPeerName(), this.configuration.isUpnpEnabled());
				else
					connected = getDht().connect(new InetSocketAddress(InetAddress.getLocalHost(), this.configuration.getP2pPort()),
							bootstrapSocketAddress, this.configuration.getNetworkInterface(),
							this.configuration.getPeerName(), publicIpAddress, this.configuration.getP2pPort(), this.configuration.isUpnpEnabled());
			}
			catch (DHTConnectionException e) {
				logger.error("Error connecting: " + e.getMessage());
				e.printStackTrace();
				throw e;
			} catch (UnknownHostException e) {
				logger.error("UnknownHostException @ InetAddress.getLocalHost()");
				throw new DHTConnectionException(e);
			}
			
			if (logger.isDebugEnabled())
				logger.debug("Connect to " + bootstrapSocketAddress + ", local port " + this.configuration.getP2pPort() + " "
						+ (connected ? "successful" : "failed"));

			if (!connected) {
				logger.warn("Problem connecting.");

				return false;
			}

			// sets incentive mechanism and creates more objects
			this.incentiveMechanism = new IncentiveMechanism(this.getStats());
			MessageFactory messageFactory = new MessageFactory();

			DirectMessageSender directMessageSender = new DirectMessageSender(getDht().getMyId());
			MessageSender messageSender = directMessageSender;
			MessageListener messageListener = directMessageSender;

			switch (this.configuration.getIncentiveMechanismType()) {
			case PSH:
				PshVideoSignaling pshVideoSignaling = new PshVideoSignaling(messageSender, messageListener, this.segmentStorage, this.dht.getMyId(),
						this.incentiveMechanism, this.configuration, messageFactory);
				this.videoSignaling = pshVideoSignaling;
				this.incentiveMechanism.setPsh(pshVideoSignaling);

				break;

			case TFT:
				this.videoSignaling = new VideoSignaling(messageSender, messageListener, this.segmentStorage, this.dht.getMyId(), this.incentiveMechanism,
						this.configuration, messageFactory);
				this.incentiveMechanism.setTft();
				break;

			case RANDOM:
				this.videoSignaling = new VideoSignaling(messageSender, messageListener, this.segmentStorage, this.dht.getMyId(), this.incentiveMechanism,
						this.configuration, messageFactory);
				this.incentiveMechanism.setRandom();
				break;

			case SDUC:
				this.videoSignaling = new SducVideoSignaling(messageSender, messageListener, this.segmentStorage, this.dht.getMyId(), this.incentiveMechanism,
						this.configuration, messageFactory);
				this.incentiveMechanism.setSduc();
				break;

			default:
				this.videoSignaling = new VideoSignaling(messageSender, messageListener, this.segmentStorage, this.dht.getMyId(), this.incentiveMechanism,
						this.configuration, messageFactory);
				this.incentiveMechanism.setFcfs();
			}

			this.videoSignaling.setStats(this.getStats());
			this.incentiveMechanism.setIncentiveMechanism(this.configuration.getIncentiveMechanismType());
			this.incentiveMechanism.startBackgroundProcesses();

			// creates tuner
			this.tuner = new Tuner(this);
			messageFactory.registerChannelList(this.tuner.getChannelIdMap());
			this.segmentStorage.registerTuner(this.tuner);
			this.videoSignaling.registerNeighborList(this.tuner.getNeighborList());

			// starts listening to incoming messages
			IncomingMessageHandler incomingMessageHandler = null;
			switch (this.configuration.getIncentiveMechanismType()) {
			case PSH:
				incomingMessageHandler = new PshIncomingMessageHandler(this.tuner, (PshVideoSignaling) this.videoSignaling,
						this.incentiveMechanism.getPshHistory(), this.dht.getMyId(), this.tuner.getNeighborList(), this.configuration.freeride,
						this.tuner.getChannelIdMap());

				this.incentiveMechanism.getPshHistory().setNeighborList(this.tuner.getNeighborList());

				break;
			case SDUC:
				incomingMessageHandler = new SducIncomingMessageHandler(this.tuner, this.videoSignaling, this.configuration.freeride, this.incentiveMechanism,
						this.tuner.getChannelIdMap());
				break;

			default:
				incomingMessageHandler = new DefaultIncomingMessageHandler(this.tuner, this.videoSignaling, this.configuration.freeride);
			}

			this.videoSignaling.startListening(incomingMessageHandler);

			// publishes all segments in a new thread
			try {
				// reads channel list
				if (logger.isDebugEnabled())
					logger.debug("ChannelList: try to fetch...");

				this.refreshAndGetChannelSet();

				// writes all available segments from the segmentstorage on the
				// dht
				Thread runner = new Thread() {
					@Override
					public void run() {
						getSegmentStorage().announceAllOnDht();
					}
				};
				runner.start();
			}
			catch (Exception e) {
				e.printStackTrace();
			}

			// adds peerofflinelistener
			this.dht.addPeerOfflineListener(this.tuner, this.videoSignaling);

		}

		// main.updateStatus();

		return true;
	}


	private void initLogClient() {
		
		if (this.logClient!=null) {
			this.usageLogger.close();
		}
		this.logClient = new LogClient(this.configuration.getLogServerHost(), this.configuration.getLogServerPort(), Utils.getRandomString(20));
		
		if (this.configuration.isAllowDataCollection()) {
			
			this.usageLogger = new LiveShiftMemoryHandler(this.configuration.getLogIntervalMillis(), this.logClient);
	
			java.util.logging.Logger liveshiftLogger = java.util.logging.Logger.getLogger("net.liveshift");
			MemoryHandler memoryHandler = new MemoryHandler(this.usageLogger, 50, Level.INFO);
			liveshiftLogger.addHandler(memoryHandler);
		}
	}
	
	public boolean disconnect() {
		synchronized (this.connectionState) {

			if (this.connectionState != ConnectionState.DISCONNECTED) {

				if (logger.isDebugEnabled())
					logger.debug("Disconnecting");

				// UsageLogger.getInstance().logEvent(new
				// UsageEvent(UsageEventType.CLOSE_CONNECTION,
				// getDht().hashCode(), null));

				this.stopVideo();

				// kills connections and gets out of dht
				if (this.publishConfiguration != null)
					this.publishConfiguration.setPublished(false);

				this.killEncoder();

				if (this.videoSignaling != null)
					this.videoSignaling.disconnect();
				if (this.incentiveMechanism != null)
					this.incentiveMechanism.shutdown();

				if (this.segmentStorage != null)
					this.segmentStorage.shutdown();

				try {
					ExecutorPool.getGeneralExecutorService().awaitTermination(5, TimeUnit.SECONDS);
				}
				catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				try {
					if (this.getDht() != null)
						this.getDht().disconnect();
				}
				catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				//sends remaining usage data and shuts down
				if (this.usageLogger != null) {
					this.usageLogger.close();
				}

				this.setConnectionState(ConnectionState.DISCONNECTED);

			}
		}

		// main.updateStatus();

		return true;
	}

	/**
	 * gets all channels (forces getting new list from DHT)
	 * 
	 * @return
	 */
	public ChannelSet refreshAndGetChannelSet() {

		if (this.connectionState != ConnectionState.CONNECTED) {
			return null;
		}

		Set<Channel> channelSet = null;

		/*
		// adds some fake channels for testing
		channelSet = new HashSet<Channel>();
		int i = 0;
		Channel testChannel = new Channel("Action movies", (byte) 1, ++i);
		testChannel.setDescription("test channel #" + i);
		channelSet.add(testChannel);
		testChannel = new Channel("Soccer", (byte) 1, ++i);
		testChannel.setDescription("test channel #" + i);
		channelSet.add(testChannel);
		testChannel = new Channel("Olympics", (byte) 1, ++i);
		testChannel.setDescription("test channel #" + i);
		channelSet.add(testChannel);
		testChannel = new Channel("Champions League", (byte) 1, ++i);
		testChannel.setDescription("soccer");
		channelSet.add(testChannel);
		testChannel = new Channel("Formula 1 Championship", (byte) 1, ++i);
		testChannel.setDescription("soccer");
		channelSet.add(testChannel);
		testChannel = new Channel("Breaking news", (byte) 1, ++i);
		channelSet.add(testChannel);
		testChannel = new Channel("TV shows", (byte) 1, ++i);
		testChannel.setDescription("Breaking bad");
		channelSet.add(testChannel);
		for (Channel channel : channelSet)
			this.tuner.addChannel(channel);
		 */
		// queries DHT
		try {
			channelSet = this.getDht().getChannelSet();
			this.tuner.clearChannelSet();
			for (Channel channel : channelSet)
				this.tuner.addChannel(channel);
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

		if (logger.isDebugEnabled()) {
			StringBuilder sb = new StringBuilder("ChannelList: got ");
			sb.append(channelSet.size());
			sb.append(". Channels: (");
			for (Channel channel : channelSet) {
				sb.append(channel.getName());
				sb.append(",");
			}
			sb.append(")");
			logger.debug(sb.toString());
		}

		return this.tuner.getChannelSet();
	}

	public Channel getChannel(int channelId) {
		Channel channel = this.tuner.getChannelById(channelId);
		if (channel == null) {
			this.refreshAndGetChannelSet(); // maybe it's a new one
			channel = this.tuner.getChannelById(channelId);
		}
		return channel;
	}

	/**
	 * gets all channels (cached if possible)
	 * 
	 * @return
	 */
	public ChannelSet getChannelSet() {
		ChannelSet channelSet = this.tuner.getChannelSet();
		if (channelSet == null)
			this.refreshAndGetChannelSet();
		return channelSet;
	}

	public Collection<Channel> getChannelsByTags(String[] strings) {
		if (this.tuner==null) {
			return null;
		}
		Collection<Channel> channels = this.tuner.getChannelsByTags(strings);
		if (channels == null || channels.isEmpty())
			this.refreshAndGetChannelSet();
		return channels;
	}

	public void playVideo() {
		this.getPlayer().setCanvas(videoCanvas);
		this.getPlayer().initializeLocalStream(this.configuration.getLocalIpAddress(), this.configuration.getPlayerPort(), this.configuration.getPlayerProtocol());
		this.getPlayer().play();
		this.setPlayerStatus(PlayerStatus.PLAYING);
	}

	public void pauseVideo() {
		// we will keep receiving video but pause playback
		if (this.getPlayerStatus() == PlayerStatus.PLAYING) {
			this.getPlayer().pausePlayer();
			this.getTuner().getVideoPlayer().setPaused(true);
			this.setPlayerStatus(PlayerStatus.PAUSED);
			logger.info("player paused");
		}
		else if (this.getPlayerStatus() == PlayerStatus.PAUSED) {
			this.getTuner().getVideoPlayer().setPaused(false);
			this.getPlayer().play();
			this.setPlayerStatus(PlayerStatus.PLAYING);
			logger.info("player play");
		}
	}

	public void stopVideo() {

		this.stopPlayer();

		this.setPlayerStatus(PlayerStatus.STOPPED);
		this.setPlaybackStatus(PlaybackStatus.NOT_PLAYING);

		if (this.tuner != null)
			this.tuner.disconnect(true);

	}

	private void stopPlayer() {
		// stops video player
		try {
			if (this.getPlayer() != null) {
				this.getPlayer().stopPlayer();
				this.getPlayer().shutdown();
			}
		}
		catch (RuntimeException e) {
			e.printStackTrace();
		}
	}
	
	private void restartEncoder() {
		
		// kills current encoder, if any
		this.killEncoder();

		// instantiates an encoder
		if (this.configuration.getEncoderName()==EncoderName.Dummy) {
			this.setEncoder(new DummyEncoder());
		}
		else if (this.configuration.getEncoderName()==EncoderName.ExtVlc_visible) {
			this.setEncoder(new ExtVLCEncoder(getPublishConfiguration().getVlcParameters(), this.configuration.getVlcPath(), false));
		}
		else if (this.configuration.getEncoderName()==EncoderName.ExtVlc_hidden) {
			this.setEncoder(new ExtVLCEncoder(getPublishConfiguration().getVlcParameters(), this.configuration.getVlcPath(), true));
		}
		else if (this.configuration.getEncoderName()==EncoderName.Vlcj) {
			this.setEncoder(new VLCJEncoder(getPublishConfiguration().getVlcParameters(), this.configuration.getFoundVlcLibPaths()));
		}
	}
	
	private void runEncoderFile(String filename) {
		
		this.restartEncoder();
		
		// starts encoder
		this.getEncoder().createNewStreamingSourceFromFile(filename, "127.0.0.1", this.configuration.getEncoderPort(), this.configuration.getEncoderProtocol());
		this.getEncoder().startEncoding();
	}

	private void runEncoderDevice(String deviceName) {

		this.restartEncoder();
		
		// starts encoder
		this.getEncoder().createNewStreamingSourceFromDevice(deviceName, "127.0.0.1", this.configuration.getEncoderPort(), this.configuration.getEncoderProtocol());
		this.getEncoder().startEncoding();
	}
	
	public void publishChannelDevice(String channelName, byte numSubstreams, String description, String deviceName) {
		runEncoderDevice(deviceName);
		this.publishChannel(channelName, numSubstreams, description, "");
	}
	
	public void publishChannelFile(String channelName, byte numSubstreams, String description, String fileName) {
		this.runEncoderFile(fileName);
		this.publishChannel(channelName, numSubstreams, description, "");
	}

	public void publishChannelNetwork(String channelName, Byte numSubstreams, String description, String networkSource) {
		this.publishChannel(channelName, numSubstreams, description, networkSource);
	}
	private void publishChannel(String channelName, byte numSubstreams, String description, String multicastGroup) {
		if (this.connectionState != ConnectionState.CONNECTED) {
			return;
		}

		// creates channel object
		Channel newChannel = new Channel(channelName, numSubstreams, Utils.getRandomInt());

		newChannel.setDescription(description);
		newChannel.setOwnChannel();

		setEncoderChannel(newChannel);

		this.tuner.addChannel(newChannel);

		// creates channel on DHT
		try {
			this.getDht().publishChannel(newChannel);
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// needs to create a SegmentAssembler
		SegmentAssembler encoderAssembler = new SegmentAssembler();

		encoderAssembler.initialize(newChannel, this.segmentStorage, this.videoSignaling.getUploadSlotManager());

		// creates and starts the packet sequencer
		EncoderReceiver encoderReceiver = null;
		if (multicastGroup==null) {
			encoderReceiver = new UdpEncoderReceiver(newChannel, encoderAssembler, this.configuration.getEncoderPort());
		}
		else {
			encoderReceiver = new UdpEncoderReceiver(newChannel, encoderAssembler, this.configuration.getEncoderPort(), multicastGroup);
		}
		this.setEncoderReceiver(encoderReceiver);
																																				// multicastGroup
		encoderReceiver.startEncoding();

		// updates (G)UI
		if (this.publishConfiguration != null)
			this.publishConfiguration.setPublished(true);
		
		this.publishState  = true;
		this.notifyUiListenersPublishState();
	}

	public void killEncoder() {
		// kills connections
		// stops streaming
		// since the tuner must be tuned to its encoder

		// if (getEncoderChannel() != null)
		// UsageLogger.getInstance().logEvent(new
		// UsageEvent(UsageEventType.UNPUBLISH_CHANNEL,getEncoderChannel().getID(),
		// getEncoderChannel()));


		if (this.getEncoder() != null)
			this.getEncoder().shutdown();

		this.setEncoder(null);

		// kills video sequencer
		if (null != this.getEncoderReceiver()) {
			this.getEncoderReceiver().kill();
		}
		this.setEncoderReceiver(null);
		setEncoderChannel(null);

		if (this.publishConfiguration != null)
			this.publishConfiguration.setPublished(false);
		
		this.publishState = false;
		this.notifyUiListenersPublishState();

	}

	public Player getPlayer() {
		return this.player;
	}

	public void setPlayer(Player player) {
		this.player = player;
	}

	public Tuner getTuner() {
		return tuner;
	}

	private void setSegmentStorage(SegmentStorage segmentStorage) {
		this.segmentStorage = segmentStorage;
	}

	public SegmentStorage getSegmentStorage() {
		return segmentStorage;
	}

	public void setEncoder(Encoder encoder) {
		this.encoder = encoder;
	}

	public Encoder getEncoder() {
		return encoder;
	}

	private void setConnectionState(ConnectionState connectionState) {
		this.connectionState = connectionState;
		this.notifyUiListenersConnectionState();
	}

	public ConnectionState getConnectionState() {
		return connectionState;
	}

	public void shutdown() {
		this.disconnect();
		this.configuration.saveUserConfiguration();
	}

	public void setDht(DHTInterface dht) {
		this.dht = dht;
	}

	public DHTInterface getDht() {
		return dht;
	}

	public VideoSignaling getVideoSignaling() {
		return videoSignaling;
	}

	private void setSegmentAssembler(SegmentAssembler segmentAssembler) {
		this.segmentAssembler = segmentAssembler;
	}

	public SegmentAssembler getSegmentAssembler() {
		return segmentAssembler;
	}

	public void setEncoderReceiver(EncoderReceiver encoderReceiver) {
		this.encoderReceiver = encoderReceiver;
	}

	public EncoderReceiver getEncoderReceiver() {
		return encoderReceiver;
	}

	public void switchChannelUtc(Channel channel, long timeshiftMS) {

		if (channel == null) {
			logger.warn("Was trying to switch to a null channel.");

			return;
		}
		if (this.playerStatus == PlayerStatus.PLAYING || this.playerStatus == PlayerStatus.PAUSED) {
			this.stopPlayer(); // autostop
		}
		
		if (timeshiftMS == 0) {
			timeshiftMS = Clock.getMainClock().getTimeInMillis(false);
		}

		// switches on tuner
		this.getTuner().switchChannel(channel, timeshiftMS);

		// autoplay
		this.playVideo();

		notifyUiListenersPlayingState();
	}

	public void switchChannelLocalTz(Channel channel, long timeshiftMS) {

		// converts local time to UTC
		if (timeshiftMS != 0) {
			timeshiftMS -= Clock.getMainClock().getTimeZoneDifferenceSeconds()*1000;
		}
		
		this.switchChannelUtc(channel, timeshiftMS);
	}

	private void setPlayerStatus(PlayerStatus playerStatus) {
		this.playerStatus = playerStatus;
		this.notifyUiListenersPlayingState();
	}

	public PlayerStatus getPlayerStatus() {
		return playerStatus;
	}

	public void setPlaybackStatus(PlaybackStatus playbackStatus) {
		this.playbackStatus = playbackStatus;
		this.notifyUiListenersPlayingState();
	}

	public PlaybackStatus getPlaybackStatus() {
		return playbackStatus;
	}

	public void registerVideoCanvas(Canvas videoCanvas) {
		this.videoCanvas = videoCanvas;
		if (this.player!=null) {
			this.player.setCanvas(videoCanvas);
			
			//to change canvas while playing, it needs to stop and start again
			if (this.playerStatus==PlayerStatus.PLAYING) {
				this.player.stopPlayer();
				this.player.play();
			}
		}
	}

	public Canvas getVideoCanvas() {
		return this.videoCanvas;
	}

	private void setEncoderChannel(Channel encoderChannel) {
		this.encoderChannel = encoderChannel;
	}

	public Channel getEncoderChannel() {
		return encoderChannel;
	}

	/**
	 * sets last play time in seconds
	 * 
	 * @param playTimeMs
	 */
	public void setPlayTimeMs(long playTimeMs) {
		this.playTimeMs = playTimeMs;
	}

	/**
	 * gets last play time in seconds
	 * 
	 * @return the time, what else?
	 */
	public long getPlayTimeMillis() {
		return playTimeMs;
	}

	public IncentiveMechanism getIncentiveMechanism() {
		return this.incentiveMechanism;
	}

	public static void quit(String message) {
		System.err.println(message);
		System.exit(1);
	}

	public boolean isPlaybackFailed() {
		if (this.tuner == null)
			return false;
		else
			return this.tuner.isPlaybackFailed();
	}

	public void fastForward(int timeMillis) {
		if (this.tuner == null)
			return;
		this.tuner.fastForward(timeMillis);
	}

	public void rewind(int timeMillis) {
		if (this.tuner == null)
			return;

		this.tuner.rewind(timeMillis);
	}

	public void addUiListener(UiListener uiListener) {
		this.uiListeners.add(uiListener);
	}

	public void removeUiListener(UiListener uiListener) {
		this.uiListeners.remove(uiListener);
	}

	public void notifyUiListenersConnectionState() {
		for (UiListener listener : this.uiListeners) {
			listener.connectionStateChanged();
		}
	}

	public void notifyUiListenersPublishState() {
		for (UiListener listener : this.uiListeners) {
			listener.publishStateChanged(this.publishConfiguration);
		}
	}

	public void notifyUiListenersPlayingState() {
		Tuner tuner = this.tuner;

		if (tuner == null) {
			return;
		}

		for (UiListener listener : this.uiListeners) {
			listener.playingStateChanged(this.tuner.getChannel(), this.playerStatus, this.playbackStatus, this.tuner.getPlayTime());
		}
	}

	public void setVolumeUp(int amount) {
		this.player.setVolumeUp(amount);
		this.notifyVolumeChanged();
	}

	public void setVolumeDown(int amount) {
		this.player.setVolumeDown(amount);		
		this.notifyVolumeChanged();
	}

	public Channel getPlayingChannel() {
		if (this.tuner == null) {
			return null;
		}
		return this.tuner.getChannel();
	}

	public boolean getPublishState() {
		return this.publishState;
	}

	public void unpublishChannel() {
		if (encoderChannel!=null)
			this.dht.unPublishChannel(encoderChannel);
		this.killEncoder();
		this.refreshAndGetChannelSet();
	}
	
	public void notifyVolumeChanged() {
		for (UiListener listener : this.uiListeners) {
			listener.volumeChanged(this.player.getVolumePercent());
		}
	}

	public int testUploadRateKbytePerSecond() {
		try {
			if (this.logClient==null) {
				this.initLogClient();
			}
			return this.logClient.getUploadRateKbytePerSecond();
		}
		catch (Exception e) {
			logger.error("Can't measure upload rate: "+e.getMessage());
			return 0;
		}
	}
}
