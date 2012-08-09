package net.liveshift.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;

import net.liveshift.configuration.Configuration;
import net.liveshift.configuration.PublishConfiguration;
import net.liveshift.configuration.Configuration.EncoderName;
import net.liveshift.core.Channel;
import net.liveshift.core.DHTConnectionException;
import net.liveshift.core.LiveShiftApplication;
import net.liveshift.core.PlayerInitializationException;
import net.liveshift.core.UiListener;
import net.liveshift.core.LiveShiftApplication.ConnectionState;
import net.liveshift.core.LiveShiftApplication.PlaybackStatus;
import net.liveshift.core.LiveShiftApplication.PlayerStatus;
import net.liveshift.player.ExtVLCPlayer;
import net.liveshift.player.VLCJPlayer;
import net.liveshift.util.ExecutorPool;
import net.liveshift.util.Utils;

import org.jfree.ui.OverlayLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LiveShiftGUI implements UiListener {

	final private static Logger logger = LoggerFactory.getLogger(LiveShiftGUI.class);

	final private LiveShiftApplication liveShiftApplication;

	private CheckFrame checkFrame;
	private SettingsFrame settingsFrame;
	private ChannelsPanel channelsPanel;
	private CenterPanel centerPanel;
	private MainFrame mainFrame;
	private StatisticsFrame statisticsFrame;
	private PublishFrame publishFrame;
	private JPanel contentWrapperPanel;
	private HelpFrame helpFrame;
	private StatusPanel statusPanel;
	private FullscreenFrame fullscreenFrame;
	private boolean fullscreen = false;

	public LiveShiftGUI(final LiveShiftApplication liveShiftApplication) {
		Locale.setDefault(Locale.ENGLISH);
		
		this.liveShiftApplication = liveShiftApplication;

		logger.debug("liveshiftgui - start");

		//sets look and feel
		try {
			String lookAndFeel = null;
			/*
			 * for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) { if ("Nimbus".equals(info.getName())) { lookAndFeel = info.getClassName();
			 * break; } }
			 */
			if (lookAndFeel == null)
				lookAndFeel = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeel);

		}
		catch (Exception e) {
			e.printStackTrace();
		}

		
		//checks whether to show welcome dialog
		if (this.liveShiftApplication.getConfiguration().isFirstTimeRun()) {
			ExecutorPool.getScheduledExecutorService().schedule(new Runnable() {

				@Override
				public void run() {
					showWelcomeDialog();
				}
			}, 3, TimeUnit.SECONDS);
			
			this.liveShiftApplication.getConfiguration().setFirstTimeRun(false);
		}

		liveShiftApplication.addUiListener(this);

		centerPanel = new CenterPanel(this);
		this.liveShiftApplication.registerVideoCanvas(centerPanel.getVideoCanvas());

		centerPanel.setButtonsVisible(false);
		centerPanel.setScreenVisible(false);

		helpFrame = new HelpFrame();
		helpFrame.setVisible(false);

		channelsPanel = new ChannelsPanel(this);

		checkFrame = new CheckFrame(this);

		settingsFrame = new SettingsFrame(this);
		settingsFrame.setVisible(false);
		mainFrame = new MainFrame(this);
		setupPublish(liveShiftApplication.getPublishConfiguration());
		publishFrame = new PublishFrame(this, this.liveShiftApplication.getPublishConfiguration());

		contentWrapperPanel = new JPanel();
		contentWrapperPanel.setOpaque(false);
		contentWrapperPanel.setLayout(new OverlayLayout());

		contentWrapperPanel.add(centerPanel);

		mainFrame.add(contentWrapperPanel, BorderLayout.CENTER);

		mainFrame.add(channelsPanel, BorderLayout.EAST);

		statusPanel = new StatusPanel();
		mainFrame.add(statusPanel, BorderLayout.SOUTH);

		channelsPanel.setVisible(false);

		statisticsFrame = new StatisticsFrame(this);

		mainFrame.setVisible(true);
		
		fullscreenFrame = new FullscreenFrame(this);
		
		checkFrame.setVisible(true);
		if (checkFrame.runChecks(liveShiftApplication.getConfiguration())) {
			checkFrame.dispose();

			if (this.liveShiftApplication.getConfiguration().isAutoConnectOnStartup()) {
				this.connectToNetwork();
			}

		}
		else {
			logger.debug("liveshiftgui - start - checks unsuccessful");
		}

		logger.debug("liveshiftgui - completed");
	}

	public void connectToNetwork() {

		int localPort = getLiveShiftApplication().getConfiguration().getP2pPort();
		String host = getLiveShiftApplication().getConfiguration().getBootstrapAddress();

		if (localPort > 0 && localPort < 65536) {
			try {
				if (!getLiveShiftApplication().connect(host, null)) {
					if (JOptionPane.showConfirmDialog(null, "Unable to connect to network. Do you want to change the connection settings?",
							"Unable to connect to network", JOptionPane.YES_NO_OPTION) == 0) {
						getSetupFrame().setVisible(true);
					}
				}
			}
			catch (DHTConnectionException e) {
				e.printStackTrace();
				logger.error("DHTConnectionException when connecting: " + e.getMessage());
				if (JOptionPane.showConfirmDialog(null, "Unable to connect to network. Check network interface, ports, bootstrap peer address, and your Internet connection. Do you want to change the connection settings?",
						"Unable to connect to network", JOptionPane.YES_NO_OPTION) == 0) {
					getSetupFrame().showTab(0);
					getSetupFrame().setVisible(true);
				}
			} catch (PlayerInitializationException e) {
				e.printStackTrace();
				logger.error("PlayerInitializationException when connecting: " + e.getMessage());
				if (JOptionPane.showConfirmDialog(null, "Unable to initialize player. Check VLC libraries and player. Do you want to change the player settings?",
						"Unable to connect to network", JOptionPane.YES_NO_OPTION) == 0) {
					getSetupFrame().showTab(1);
					getSetupFrame().setVisible(true);
				}
			}
		}
		else {
			if (JOptionPane.showConfirmDialog(null, "Unable to connect to network. Do you want to change the connection settings?",
					"Unable to connect to network", JOptionPane.YES_NO_OPTION) == 0) {
				getSetupFrame().setVisible(true);
			}
		}
	}

	public void disconnectFromNetwork() {
		getLiveShiftApplication().disconnect();
	}

	public void play() {
		getLiveShiftApplication().playVideo();
	}

	public String checkVlcEncoder() {

		if (getLiveShiftApplication().getConfiguration().getEncoderName() == EncoderName.ExtVlc_hidden || getLiveShiftApplication().getConfiguration().getEncoderName() == EncoderName.ExtVlc_visible) {
			if (!Utils.fileExists(getLiveShiftApplication().getConfiguration().getVlcPath()))
				return "Cannot find VLC executable. Please update VLC path or use another encoder.";
		}
		else if (getLiveShiftApplication().getConfiguration().getEncoderName() == EncoderName.Vlcj) {
			if (!liveShiftApplication.getConfiguration().verifyVlcLibs()) {
				return "Cannot find VLC libraries. Please use another encoder or specify a valid VLC library path.";
			}
		}
		return "";
	}
	
	public String checkVlcPlayerUsage() {
		if (getLiveShiftApplication().getPlayer() instanceof ExtVLCPlayer) {
			if (!Utils.fileExists(getLiveShiftApplication().getConfiguration().getVlcPath()))
				return "Cannot find VLC executable. Please update VLC path or use another player.";
		}
		else if (getLiveShiftApplication().getPlayer() instanceof VLCJPlayer) {
			if (!liveShiftApplication.getConfiguration().verifyVlcLibs()) {
				return "Cannot find VLC libraries. Please use another player or specify a valid VLC library path.";
			}
		}
		return "";
	}

	public LiveShiftApplication getLiveShiftApplication() {
		return liveShiftApplication;
	}

	private Dimension getScreenDimension() {
		return Toolkit.getDefaultToolkit().getScreenSize();
	}

	private void setupPublish(PublishConfiguration publishConfiguration) {
		logger.debug("liveshiftgui - init - setup publish - started");

		List<Byte> tmp = new ArrayList<Byte>();
		for (byte i = 1; i <= Configuration.MAX_NUMBER_OF_SUBSTREAMS; i++) {
			tmp.add(i);
		}

		logger.debug("liveshiftgui - init - setup publish - completed");
	}

	public Rectangle getAppBounds(SubAppPosition position) {
		Rectangle dim = Design.getAvailableScreen();

		int posX = dim.x;
		int posY = dim.y;
		int width = dim.width;
		int height = dim.height;

		if (!fullscreen) {
			width = Math.max(dim.width / 2, 800);
			height = Math.max(dim.height / 2, 480);
			posX = (dim.width-width) / 2;
			posY = (dim.height-height) / 2;
		}

		if (position == SubAppPosition.LEFT) {
			width = 96;
		}
		else if (position == SubAppPosition.MIDDLE) {
			posX += 96;
			width = (int) ((width - 96) * 0.8);
		}
		else if (position == SubAppPosition.RIGHT) {
			posX += 96 + (int) ((width - 96) * 0.8);
			width = (int) ((width - 96) * 0.2);
		}
		return new Rectangle(posX, posY, width, height);
	}
	
	public enum SubAppPosition {
		LEFT, MIDDLE, RIGHT, ALL
	}

	public CheckFrame getCheckFrame() {
		return checkFrame;
	}

	public SettingsFrame getSetupFrame() {
		return settingsFrame;
	}

	public ChannelsPanel getChannelsPanel() {
		return channelsPanel;
	}

	public CenterPanel getPlayerPanel() {
		return centerPanel;
	}

	public MainFrame getMainFrame() {
		return mainFrame;
	}

	public StatisticsFrame getStatisticsFrame() {
		return statisticsFrame;
	}

	public PublishFrame getPublishFrame() {
		return publishFrame;
	}

	public HelpFrame getHelpPanel() {
		return helpFrame;
	}

	public boolean isFullscreen() {
		return fullscreen;
	}

	public void setFullscreen(boolean fullscreen) {
		
		this.mainFrame.setVisible(!fullscreen);

		this.fullscreenFrame.setFullscreen(fullscreen);
		this.fullscreenFrame.setPlaying(fullscreen);
		this.centerPanel.setPlaying(!fullscreen);
		
		if (fullscreen) {
			this.liveShiftApplication.registerVideoCanvas(this.fullscreenFrame.getVideoCanvas());
		}
		else {
			this.liveShiftApplication.registerVideoCanvas(this.centerPanel.getVideoCanvas());
		}
		
		this.fullscreen = fullscreen;
	}

	public void toggleFullScreen() {
		setFullscreen(!isFullscreen());
	}
	
	public String getCurrentVersion() {
		File versionFile = new File("liveshift.version");
		if (!versionFile.exists())
			return "0.0";
		else {
			try {
				BufferedReader in = new BufferedReader(new FileReader("liveshift.version"));
				String version = in.readLine();
				return version;
			}
			catch (FileNotFoundException e) {
				return "0.0";
			}
			catch (IOException e) {
				return "0.0";
			}
		}
	}

	public void switchChannel(Channel channel, long timeshiftUtcMS) {

		centerPanel.setButtonsVisible(true);
		centerPanel.setScreenVisible(true);

		this.liveShiftApplication.switchChannelUtc(channel, timeshiftUtcMS);
		
	}

	public void switchToSelectedChannel(long timeshiftUtcS) {
		Channel channel = getChannelsPanel().getSelectedChannel();
		if (channel != null) {
			switchChannel(channel, timeshiftUtcS);
		}
		else {
			JOptionPane.showMessageDialog(mainFrame, "Please select a channel first.");
		}
	}

	public void stop() {
		getLiveShiftApplication().stopVideo();
	}

	public void pause() {
		getLiveShiftApplication().pauseVideo();
	}

	public void showPlayButtons() {
		centerPanel.setButtonsVisible(true);
	}

	public void setChannelListVisible(boolean visible) {
		getChannelsPanel().setVisible(visible);
	}

	public void toggleChannelListVisible() {
		setChannelListVisible(!getChannelsPanel().isVisible());
	}

	public void resetCenterPanel() {
		centerPanel.setButtonsVisible(true);
		centerPanel.setScreenVisible(false);
	}

	@Override
	public void connectionStateChanged() {

		ConnectionState connectionState = getLiveShiftApplication().getConnectionState();

		logger.debug("connectionStateChanged:" + connectionState);

		if (connectionState == ConnectionState.CONNECTED) {
			this.setChannelListVisible(true);
			centerPanel.setButtonsVisible(false);
			centerPanel.setScreenVisible(false);

			mainFrame.connectBtn.setIcon(Design.getInstance().getIcon(Design.ICON_NETWORK_CONNECTED));
			mainFrame.connectBtn.setToolTipText(Design.TOOLTIP_DISCONNECT);
			mainFrame.publishBtn.setToolTipText(Design.TOOLTIP_PUBLISH_CONNECTED);
			mainFrame.channelsBtn.setToolTipText(Design.TOOLTIP_TOGGLE_CHANNEL_LIST_CONNECTED);

			this.channelsPanel.updateChannelList();
			this.setChannelListVisible(true);
			this.channelsPanel.	updateChannelList();
			
			this.statusPanel.setNetworkText("Connected");

		}
		else if (connectionState == ConnectionState.CONNECTING) {

			mainFrame.connectBtn.setIcon(Design.getInstance().getIcon(Design.ICON_NETWORK_CONNECTING));

			this.statusPanel.setNetworkText("Connecting...");

		}
		else if (connectionState == ConnectionState.DISCONNECTED) {

			setChannelListVisible(false);
			resetCenterPanel();

			mainFrame.connectBtn.setIcon(Design.getInstance().getIcon(Design.ICON_NETWORK_DISCONNECTED));
			mainFrame.connectBtn.setToolTipText(Design.TOOLTIP_CONNECT);
			mainFrame.publishBtn.setToolTipText(Design.TOOLTIP_PUBLISH_DISCONNECTED);
			mainFrame.channelsBtn.setToolTipText(Design.TOOLTIP_TOGGLE_CHANNEL_LIST_DISCONNECTED);

			mainFrame.setVisible(true);

			centerPanel.setButtonsVisible(false);
			centerPanel.setScreenVisible(false);

			this.setChannelListVisible(false);
			this.statusPanel.setNetworkText("Not connected.");

		}
	}

	private void showWelcomeDialog() {

		String dialogText = "<html><div style='width:400px;'>" + "<P>Thank you for participating in the LiveShift trial.<br/><br/> LiveShift is a "
				+ "peer-to-peer open source software that allows the transmission of live " + "events that can be watched at any future time.<br/><br/>"
				+ "Please report any bugs you may find at " + "<a href='https://sourceforge.net/projects/liveshift/forums/forum/1842688'>"
				+ "https://sourceforge.net/projects/liveshift/forums/forum/1842688</a>.<br/><br/>"
				+ "With your permission, LiveShift collects anonymous usage data, which are "
				+ "used for fixing bugs, evaluating the trial, and improving its overall "
				+ "performance.<br/><br/>Please select below your preference. You can always "
				+ "change your mind at the application settings window.</div></html>";

		Object[] options = { "No, I'm a selfish bastard", "Sure, I want to help" };

		int n = JOptionPane.showOptionDialog(this.mainFrame, dialogText, "Welcome to LiveShift", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
				options, "Sure, I want to help");

		switch (n) {
		case 0: // no

			this.liveShiftApplication.getConfiguration().setAllowDataCollection(false);

			break;
		case 1: // yes

			this.liveShiftApplication.getConfiguration().setAllowDataCollection(true);
		}
	}

	@Override
	public void publishStateChanged(PublishConfiguration publishTabBean) {

		if (publishTabBean.getPublished()) {
			this.statusPanel.setPublishText("Publishing " + publishTabBean.getName());
			mainFrame.publishBtn.setIcon(Design.getInstance().getIcon(Design.ICON_PUBLISHING));
			mainFrame.publishBtn.setToolTipText(Design.TOOLTIP_PUBLISH_PUBLISHING);
			
			this.channelsPanel.	updateChannelList();
		}
		else {
			this.statusPanel.setPublishText("Not publishing");
			mainFrame.publishBtn.setIcon(Design.getInstance().getIcon(Design.ICON_PUBLISH));
			mainFrame.publishBtn.setToolTipText(Design.TOOLTIP_PUBLISH_CONNECTED);
			
			this.channelsPanel.	updateChannelList();
		}
	}

	@Override
	public void playingStateChanged(Channel channel, PlayerStatus playerStatus, PlaybackStatus playbackStatus, long playTime) {
		logger.debug("playingStateChanged:" + channel + "," + playerStatus + "," + playbackStatus + "," + playTime);
		
		String channelName = "";
		if (channel != null) {
			channelName = channel.getName();
		}
		this.statusPanel.playingStateChanged(channelName, playerStatus, playbackStatus, playTime);
	}

	public Collection<Channel> getChannelsByTags(String[] tags) {
		return this.liveShiftApplication.getChannelsByTags(tags);
	}

	public void publishChannelDevice(String channelName, Byte numSubstreams, String description, String deviceName) {
		// run encoder and writes channel on DHT

		this.liveShiftApplication.publishChannelDevice(channelName, numSubstreams, description, deviceName);
	}

	public void publishChannelFile(String channelName, Byte numSubstreams, String description, String fileName) {
		// run encoder and writes channel on DHT
		this.liveShiftApplication.publishChannelFile(channelName, numSubstreams, description, fileName);
	}

	@Override
	public void volumeChanged(float volumePercent) {
		this.centerPanel.getPlayControlPanel().setVolumeLevel(volumePercent);
		this.fullscreenFrame.getPlayControlPanel().setVolumeLevel(volumePercent);
	}

	public void publishChannelNetwork(String channelName, Byte numSubstreams, String description, String string, String networkSource) {
		this.liveShiftApplication.publishChannelNetwork(channelName, numSubstreams, description, networkSource);
	}

	public ConnectionState getConnectionState() {
		return this.liveShiftApplication.getConnectionState();
	}

	public boolean getPublishState() {
		return this.liveShiftApplication.getPublishState();
	}

	public void unpublishChannel() {
		this.liveShiftApplication.unpublishChannel();
	}

	public int testUploadRate() {
		return this.liveShiftApplication.testUploadRateKbytePerSecond();
	}
}
