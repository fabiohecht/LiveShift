package net.liveshift.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.co.caprica.vlcj.runtime.RuntimeUtil;

import net.liveshift.core.LiveShiftApplication;
import net.liveshift.encoder.DummyEncoder;
import net.liveshift.encoder.Encoder;
import net.liveshift.encoder.ExtVLCEncoder;
import net.liveshift.encoder.VLCJEncoder;
import net.liveshift.gui.LiveShiftGUI;
import net.liveshift.incentive.IncentiveMechanism.IncentiveMechanismType;
import net.liveshift.player.*;
import net.liveshift.util.Utils;
import net.liveshift.video.playbackpolicies.PlaybackPolicy.PlaybackPolicyType;
import net.liveshift.incentive.IncentiveMechanism.IncentiveMechanismType;
import net.liveshift.player.Player;
import net.liveshift.player.VLCJPlayer;

/**
 * Default LiveShift configuration
 *
 * @author Fabio Victora Hecht
 * @author Kevin Leopold
 *
 */
public class Configuration {
	
	public enum EncoderName {Dummy, ExtVlc_hidden, ExtVlc_visible, Vlcj}
	public enum PlayerName {Dummy, ExtVlc, Vlcj}
	public enum StreamingProtocol {RTP, UDP}
	
	private static final Logger logger = LoggerFactory.getLogger(Configuration.class);

	public  final static long SEGMENTBLOCK_SIZE_MS = 1000; //must fit perfectly into one segment! i.e. SEGMENT_SIZE_SECONDS%SEGMENTBLOCK_SIZE_SECONDS==0
	public  final static long SEGMENT_SIZE_MS = 600000;

	private IncentiveMechanismType incentiveMechanismType = IncentiveMechanismType.FCFS;
	private PlaybackPolicyType playbackPolicyType = PlaybackPolicyType.RATIO;
	private float playbackPolicyParameter = 2;
	private float playbackPolicyParameter2;
	private int playbackBuffering = 6;
	
	private EncoderName encoderName = EncoderName.ExtVlc_hidden;
	private PlayerName playerName = PlayerName.Vlcj;
	
	public boolean	noDynamicUploadSlotManagement = true;
	public boolean	vectorHavesEnabled = false;
	public boolean	neighborMaintenance = true;
	public boolean	noHaveSuppression = false;
	public int numSubscribers = 5;
	
	private Enumeration<NetworkInterface> interfaces;
	private String localIpAddress;

    private String bootstrapAddress = "bootstrap.liveshift.net:8080";
    private String networkInterface = "eth0";
    private Integer playerPort;
    private Integer encoderPort;
    private Integer p2pPort;
    private Integer uploadRate;
    private String ntpServer = "time.liveshift.net";
    private String peerName;
    private String storageDir;
    private String vlcPath;
    private String userVlcLibPath;
    private Set<String> foundVlcLibPaths = new HashSet<String>();
    private boolean autoConnectOnStartup;
    private boolean autoConnectSingleChannel;
    private boolean allowDataCollection;
    private String logServerHost;
    private int logServerPort;
    private String updateServer;
    public long logIntervalMillis = 30000;
	private boolean ntpEnabled = false;
	private boolean upnpEnabled;
	private StreamingProtocol playerProtocol = StreamingProtocol.RTP;
	private StreamingProtocol encoderProtocol = StreamingProtocol.RTP;
	private boolean firstTimeRun = true;

	public Configuration() {
		try {
			this.interfaces = NetworkInterface.getNetworkInterfaces();
			this.localIpAddress = InetAddress.getLocalHost().getHostAddress();
		}
		catch (Exception e) {
			e.printStackTrace();
			logger.error("Exception creating configuration: "+e.getMessage());
			LiveShiftApplication.quit("Exception creating configuration: "+e.getMessage());
		}

		loadUserConfiguration();
		
		//some defaults (more above on declarations)
		if (this.getEncoderPort()==null)
			this.setEncoderPort(randomBetween(11000,11999));
		if (this.getP2pPort()==null)
			this.setP2pPort(Integer.parseInt(this.getBootstrapAddress().split(":")[1]));
			//this.setP2pPort(randomBetween(10000,10999));
		if (this.getPeerName()==null)
			this.setPeerName(this.createDefaultPeerName());
		if (this.storageDir==null)
			this.storageDir = System.getProperty("java.io.tmpdir")+File.separatorChar+"LiveShift"+File.separatorChar+"storage-"+this.peerName;
		if (this.getPlayerPort()==null)
			this.setPlayerPort(randomBetween(12000,12999));
		if(this.getLogServerHost() == null)
			this.setLogServerHost("logging.liveshift.net");
		if(this.getLogServerPort() == 0)
			this.setLogServerPort(8888);
		if(this.getUpdateServer() == null)
			this.setUpdateServer("http://update.liveshift.net:8888");
	}
	

