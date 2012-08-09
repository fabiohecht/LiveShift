package net.liveshift.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Design {
	// Init logger
	private static final Logger logger = LoggerFactory.getLogger(Design.class);

	public static final String TOOLTIP_CONNECT = "<html><b>Connect</b><br />Connect to the LiveShift network.</html>";
	public static final String TOOLTIP_DISCONNECT = "<html><b>Disconnect</b><br />Leave the LiveShift network.</html>";
	public static final String TOOLTIP_PUBLISH_DISCONNECTED = "<html><b>Publish</b><br />Please connect to the LiveShift network first.</html>";
	public static final String TOOLTIP_PUBLISH_CONNECTED = "<html><b>Publish</b><br />Create your own LiveShift channel.</html>";
	public static final String TOOLTIP_PUBLISH_PUBLISHING = "<html><b>Unpublish</b><br />Stop publishing.</html>";
	public static final String TOOLTIP_TOGGLE_CHANNEL_LIST_DISCONNECTED = "<html><b>Toggle channel list</b><br />Please connect to the LiveShift network first.</html>";
	public static final String TOOLTIP_TOGGLE_CHANNEL_LIST_CONNECTED = "<html><b>Toggle channel list</b><br />.</html>";
	public static final String TOOLTIP_SETTINGS = "<html><b>Settings</b><br />Configure network, video, and storage.</html>";
	public static final String TOOLTIP_STATISTICS = "<html><b>Statistics</b><br />P2P network information.</html>";
	public static final String TOOLTIP_HELP = "<html><b>Help</b><br />A small tutorial about how LiveShift works.</html>";
	public static final String TOOLTIP_CLOSE_CHANNEL_LIST = "Close channel list";
	public static final String TOOLTIP_SEARCH = "Search";
	
	public static final String LOGO_BACKGROUND_IMAGE_FILE = "/logo_background.png";
	public static final String LOGO_SPLASH_IMAGE_FILE = "/logo_watermark.png";
	
	public static final String[] APP_ICONS = new String[]{"/logo-icon-64-alpha.png","/logo-icon-48-alpha.png","/logo-icon-32-alpha.png","/logo-icon-20-alpha.png","/logo-icon-16-alpha.png"};
	
	/*
	 * definitions of the colors and fonts used
	 */
	public static Color backgroundColor = new Color(80, 110, 190);
	public static Font smallFont = new Font("Arial", Font.PLAIN, 12);
	public static Font mediumFont = new Font("Arial", Font.PLAIN, 14);
	public static Font mediumFontBold = new Font("Arial", Font.BOLD, 14);
	public static Font titleFont = new Font("Arial", Font.BOLD, 16);
	public static Color titleColor = new Color(0, 0, 0);
	public static Color titleDescriptionColor = new Color(0, 0, 0);
	public static Color borderColor = new Color(80, 110, 190);

	protected static final Color TEXT_COLOR_LIGHT = Color.WHITE;
	protected static final Color TEXT_COLOR_DARK = Color.BLACK;
	protected static final Color TEXT_COLOR_DISABLED = Color.GRAY;

	/*
	 * cache icons
	 */
	private static Map<String, ImageIcon> icons;
	public static String ICON_NETWORK_DISCONNECTED = "network_disconnected";
	public static String ICON_NETWORK_CONNECTED = "network_connected";
	public static String ICON_NETWORK_CONNECTING = "network_connecting";
	public static String ICON_TOGGLE_CHANNEL_LIST = "toggle_channel_list";
	public static String ICON_PUBLISH = "publish";
	public static String ICON_PUBLISHING = "publishing";
	public static String ICON_SETTINGS = "settings";
	public static String ICON_STATISTICS = "statistics";
//	public static String ICON_HELP = "help";
	
	public static String ICON_TIMESHIFT = "timeshift";
	public static String ICON_PLAY = "play";
	public static String ICON_PAUSE = "pause";
	public static String ICON_STOP = "stop";
	public static String ICON_FASTFORWARD = "fastforward";
	public static String ICON_REWIND = "rewind";
	public static String ICON_FULL_SCREEN = "full_screen";
	public static String ICON_VOLUME_UP = "volume_up";
	public static String ICON_VOLUME_DOWN = "volume_down";

	public static String ICON_CLOSE_CHANNEL_LIST = "close_channel_list";
	public static String ICON_SEARCH = "search";

	private static List<String> iconIds;
	private static Map<String, Integer> iconSizes;

	private static Design instance;

	private Design() {
		iconIds = new ArrayList<String>();
		iconIds.add(ICON_NETWORK_DISCONNECTED);
		iconIds.add(ICON_NETWORK_CONNECTED);
		iconIds.add(ICON_NETWORK_CONNECTING);
		iconIds.add(ICON_PAUSE);
//		iconIds.add(ICON_HELP);
		iconIds.add(ICON_TIMESHIFT);
		iconIds.add(ICON_FASTFORWARD);
		iconIds.add(ICON_PLAY);
		iconIds.add(ICON_STOP);
		iconIds.add(ICON_PUBLISH);
		iconIds.add(ICON_PUBLISHING);
		iconIds.add(ICON_REWIND);
		iconIds.add(ICON_SETTINGS);
		iconIds.add(ICON_STATISTICS);
		iconIds.add(ICON_CLOSE_CHANNEL_LIST);
		iconIds.add(ICON_TOGGLE_CHANNEL_LIST);
		iconIds.add(ICON_FULL_SCREEN);
		iconIds.add(ICON_VOLUME_UP);
		iconIds.add(ICON_VOLUME_DOWN);
		iconIds.add(ICON_SEARCH);
		
		iconSizes = new HashMap<String, Integer>();
		iconSizes.put(ICON_NETWORK_DISCONNECTED, 48);
		iconSizes.put(ICON_NETWORK_CONNECTED, 48);
		iconSizes.put(ICON_NETWORK_CONNECTING, 48);
		iconSizes.put(ICON_PAUSE, 32);
//		iconSizes.put(ICON_HELP, 48);
		iconSizes.put(ICON_TIMESHIFT, 32);
		iconSizes.put(ICON_SETTINGS, 48);
		iconSizes.put(ICON_FASTFORWARD, 32);
		iconSizes.put(ICON_PLAY, 32);
		iconSizes.put(ICON_STOP, 32);
		iconSizes.put(ICON_PUBLISH, 48);
		iconSizes.put(ICON_PUBLISHING, 48);
		iconSizes.put(ICON_REWIND, 32);
		iconSizes.put(ICON_STATISTICS, 48);
		iconSizes.put(ICON_CLOSE_CHANNEL_LIST, 16);
		iconSizes.put(ICON_TOGGLE_CHANNEL_LIST, 48);
		iconSizes.put(ICON_FULL_SCREEN, 32);
		iconSizes.put(ICON_VOLUME_UP, 32);
		iconSizes.put(ICON_VOLUME_DOWN, 32);
		iconSizes.put(ICON_SEARCH, 16);

		icons = new HashMap<String, ImageIcon>();

		// load icons into cache
		for (String id : iconIds) {
			try {
				ImageIcon icon;
				icon = new ImageIcon(Design.class.getResource("/" + id + ".png"));
				icons.put(id, new ImageIcon(icon.getImage().getScaledInstance(iconSizes.get(id), iconSizes.get(id), Image.SCALE_SMOOTH)));
//				CLI.log("icon loaded: " + id);
			} catch (Exception ex) {
//				CLI.log("unable to load icon: " + id);
//				CLI.log(ex.getMessage());
				logger.warn("unable to load icon: " + id, ex);
			}
		}
	}

	// singleton
	public static Design getInstance() {
		if (instance == null)
			instance = new Design();
		return instance;
	}

	public ImageIcon getIcon(String id) {
		if (icons.get(id) != null)
			return icons.get(id);
		else {
			logger.warn("Icon not found: "+id);
			return new ImageIcon();
		}
	}

	public static List<? extends Image> getAppIcons() {
		List<Image> icons = new ArrayList<Image>(4);
		for (String icon : APP_ICONS) {
			ImageIcon image;
			image = new ImageIcon(Design.class.getResource(icon));
			icons.add(image.getImage());
		}
		return icons;
	}

	public static Image getAppIcon() {
		ImageIcon image;
		image = new ImageIcon(Design.class.getResource(APP_ICONS[0]));
		return image.getImage();
	}
	
	public static Rectangle getAvailableScreen() {
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		return ge.getMaximumWindowBounds();
	}


	public static Rectangle getCenteredPosition(int width, int height) {
		Rectangle dim = getAvailableScreen();

		return new Rectangle((dim.width-width)/2, (dim.height-height)/2, width, height);
	}

	public static void paintGradientBackground(Graphics grphcs, int width, int height) {

        Graphics2D g2d = (Graphics2D) grphcs;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        GradientPaint gp = new GradientPaint(0, 0, Color.DARK_GRAY, width, height, Color.BLACK);
        
        g2d.setPaint(gp);
        g2d.fillRect(0, 0, width, height);
        		
	}
}
