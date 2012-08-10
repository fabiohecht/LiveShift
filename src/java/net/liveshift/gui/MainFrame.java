package net.liveshift.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Graphics;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import net.liveshift.core.LiveShiftApplication.ConnectionState;


public class MainFrame extends JFrame {
	
	private static final long serialVersionUID = 6185018969437336868L;
	
	JPanel controlPnl;
	JButton connectBtn;
	JButton publishBtn;
	JButton channelsBtn;
	JButton setupBtn;
	JButton statisticsBtn;
	
	final protected JFrame mainFrame;
	final private LiveShiftGUI liveShiftGUI;
	
	public MainFrame(final LiveShiftGUI liveShiftGUI) {
		
		this.liveShiftGUI = liveShiftGUI;
		this.mainFrame = this;
		
		this.setTitle("LiveShift");
		this.setIconImages(Design.getAppIcons());
		
		this.setBounds(liveShiftGUI.getAppBounds(LiveShiftGUI.SubAppPosition.ALL));
		
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		// gradient background
		JPanel backgroundPanel = new JPanel() {
			@Override
			protected void paintComponent(Graphics grphcs) {
				
				Design.paintGradientBackground(grphcs, getWidth(), getHeight());
				
				super.paintComponent(grphcs);
			}
		};
		backgroundPanel.setOpaque(false);
		backgroundPanel.setLayout(new BorderLayout());
		this.setContentPane(backgroundPanel);
		
		controlPnl = new JPanel();
		controlPnl.setLayout(new BoxLayout(controlPnl, BoxLayout.Y_AXIS));
		controlPnl.setOpaque(false);
		controlPnl.setBorder(new EmptyBorder(10, 10, 10, 10));
		
		connectBtn = new LiveShiftButton(Design.getInstance().getIcon(Design.ICON_NETWORK_DISCONNECTED), true);
		connectBtn.setToolTipText(Design.TOOLTIP_CONNECT);
		connectBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (liveShiftGUI.getConnectionState() == ConnectionState.CONNECTED) {
					liveShiftGUI.disconnectFromNetwork();
				} else {
					liveShiftGUI.connectToNetwork();
				}
			}
		});
		controlPnl.add(Box.createVerticalStrut(10));
		controlPnl.add(connectBtn);
		
		publishBtn = new LiveShiftButton(Design.getInstance().getIcon(Design.ICON_PUBLISH), true);
		publishBtn.setToolTipText(Design.TOOLTIP_PUBLISH_DISCONNECTED);
		publishBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (liveShiftGUI.getConnectionState() == ConnectionState.CONNECTED) {
					if (liveShiftGUI.getPublishState()) {
						liveShiftGUI.unpublishChannel();
						liveShiftGUI.getChannelsPanel().updateChannelList();
					} else {
						liveShiftGUI.getPublishFrame().setVisible(!liveShiftGUI.getPublishFrame().isVisible());
					}
				} else {
					JOptionPane.showMessageDialog(mainFrame, "In order to publish your own channel, you need to be connected to the LiveShift network.");
				}
			}
		});
		controlPnl.add(Box.createVerticalStrut(10));
		controlPnl.add(publishBtn);
		
		channelsBtn = new LiveShiftButton(Design.getInstance().getIcon(Design.ICON_TOGGLE_CHANNEL_LIST), true);
		channelsBtn.setToolTipText(Design.TOOLTIP_TOGGLE_CHANNEL_LIST_DISCONNECTED);
		channelsBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				liveShiftGUI.toggleChannelListVisible();
			}
		});
		controlPnl.add(Box.createVerticalStrut(10));
		controlPnl.add(channelsBtn);
		
		setupBtn = new LiveShiftButton(Design.getInstance().getIcon(Design.ICON_SETTINGS), true);
		setupBtn.setToolTipText(Design.TOOLTIP_SETTINGS);
		setupBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				liveShiftGUI.getSetupFrame().updateValues();
				liveShiftGUI.getSetupFrame().setVisible(!liveShiftGUI.getSetupFrame().isVisible());
			}
		});
		controlPnl.add(Box.createVerticalStrut(10));
		controlPnl.add(setupBtn);
		
		statisticsBtn = new LiveShiftButton(Design.getInstance().getIcon(Design.ICON_STATISTICS), true);
		statisticsBtn.setToolTipText(Design.TOOLTIP_STATISTICS);
		statisticsBtn.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				liveShiftGUI.getStatisticsFrame().setVisible(!liveShiftGUI.getStatisticsFrame().isVisible());
			}
		});
		controlPnl.add(Box.createVerticalStrut(10));
		controlPnl.add(statisticsBtn);
		/*
		 * helpBtn = new LiveShiftButton(Design.getInstance().getIcon(
		 * Design.ICON_HELP), true);
		 * helpBtn.setToolTipText(Design.TOOLTIP_HELP);
		 * helpBtn.addMouseListener(new MouseAdapter() { public void
		 * mousePressed(MouseEvent e) { File pdfFile = new
		 * File(System.getProperty("user.dir") + "/help.pdf"); if
		 * (pdfFile.exists()) { if (Desktop.isDesktopSupported()) { try {
		 * Desktop.getDesktop().open(pdfFile); } catch (IOException e1) { //
		 * TODO Auto-generated catch block e1.printStackTrace(); } } else {
		 * System.out.println("Awt Desktop is not supported!"); }
		 * 
		 * } else {
		 * System.out.println("File ("+pdfFile.getName()+") does not exist!"); }
		 * } }); wrapperPnl.add(helpBtn);
		 */
		getContentPane().add(controlPnl, BorderLayout.WEST);
	}
	
}