    private void loadUserConfiguration() {
        Properties config = new Properties();
    	String configurationFile = getConfigurationFileName();
        try {
            FileInputStream fis = new FileInputStream(configurationFile);
            config.load(fis);
            if (config.containsKey("BootstrapAddress")) {
                setBootstrapAddress(config.getProperty("BootstrapAddress"));
            }
            if (config.containsKey("NetworkInterface")) {
                setNetworkInterface(config.getProperty("NetworkInterface"));
            }
            if (config.containsKey("NtpServer")) {
                setNtpServer(config.getProperty("NtpServer"));
            }
            if (config.containsKey("PeerName")) {
                setPeerName(config.getProperty("PeerName"));
            }
            if (config.containsKey("UploadRate")) {
                setUploadRate(Integer.valueOf(config.getProperty("UploadRate")));
            }
            if (config.containsKey("StorageFolder")) {
                setStorageDir(config.getProperty("StorageFolder"));
            }
            if (config.containsKey("VlcPath")) {
                setVlcPath(config.getProperty("VlcPath"));
            }
            if (config.containsKey("UserVlcLibPath")) {
                setUserVlcLibPath(config.getProperty("UserVlcLibPath"));
            }
            if (config.containsKey("VlcCommand")) {
                // setVlcCommand(config.getProperty("VlcCommand"));
            }
            if (config.containsKey("AutoConnectOnStartup")) {
                setAutoConnectOnStartup(Boolean.parseBoolean(config.getProperty("AutoConnectOnStartup")));
            }
            if (config.containsKey("UpnpEnabled")) {
                setUpnpEnabled(Boolean.parseBoolean(config.getProperty("UpnpEnabled")));
            }
            if (config.containsKey("AllowDataCollection")) {
                setAllowDataCollection(Boolean.parseBoolean(config.getProperty("AllowDataCollection")));
            }
            if (config.containsKey("P2pPort")) {
                setP2pPort(Integer.parseInt(config.getProperty("P2pPort")));
            }
            if (config.containsKey("EncoderPort")) {
                setEncoderPort(Integer.parseInt(config.getProperty("EncoderPort")));
            }
            if (config.containsKey("PlayerPort")) {
            	setPlayerPort(Integer.parseInt(config.getProperty("PlayerPort")));
            }
            if (config.containsKey("PlayerProtocol")) {
                setPlayerProtocolName(config.getProperty("PlayerProtocol"));
            }
            if (config.containsKey("PlayerName")) {
                setPlayerName(config.getProperty("PlayerName"));
            }
            if (config.containsKey("EncoderName")) {
            	setEncoderName(config.getProperty("EncoderName"));
            }
            if (config.containsKey("FirstTimeRun")) {
                setFirstTimeRun(Boolean.parseBoolean(config.getProperty("FirstTimeRun")));
            }
            
            if (config.containsKey("LogServerHost")) {
                setLogServerHost(config.getProperty("LogServerHost"));
            }
            if (config.containsKey("LogServerPort")) {
                setLogServerPort(Integer.parseInt(config.getProperty("LogServerPort")));
            }
            if (config.containsKey("UpdateServer")) {
                setUpdateServer(config.getProperty("UpdateServer"));
            }
            if (config.containsKey("LogInterval")) {
                setLogIntervalMillis(Long.parseLong(config.getProperty("LogInterval")));
            }

        } catch (FileNotFoundException e) {
            logger.debug("Configuration file ("+configurationFile+") not found. use default values");
        } catch (IOException e) {
            logger.debug("Error loading configuration file ("+configurationFile+"). use default values");
        }
    }
	
	public void saveUserConfiguration() {
		Properties config = new Properties();
    	String configurationFile = getConfigurationFileName();

		if (getBootstrapAddress() != null) {
			config.setProperty("BootstrapAddress", getBootstrapAddress());
		}
		if (getNetworkInterface() != null) {
			config.setProperty("NetworkInterface", getNetworkInterface());
		}
		if (getNtpServer() != null) {
			config.setProperty("NtpServer", getNtpServer());
		}
		if (getPeerName() != null) {
			config.setProperty("PeerName", getPeerName());
		}
		if (getUploadRate() != null) {
			config.setProperty("UploadRate", Integer.toString(getUploadRate()));
		}
		if (getStorageDir() != null) {
			config.setProperty("StorageFolder", getStorageDir());
		}
		if (getVlcPath() != null) {
			config.setProperty("VlcPath", getVlcPath());
		}
		if (getUserVlcLibPath() != null) {
			config.setProperty("UserVlcLibPath", getUserVlcLibPath());
		}
		// if (getVlcCommand()!=null) {
		// config.setProperty("VlcCommand", getVlcCommand());
		// }

		config.setProperty("AutoConnectOnStartup", Boolean.toString(isAutoConnectOnStartup()));
		config.setProperty("AllowDataCollection", Boolean.toString(isAllowDataCollection()));
		config.setProperty("FirstTimeRun", Boolean.toString(isFirstTimeRun()));
		config.setProperty("UpnpEnabled", Boolean.toString(isUpnpEnabled()));

		if (getP2pPort() != null) {
			config.setProperty("P2pPort", Integer.toString(getP2pPort()));
		}
		if (getEncoderPort() != null) {
			config.setProperty("EncoderPort", Integer.toString(getEncoderPort()));
		}
		if (getPlayerPort() != null) {
			config.setProperty("PlayerPort", Integer.toString(getPlayerPort()));
		}
		if (getPlayerProtocol() != null) {
			config.setProperty("PlayerProtocol", getPlayerProtocol().name());
		}
		if (getLogServerHost() != null) {
			config.setProperty("LogServerHost", getLogServerHost());
		}
		if (getLogServerPort() != 0) {
			config.setProperty("LogServerPort", Integer.toString(getLogServerPort()));
		}		
		config.setProperty("LogInterval", Long.toString(getLogIntervalMillis()));
		        
		if (getUpdateServer() != null) {
			config.setProperty("UpdateServer", getUpdateServer());
		}
		if (getPlayerName().name() != null) {
			config.setProperty("PlayerName", getPlayerName().name());
		}
		if (getEncoderName().name() != null) {
			config.setProperty("EncoderName", getEncoderName().name());
		}

		try {
			File userCfgFile = new File(configurationFile);
			if (!userCfgFile.exists())
				userCfgFile.createNewFile();
			config.store(new FileOutputStream(configurationFile), null);
		} catch (FileNotFoundException e) {
			logger.error("configuration file " + configurationFile + " not found", e);
		} catch (IOException e) {
			logger.error("configuration file " + configurationFile + " i/o error", e);
		}
	}

	private static String getConfigurationFileName() {
		if (RuntimeUtil.isWindows()) {
			return CONFIGURATION_FILE_WINDOWS;
		}
		else if (RuntimeUtil.isNix()) {
			return CONFIGURATION_FILE_NIX;
		}
		else {
			throw new RuntimeException("Platform not supported. at the moment, only Windows and Linux are.");
		}
	}


	private int randomBetween(int a, int b) {
		return a+(int)(Math.random()*(b-a+1));
	}
	
	/**
	 * Defines whether this node should deny all requests or not
	 */
	public  boolean freeride = false;
	

	/**
	 * Defines whether an incoming request by a peer with higher reputation than other peer in the queue preemts the bad one
	 * 
	 */
	public  final boolean	uploadQueuePreempt	= true;
	
	public final int blockSchedulerReadAheadBlocks = 6;

	public PlayerName getPlayerName() {
		return this.playerName;
	}
	public void setPlayerName(PlayerName playerName) {
		this.playerName = playerName;
	}
	
	private static final String CONFIGURATION_FILE_NIX = ".liveshift";
	private static final String CONFIGURATION_FILE_WINDOWS = "liveshift.ini";

	public static final byte MAX_NUMBER_OF_SUBSTREAMS = 5;

	/**
	 * Rank Management
	 */
	public  String getStorageRankSavePath() {
		return this.getStorageDirectory() + "/order.bin";
	}
	
	private  final long DEFAULT_STORAGE_PROTECTION_TIME_MS = 20*60*1000; // 20 minutes protection
	private  Long STORAGE_PROTECTION_TIME_MS = null;
	public  void setStorageProtectionTimeMS(long timeMS) {
		STORAGE_PROTECTION_TIME_MS = timeMS;
	}
	public  long getStorageProtectionTimeMS() {
		if (STORAGE_PROTECTION_TIME_MS == null) {
			return DEFAULT_STORAGE_PROTECTION_TIME_MS;
		}
		return STORAGE_PROTECTION_TIME_MS;
	}
	
	private  final int DEFAULT_STORAGE_PROTECTED_INTERVAL_COUNT = 2;
	private  Integer STORAGE_PROTECTED_INTERVAL_COUNT = null;
	public  void setStorageProtectedIntervalCount(int count) {
		STORAGE_PROTECTED_INTERVAL_COUNT = count;
	}
	public  int getStorageProtectedIntervalCount() {
		if (STORAGE_PROTECTED_INTERVAL_COUNT == null) {
			return DEFAULT_STORAGE_PROTECTED_INTERVAL_COUNT;
		}
		return STORAGE_PROTECTED_INTERVAL_COUNT;
	}
	
	private  final long DEFAULT_STORAGE_LIMIT_MAX_MS = 24*60*60*1000; // 1 day of storage
	private  Long STORAGE_LIMIT_MAX_MS = null;
	public  void setStorageTimeLimitMS(long timeMS) {
		STORAGE_LIMIT_MAX_MS = timeMS;
	}
	public  long getStorageLimitMaxMS() {
		if (STORAGE_LIMIT_MAX_MS == null) {
			return DEFAULT_STORAGE_LIMIT_MAX_MS;
		}
		return STORAGE_LIMIT_MAX_MS;
	}
	
	private  final long DEFAULT_STORAGE_SIZE_LIMIT = 1024*1024*1024; // 1 GB
	public long getDefaultStorageSizeLimit() {
		return DEFAULT_STORAGE_SIZE_LIMIT;
	}

	private  Long STORAGE_SIZE_LIMIT = null;
	public  void setStorageSizeLimit(long size) {
		STORAGE_SIZE_LIMIT = size;
	}
	public  long getStorageSizeLimit() {
		if (STORAGE_SIZE_LIMIT == null) {
			return DEFAULT_STORAGE_SIZE_LIMIT;
		}
		return STORAGE_SIZE_LIMIT;
	}
	
	private  final long DEFAULT_STORAGE_AUTO_INTERVAL_SPAN_MS = SEGMENT_SIZE_MS; // removes whole segments
	private  Long STORAGE_AUTO_INTERVAL_SPAN_MS = null;


	public  void setStorageAutoIntervalSpanMS(long timeMS) {
		STORAGE_AUTO_INTERVAL_SPAN_MS = timeMS;
	}
	public  long getStorageAutoIntervalSpanMS() {
		if (STORAGE_AUTO_INTERVAL_SPAN_MS == null) {
			return DEFAULT_STORAGE_AUTO_INTERVAL_SPAN_MS;
		}
		return STORAGE_AUTO_INTERVAL_SPAN_MS;
	}

	public String getStorageDirectory() {
		return this.storageDir;
	}
	
	public int getRandomPort(){
		return (int) (10000 + Math.random() * 10000);
	}

	public String createDefaultPeerName() {
		try {
			return InetAddress.getLocalHost().getHostName()+"-"+this.randomBetween(0, 999);
		} catch (UnknownHostException e) {
			return "";
		}
	}
	
	public void setPlaybackPolicy(PlaybackPolicyType playbackPolicyType) {
		this.playbackPolicyType=playbackPolicyType;
	}
	public PlaybackPolicyType getPlaybackPolicy() {
		return this.playbackPolicyType;
	}
	
	public void setPlaybackPolicyParameter(float playbackPolicyParameter) {
		this.playbackPolicyParameter=playbackPolicyParameter;
	}
	public float getPlaybackPolicyParameter() {
		return this.playbackPolicyParameter;
	}

	public void setPlaybackPolicyParameter2(float playbackPolicyParameter2) {
		this.playbackPolicyParameter2=playbackPolicyParameter2;
	}
	public float getPlaybackPolicyParameter2() {
		return this.playbackPolicyParameter2;
	}

	public void setPlaybackBuffering(int playbackBuffering) {
		this.playbackBuffering=playbackBuffering;
	}
	public int getPlaybackBuffering() {
		return this.playbackBuffering;
	}

	public void setIncentiveMechanismType(final IncentiveMechanismType incentiveMechanismType) {
		this.incentiveMechanismType = incentiveMechanismType;
	}
	public IncentiveMechanismType getIncentiveMechanismType() {
		return incentiveMechanismType;
	}

	public EncoderName getEncoderName() {
		return this.encoderName;
	}
	public void setEncoderName(EncoderName encoderName) {
		this.encoderName = encoderName;
	}

    public boolean isAllowDataCollection() {
		return allowDataCollection;
	}

	public void setAllowDataCollection(boolean allowDataCollection) {
		this.allowDataCollection = allowDataCollection;
	}

    public String getBootstrapAddress()
    {
        return bootstrapAddress;
    }

    public void setBootstrapAddress(String bootstrapAddress)
    {
        this.bootstrapAddress = bootstrapAddress;
    }
	
    public String getNetworkInterface()
    {
        return networkInterface;
    }
    public void setNetworkInterface(String networkInterface)
    {
        this.networkInterface = networkInterface;
    }

    public Integer getPlayerPort()
    {
        return playerPort;
    }
    public void setPlayerPort(Integer playerPort)
    {
        this.playerPort = playerPort;
    }
	
    public Integer getEncoderPort()
    {
        return encoderPort;
    }

    /**
     * @param encoderPort the encoderPort to set
     */
    public void setEncoderPort(Integer encoderPort)
    {
        this.encoderPort = encoderPort;
    }

    /**
     * @return the p2pPort
     */
    public Integer getP2pPort()
    {
        return p2pPort;
    }

    /**
     * @param p2pPort the p2pPort to set
     */
    public void setP2pPort(Integer p2pPort)
    {
        this.p2pPort = p2pPort;
    }

    public Integer getUploadRate()
    {
        return uploadRate;
    }

    public void setUploadRate(int uploadRate)
    {
        this.uploadRate = uploadRate;
    }

    public String getNtpServer()
    {
        return ntpServer;
    }

    public void setNtpServer(String ntpServer)
    {
        this.ntpServer = ntpServer;
    }

	public void setPeerName(String peerName) {
        this.peerName = peerName;
	}

	public String getPeerName() {
		return peerName;
	}
	
	public void setStorageDir(String storageDir) {
		this.storageDir = storageDir;
	}
	
	public String getStorageDir() {
		return storageDir;
	}

	public void setVlcPath(String vlcPath) {
        this.vlcPath = vlcPath;
	}

	public String getVlcPath() {
		return vlcPath;
	}
	
	

//	public void setVlcCommand(String vlcCommand) {
//        propertySupport.firePropertyChange(PROP_VLC_COMMAND, this.vlcCommand, this.vlcCommand = vlcCommand);
//	}
//
//	public String getVlcCommand() {
//		return vlcCommand;
//	}

	public String getLogServerHost() {
		return logServerHost;
	}

	public void setLogServerHost(String logServerHost) {
		this.logServerHost = logServerHost;
	}
	
	public String getUpdateServer() {
		return updateServer;
	}

	public void setUpdateServer(String updateServer) {
		this.updateServer = updateServer;
	}

	public int getLogServerPort() {
		return logServerPort;
	}

	public void setLogServerPort(int logServerPort) {
		this.logServerPort = logServerPort;
	}
	
	public long getLogIntervalMillis() {
		return logIntervalMillis;
	}

	public void setLogIntervalMillis(long logInterval) {
		this.logIntervalMillis = logInterval;
	}

	public void setAutoConnectOnStartup(boolean autoConnectOnStartup) {
        this.autoConnectOnStartup = autoConnectOnStartup;
	}

	public boolean isAutoConnectOnStartup() {
		return autoConnectOnStartup;
	}
	
	public void setAutoConnectSingleChannel(boolean autoConnectSingleChannel) {
        this.autoConnectSingleChannel = autoConnectSingleChannel;
	}

	public boolean getAutoConnectSingleChannel() {
		return autoConnectSingleChannel;
	}

	private void setEncoderName(String encoderName) {
		for (EncoderName name : EncoderName.values()) {
			if (encoderName.equals(name.name())) {
				this.setEncoderName(name);
				break;
			}
		}
	}

	private void setPlayerName(String playerName) {
		for (PlayerName name : PlayerName.values()) {
			if (playerName.equals(name.name())) {
				this.setPlayerName(name);
				break;
			}
		}
	}
	
	private void setPlayerProtocolName(String playerProtocolName) {
		for (StreamingProtocol name : StreamingProtocol.values()) {
			if (playerProtocolName.equals(name.name())) {
				this.setPlayerProtocol(name);
				break;
			}
		}
	}

	public boolean isNtpEnabled() {
		return this.ntpEnabled;
	}

	public void setNtpEnabled(boolean ntpEnabled) {
		this.ntpEnabled = ntpEnabled;
	}

	public String getLocalIpAddress() {
		return this.localIpAddress;
	}

	public void setUpnpEnabled(boolean upnpEnabled) {
		this.upnpEnabled=upnpEnabled;
	}
	public boolean isUpnpEnabled() {
		return this.upnpEnabled;
	}

	public String getUserVlcLibPath() {
		return userVlcLibPath;
	}
	public void setUserVlcLibPath(String userVlcLibPath) {
		this.userVlcLibPath = userVlcLibPath;
	}
	
	public Set<String> getFoundVlcLibPaths() {
		return foundVlcLibPaths;
	}
	public void setFoundVlcLibPaths(Set<String> foundVlcLibPaths) {
		this.foundVlcLibPaths = foundVlcLibPaths;
	}

	public void setPlayerProtocol(StreamingProtocol playerProtocol) {
		this.playerProtocol=playerProtocol;
	}
	public StreamingProtocol getPlayerProtocol() {
		return this.playerProtocol;
	}

	public void setEncoderProtocol(StreamingProtocol encoderProtocol) {
		this.encoderProtocol=encoderProtocol;
	}
	public StreamingProtocol getEncoderProtocol() {
		return this.encoderProtocol;
	}

	public boolean isFirstTimeRun() {
		return this.firstTimeRun;
	}
	public void setFirstTimeRun(boolean firstTimeRun) {
		this.firstTimeRun=firstTimeRun;
	}

	public static boolean checkVlcLibs(String path) {
		Set<String> vlcLibNames = getVlcLibNames();

		boolean allFilesThere = true;
		for (String vlcLibName : vlcLibNames) {
			if (!new File(path + File.separator + vlcLibName).exists()) {
				allFilesThere = false;
			}
		}
		return allFilesThere;
	}
	
	public static Set<String> findVlcLibs(String suspect) {

		Set<String> suspects = new HashSet<String>();
		if (suspect!=null && !suspect.equals("")) {
			suspects.add(suspect);
		}
		suspects.add(new File(".").getAbsolutePath()+File.separatorChar+"vlclibs");
		if (RuntimeUtil.isWindows()) {
			suspects.add(System.getenv("ProgramFiles") + "\\videolan\\vlc");
			
			//maybe read system registry to try and find VLC
		}
		else if (RuntimeUtil.isNix()) {
			//suspects.add("/usr/bin/vlc");  << vlc executable
			suspects.add("/usr/lib");
		}
		else {
			throw new RuntimeException("Platform not supported, currently only Linux and Windows.");
		}
		
		Iterator<String> iter = suspects.iterator();
		while (iter.hasNext()) {
			String path = iter.next();
			if (!checkVlcLibs(path)) {
				iter.remove();
			}
		}
		return suspects;
	}
	
	private static Set<String> getVlcLibNames() {
		Set<String> out = new HashSet<String>();
		if (RuntimeUtil.isWindows()) {
			out.add("libvlccore.dll");
			out.add("libvlc.dll");
		} else {
			out.add("libvlc.so.5");
			//out.add("libvlccore.so.4");
		}
		return out;
	}


	public boolean verifyVlcLibs(String possibleLibPath) {
		//if found works, that's perfect
		if (this.foundVlcLibPaths!=null) {
			for (String vlcLibPath : this.foundVlcLibPaths) {
				if (checkVlcLibs(vlcLibPath)) {
					return true;
				}
			}
		}
		
		//if none worked, searches
		boolean foundOne=false;
		Set<String> foundVlcLibs = findVlcLibs(possibleLibPath);
		for (String vlcLibPath : foundVlcLibs) {
			if (checkVlcLibs(vlcLibPath)) {
				this.foundVlcLibPaths.add(vlcLibPath);
				foundOne=true;
			}
		}
		return foundOne;
	}


	public boolean verifyVlcLibs() {
		return this.verifyVlcLibs(this.userVlcLibPath);
	}
}