package net.liveshift.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.border.TitledBorder;

import net.liveshift.configuration.Configuration;
import net.liveshift.configuration.Configuration.EncoderName;
import net.liveshift.configuration.Configuration.PlayerName;
import net.liveshift.configuration.Configuration.StreamingProtocol;
import net.liveshift.core.LiveShiftApplication;
import net.liveshift.encoder.ExtVLCEncoder;
import net.liveshift.encoder.VLCJEncoder;
import net.liveshift.player.DummyPlayer;
import net.liveshift.player.ExtVLCPlayer;
import net.liveshift.player.VLCJPlayer;
import net.liveshift.util.Utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import javax.swing.SwingConstants;

public class SettingsFrame extends JDialog {

	final private static Logger logger = LoggerFactory.getLogger(SettingsFrame.class);

	private static final long serialVersionUID = -6472612953571389098L;
	
	final private LiveShiftGUI liveShiftGUI;

	private JPanel contentPane;
	private JTextField txtBootstrapPeer;
	private JTextField txtPeerName;
	private JTextField txtStorageFolder;
	private JTextField txtMaxStoragesizeBlocks;
	private JTextField txtP2pPort;
	private JTextField txtEncoderPort;
	private JTextField txtNtpServer;
	private JComboBox cmbInterface;
	private JCheckBox chckbxConnectAtStartup;
	private JCheckBox chckbxEnableUPnp;
	private JCheckBox chckbxAllowDataCollection;
	private JComboBox cmbEncoder;
	private JComboBox cmbPlayer;
	private JComboBox cmbPlayerProtocol;
	private JTextField txtUploadRate;
	private JTextField txtVlcPath;
	private JTextField txtUserVlcLibPath;
	private JTextField txtPlayerPort;
	private JFileChooser fileChooser = new JFileChooser();
	final private JTabbedPane tabbedPane;
	
	public SettingsFrame(final LiveShiftGUI liveShiftGUI) {
		
		this.liveShiftGUI = liveShiftGUI;
		
		this.setTitle("LiveShift - Settings");
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.setModalityType(ModalityType.APPLICATION_MODAL);
		this.setIconImages(Design.getAppIcons());
		this.getContentPane().setLayout(new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS));
		this.setBounds(Design.getCenteredPosition(550, 450));

		tabbedPane = new JTabbedPane(SwingConstants.TOP);
		getContentPane().add(tabbedPane);

		tabbedPane.addTab("Network", null, getNetworkPanel(), null);
		tabbedPane.addTab("Video", null, getVideoPanel(), null);
		tabbedPane.addTab("Storage", null, getStoragePanel(), null);

		this.getContentPane().add(Box.createVerticalStrut(10));

		JPanel hintPanel = new JPanel();
		this.getContentPane().add(hintPanel);
		hintPanel.setLayout(new BoxLayout(hintPanel, BoxLayout.X_AXIS));

		JLabel lblSettingsAreApplied = new JLabel("Settings are applied when connection is established.");
		lblSettingsAreApplied.setHorizontalAlignment(SwingConstants.LEFT);
		hintPanel.add(lblSettingsAreApplied);
		hintPanel.add(Box.createHorizontalGlue());

		this.getContentPane().add(Box.createVerticalGlue());

		JPanel buttonPanel = new JPanel();
		this.getContentPane().add(buttonPanel);
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

		buttonPanel.add(Box.createHorizontalGlue());

		JButton btnCancel = new JButton("     Cancel     ");
		btnCancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (!unsavedChanges())
					close();
				else {
					if (JOptionPane.showConfirmDialog(contentPane, "You have unsaved changes. Close without saving?", "Unsaved changes",
							JOptionPane.YES_NO_OPTION) == 0)
						close();
				}
			}
		});

		buttonPanel.add(btnCancel);
		buttonPanel.add(Box.createHorizontalStrut(5));

		JButton btnOk = new JButton("       OK       ");
		btnOk.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				String endMsg = checkVlcEncoder();
				String playMsg = checkVlcPlayer();
				if (endMsg == "" && playMsg == "") {
					saveChanges();
					close();
				} else {
					JOptionPane.showMessageDialog(contentPane, endMsg + "\n" + playMsg);
				}
			}
		});
		buttonPanel.add(btnOk);
		buttonPanel.add(Box.createHorizontalStrut(5));

	}

	private String checkVlcEncoder() {
		if (cmbEncoder.getSelectedItem().toString().compareTo("External VLC (hidden)") == 0
				|| cmbEncoder.getSelectedItem().toString().compareTo("External VLC (visible)") == 0) {
			if (!Utils.fileExists(txtVlcPath.getText()))
				return "Cannot find local VLC. Please update VLC path or use another encoder.";
		} else if (cmbEncoder.getSelectedItem().toString().compareTo("Integrated VLC (hidden)") == 0) {
			if (!liveShiftGUI.getLiveShiftApplication().getConfiguration().verifyVlcLibs(txtUserVlcLibPath.getText())) {
				return "Cannot find VLC libraries. Please use another encoder or specify a valid VLC library path.";
			}
		}
		return "";
	}

	private String checkVlcPlayer() {
		if (cmbPlayer.getSelectedItem().toString().compareTo("External VLC") == 0) {
			if (!Utils.fileExists(txtVlcPath.getText()))
				return "Cannot find local VLC. Please update VLC path or use another player.";
		} else if (cmbPlayer.getSelectedItem().toString().compareTo("Integrated VLC") == 0) {
			if (!liveShiftGUI.getLiveShiftApplication().getConfiguration().verifyVlcLibs(txtUserVlcLibPath.getText())) {
				return "Cannot find VLC libraries. Please use another player or specify a valid VLC library path.";
			}
		}
		return "";
	}

	private JPanel getVideoPanel() {
		Configuration configuration = liveShiftGUI.getLiveShiftApplication().getConfiguration();

		JPanel pnlVideoSettings = new JPanel();

		GridBagLayout gbl_pnlVideoSettings = new GridBagLayout();
		gbl_pnlVideoSettings.columnWidths = new int[] { 0 };
		gbl_pnlVideoSettings.rowHeights = new int[] { 0, 0, 0, 0 };
		gbl_pnlVideoSettings.columnWeights = new double[] { 1.0 };
		gbl_pnlVideoSettings.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0 };
		pnlVideoSettings.setLayout(gbl_pnlVideoSettings);

		JPanel pnlEncoder = new JPanel();
		GridBagConstraints gbc_pnlEncoder = new GridBagConstraints();
		gbc_pnlEncoder.fill = GridBagConstraints.BOTH;
		gbc_pnlEncoder.insets = new Insets(0, 0, 5, 0);
		gbc_pnlEncoder.anchor = GridBagConstraints.NORTHWEST;
		gbc_pnlEncoder.gridx = 0;
		gbc_pnlEncoder.gridy = 0;
		pnlVideoSettings.add(pnlEncoder, gbc_pnlEncoder);
		pnlEncoder.setBorder(new TitledBorder(null, "Encoder", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		GridBagLayout gbl_pnlEncoder = new GridBagLayout();
		gbl_pnlEncoder.columnWidths = new int[] { 180, 0, 120 };
		gbl_pnlEncoder.rowHeights = new int[] { 0, 0, 0, 0, 0, 0 };
		gbl_pnlEncoder.columnWeights = new double[] { 0.0, 1.0, 0.0 };
		gbl_pnlEncoder.rowWeights = new double[] { 0.0, 0.0, 0.0, 1.0, 0.0, Double.MIN_VALUE };
		pnlEncoder.setLayout(gbl_pnlEncoder);

		JLabel lblEncoderLabel = new JLabel("Encoder");
		GridBagConstraints gbc_lblEncoderLabel = new GridBagConstraints();
		gbc_lblEncoderLabel.anchor = GridBagConstraints.WEST;
		gbc_lblEncoderLabel.insets = new Insets(0, 0, 5, 5);
		gbc_lblEncoderLabel.gridx = 0;
		gbc_lblEncoderLabel.gridy = 0;
		pnlEncoder.add(lblEncoderLabel, gbc_lblEncoderLabel);

		cmbEncoder = new JComboBox();
		cmbEncoder.setModel(new DefaultComboBoxModel(new String[] { "External VLC (hidden)", "External VLC (visible)", "Integrated VLC (hidden)", "Dummy" }));
		GridBagConstraints gbc_cmbEncoder = new GridBagConstraints();
		gbc_cmbEncoder.insets = new Insets(0, 0, 5, 5);
		gbc_cmbEncoder.fill = GridBagConstraints.HORIZONTAL;
		gbc_cmbEncoder.gridwidth = 2;
		gbc_cmbEncoder.gridx = 1;
		gbc_cmbEncoder.gridy = 0;
		String encoder = "";
		if (liveShiftGUI.getLiveShiftApplication().getEncoder() instanceof VLCJEncoder)
			encoder = "Integrated VLC (hidden)";
		else if (liveShiftGUI.getLiveShiftApplication().getEncoder() instanceof ExtVLCEncoder)
			encoder = "External VLC (hidden)";
		else
			encoder = "External VLC (visible)";
		cmbEncoder.setSelectedItem(encoder);
		pnlEncoder.add(cmbEncoder, gbc_cmbEncoder);

		JLabel lblEncoderPort = new JLabel("Encoder port (local)");
		GridBagConstraints gbc_lblEncoderPort = new GridBagConstraints();
		gbc_lblEncoderPort.insets = new Insets(0, 0, 5, 5);
		gbc_lblEncoderPort.anchor = GridBagConstraints.WEST;
		gbc_lblEncoderPort.gridx = 0;
		gbc_lblEncoderPort.gridy = 1;
		pnlEncoder.add(lblEncoderPort, gbc_lblEncoderPort);

		txtEncoderPort = new JTextField();
		GridBagConstraints gbc_txtEncoderPort = new GridBagConstraints();
		gbc_txtEncoderPort.insets = new Insets(0, 0, 5, 5);
		gbc_txtEncoderPort.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtEncoderPort.gridwidth = GridBagConstraints.REMAINDER;
		gbc_txtEncoderPort.gridx = 1;
		gbc_txtEncoderPort.gridy = 1;
		pnlEncoder.add(txtEncoderPort, gbc_txtEncoderPort);
		txtEncoderPort.setText(Integer.toString(configuration.getEncoderPort()));

		JPanel pnlPlayer = new JPanel();
		pnlPlayer.setBorder(new TitledBorder(null, "Player", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		GridBagConstraints gbc_pnlPlayer = new GridBagConstraints();
		gbc_pnlPlayer.insets = new Insets(0, 0, 5, 0);
		gbc_pnlPlayer.fill = GridBagConstraints.BOTH;
		gbc_pnlPlayer.gridx = 0;
		gbc_pnlPlayer.gridy = 1;
		pnlVideoSettings.add(pnlPlayer, gbc_pnlPlayer);
		GridBagLayout gbl_pnlPlayer = new GridBagLayout();
		gbl_pnlPlayer.columnWidths = new int[] { 180, 0, 120, 0 };
		gbl_pnlPlayer.rowHeights = new int[] { 0, 0 };
		gbl_pnlPlayer.columnWeights = new double[] { 0.0, 1.0, 0.0, 0.0 };
		gbl_pnlPlayer.rowWeights = new double[] { 0.0, 0.0 };
		pnlPlayer.setLayout(gbl_pnlPlayer);

		JLabel lblPlayerLabel = new JLabel("Player");
		GridBagConstraints gbc_lblPlayerLabel = new GridBagConstraints();
		gbc_lblPlayerLabel.anchor = GridBagConstraints.WEST;
		gbc_lblPlayerLabel.insets = new Insets(0, 0, 5, 5);
		gbc_lblPlayerLabel.gridx = 0;
		gbc_lblPlayerLabel.gridy = 0;
		pnlPlayer.add(lblPlayerLabel, gbc_lblPlayerLabel);

		cmbPlayer = new JComboBox();
		cmbPlayer.setModel(new DefaultComboBoxModel(new String[] { "External VLC", "Integrated VLC", "Dummy Player (for testing)" }));
		GridBagConstraints gbc_cmbPlayer = new GridBagConstraints();
		gbc_cmbPlayer.insets = new Insets(0, 0, 5, 5);
		gbc_cmbPlayer.fill = GridBagConstraints.HORIZONTAL;
		gbc_cmbPlayer.gridwidth = 3;
		gbc_cmbPlayer.gridx = 1;
		gbc_cmbPlayer.gridy = 0;
		String player = "";
		if (liveShiftGUI.getLiveShiftApplication().getPlayer() instanceof ExtVLCPlayer)
			player = "External VLC";
		else if (liveShiftGUI.getLiveShiftApplication().getPlayer() instanceof VLCJPlayer)
			player = "Integrated VLC";
		else if (liveShiftGUI.getLiveShiftApplication().getPlayer() instanceof DummyPlayer)
			player = "Dummy Player (for testing)";
		else
			player = "Dummy Player (for testing)";
		cmbPlayer.setSelectedItem(player);
		pnlPlayer.add(cmbPlayer, gbc_cmbPlayer);

		JLabel lblPlayerPortlocal_1 = new JLabel("Player port (local)");
		GridBagConstraints gbc_lblPlayerPortlocal_1 = new GridBagConstraints();
		gbc_lblPlayerPortlocal_1.insets = new Insets(0, 0, 5, 5);
		gbc_lblPlayerPortlocal_1.anchor = GridBagConstraints.WEST;
		gbc_lblPlayerPortlocal_1.gridx = 0;
		gbc_lblPlayerPortlocal_1.gridy = 1;
		pnlPlayer.add(lblPlayerPortlocal_1, gbc_lblPlayerPortlocal_1);

		txtPlayerPort = new JTextField();
		GridBagConstraints gbc_txtPlayerPort = new GridBagConstraints();
		gbc_txtPlayerPort.insets = new Insets(0, 0, 5, 5);
		gbc_txtPlayerPort.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtPlayerPort.gridx = 1;
		gbc_txtPlayerPort.gridy = 1;
		pnlPlayer.add(txtPlayerPort, gbc_txtPlayerPort);
		txtPlayerPort.setText(Integer.toString(configuration.getPlayerPort()));

		JLabel lblPlayerProtocol = new JLabel("Player protocol");
		GridBagConstraints gbc_lblPlayerProtocolLabel = new GridBagConstraints();
		gbc_lblPlayerProtocolLabel.anchor = GridBagConstraints.WEST;
		gbc_lblPlayerProtocolLabel.insets = new Insets(0, 0, 5, 5);
		gbc_lblPlayerProtocolLabel.gridx = 2;
		gbc_lblPlayerProtocolLabel.gridy = 1;
		pnlPlayer.add(lblPlayerProtocol, gbc_lblPlayerProtocolLabel);

		cmbPlayerProtocol = new JComboBox();
		cmbPlayerProtocol.setModel(new DefaultComboBoxModel(StreamingProtocol.values()));
		GridBagConstraints gbc_cmbPlayerProtocol = new GridBagConstraints();
		gbc_cmbPlayerProtocol.insets = new Insets(0, 0, 5, 5);
		gbc_cmbPlayerProtocol.fill = GridBagConstraints.HORIZONTAL;
		gbc_cmbPlayerProtocol.gridx = 3;
		gbc_cmbPlayerProtocol.gridy = 1;
		StreamingProtocol playerProtocol = liveShiftGUI.getLiveShiftApplication().getConfiguration().getPlayerProtocol();
		cmbPlayerProtocol.setSelectedItem(playerProtocol);
		pnlPlayer.add(cmbPlayerProtocol, gbc_cmbPlayerProtocol);
		
		JPanel pnlVlc = new JPanel();
		pnlVlc.setBorder(new TitledBorder(null, "VLC", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		GridBagConstraints gbc_pnlVideo = new GridBagConstraints();
		gbc_pnlVideo.fill = GridBagConstraints.BOTH;
		gbc_pnlVideo.gridx = 0;
		gbc_pnlVideo.gridy = 2;
		pnlVideoSettings.add(pnlVlc, gbc_pnlVideo);
		GridBagLayout gbl_pnlVideo = new GridBagLayout();
		gbl_pnlVideo.columnWidths = new int[] { 180, 0, 0 };
		gbl_pnlVideo.rowHeights = new int[] { 0 };
		gbl_pnlVideo.columnWeights = new double[] { 0.0, 1.0, 0.0 };
		gbl_pnlVideo.rowWeights = new double[] { 0.0 };
		pnlVlc.setLayout(gbl_pnlVideo);

		JLabel lblVlcPathLabel = new JLabel("Path to ext. VLC");
		GridBagConstraints gbc_lblVlcPathLabel = new GridBagConstraints();
		gbc_lblVlcPathLabel.anchor = GridBagConstraints.WEST;
		gbc_lblVlcPathLabel.insets = new Insets(0, 0, 0, 5);
		gbc_lblVlcPathLabel.gridx = 0;
		gbc_lblVlcPathLabel.gridy = 0;
		pnlVlc.add(lblVlcPathLabel, gbc_lblVlcPathLabel);

		txtVlcPath = new JTextField();
		GridBagConstraints gbc_tfVlcPath = new GridBagConstraints();
		gbc_tfVlcPath.anchor = GridBagConstraints.WEST;
		gbc_tfVlcPath.insets = new Insets(0, 0, 0, 5);
		gbc_tfVlcPath.fill = GridBagConstraints.HORIZONTAL;
		gbc_tfVlcPath.gridx = 1;
		gbc_tfVlcPath.gridy = 0;
		pnlVlc.add(txtVlcPath, gbc_tfVlcPath);
		txtVlcPath.setText(liveShiftGUI.getLiveShiftApplication().getConfiguration().getVlcPath());

		JButton btnVlcSelect = new JButton("Select Path");
		GridBagConstraints gbc_btnVlcSelect = new GridBagConstraints();
		gbc_btnVlcSelect.anchor = GridBagConstraints.NORTHWEST;
		gbc_btnVlcSelect.insets = new Insets(0, 0, 5, 5);
		gbc_btnVlcSelect.gridx = 2;
		gbc_btnVlcSelect.gridy = 0;
		btnVlcSelect.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				fileChooser.showOpenDialog(new JFrame());
				if (fileChooser.getSelectedFile() != null) {
					File selFile = fileChooser.getSelectedFile();
					txtVlcPath.setText(selFile.getAbsolutePath());
				}
			}
		});
		pnlVlc.add(btnVlcSelect, gbc_btnVlcSelect);

		JLabel lblVlcLibPathLabel = new JLabel("Path to VLC libraries");
		GridBagConstraints gbc_lblVlcLibPathLabel = new GridBagConstraints();
		gbc_lblVlcLibPathLabel.anchor = GridBagConstraints.WEST;
		gbc_lblVlcLibPathLabel.insets = new Insets(0, 0, 0, 5);
		gbc_lblVlcLibPathLabel.gridx = 0;
		gbc_lblVlcLibPathLabel.gridy = 1;
		pnlVlc.add(lblVlcLibPathLabel, gbc_lblVlcLibPathLabel);

		txtUserVlcLibPath = new JTextField();
		GridBagConstraints gbc_tfVlcLibPath = new GridBagConstraints();
		gbc_tfVlcLibPath.anchor = GridBagConstraints.WEST;
		gbc_tfVlcLibPath.insets = new Insets(0, 0, 0, 5);
		gbc_tfVlcLibPath.fill = GridBagConstraints.HORIZONTAL;
		gbc_tfVlcLibPath.gridx = 1;
		gbc_tfVlcLibPath.gridy = 1;
		pnlVlc.add(txtUserVlcLibPath, gbc_tfVlcLibPath);
		txtUserVlcLibPath.setText(liveShiftGUI.getLiveShiftApplication().getConfiguration().getUserVlcLibPath());

		JButton btnVlcLibSelect = new JButton("Select Path");
		GridBagConstraints gbc_btnVlcLibSelect = new GridBagConstraints();
		gbc_btnVlcLibSelect.anchor = GridBagConstraints.NORTHWEST;
		gbc_btnVlcLibSelect.insets = new Insets(0, 0, 5, 5);
		gbc_btnVlcLibSelect.gridx = 2;
		gbc_btnVlcLibSelect.gridy = 1;
		btnVlcLibSelect.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				fileChooser.showOpenDialog(new JFrame());
				if (fileChooser.getSelectedFile() != null) {
					File selFile = fileChooser.getSelectedFile();
					txtUserVlcLibPath.setText(selFile.getAbsolutePath());
				}
			}
		});
		pnlVlc.add(btnVlcLibSelect, gbc_btnVlcLibSelect);

		return pnlVideoSettings;
	}

	private JPanel getStoragePanel() {
		Configuration configuration = liveShiftGUI.getLiveShiftApplication().getConfiguration();

		JPanel pnlStorage = new JPanel();

		pnlStorage.setBorder(new TitledBorder(null, "Storage", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		GridBagLayout gbl_pnlStorage = new GridBagLayout();
		gbl_pnlStorage.columnWidths = new int[] { 180, 0 };
		gbl_pnlStorage.rowHeights = new int[] { 0, 0, 0 };
		gbl_pnlStorage.columnWeights = new double[] { 0.0, 1.0 };
		gbl_pnlStorage.rowWeights = new double[] { 0.0, 0.0, 0.0 };
		pnlStorage.setLayout(gbl_pnlStorage);

		JLabel lblStorageFolder = new JLabel("Storage folder");
		GridBagConstraints gbc_lblStorageFolder = new GridBagConstraints();
		gbc_lblStorageFolder.anchor = GridBagConstraints.WEST;
		gbc_lblStorageFolder.insets = new Insets(0, 0, 5, 5);
		gbc_lblStorageFolder.gridx = 0;
		gbc_lblStorageFolder.gridy = 0;
		pnlStorage.add(lblStorageFolder, gbc_lblStorageFolder);

		txtStorageFolder = new JTextField();
		GridBagConstraints gbc_txtStorageFolder = new GridBagConstraints();
		gbc_txtStorageFolder.anchor = GridBagConstraints.WEST;
		gbc_txtStorageFolder.insets = new Insets(0, 0, 5, 5);
		gbc_txtStorageFolder.gridx = 1;
		gbc_txtStorageFolder.gridy = 0;
		gbc_txtStorageFolder.fill = GridBagConstraints.HORIZONTAL;
		pnlStorage.add(txtStorageFolder, gbc_txtStorageFolder);
		txtStorageFolder.setText(configuration.getStorageDir());

		JLabel lblMaximumStoragein = new JLabel("Storage limit (blocks)");
		GridBagConstraints gbc_lblMaximumStoragein = new GridBagConstraints();
		gbc_lblMaximumStoragein.anchor = GridBagConstraints.WEST;
		gbc_lblMaximumStoragein.insets = new Insets(0, 0, 5, 5);
		gbc_lblMaximumStoragein.gridx = 0;
		gbc_lblMaximumStoragein.gridy = 1;
		pnlStorage.add(lblMaximumStoragein, gbc_lblMaximumStoragein);

		txtMaxStoragesizeBlocks = new JTextField();
		GridBagConstraints gbc_txtMaxStoragesizeBlocks = new GridBagConstraints();
		gbc_txtMaxStoragesizeBlocks.anchor = GridBagConstraints.WEST;
		gbc_txtMaxStoragesizeBlocks.insets = new Insets(0, 0, 5, 5);
		gbc_txtMaxStoragesizeBlocks.gridx = 1;
		gbc_txtMaxStoragesizeBlocks.gridy = 1;
		gbc_txtMaxStoragesizeBlocks.fill = GridBagConstraints.HORIZONTAL;
		pnlStorage.add(txtMaxStoragesizeBlocks, gbc_txtMaxStoragesizeBlocks);
		txtMaxStoragesizeBlocks.setText(Long.toString(liveShiftGUI.getLiveShiftApplication().getConfiguration().getStorageSizeLimit()));

		chckbxAllowDataCollection = new JCheckBox("Allow anonymous usage data collection");
		GridBagConstraints gbc_chckbxAllowDataCollection = new GridBagConstraints();
		gbc_chckbxAllowDataCollection.gridwidth = 2;
		gbc_chckbxAllowDataCollection.anchor = GridBagConstraints.WEST;
		gbc_chckbxAllowDataCollection.gridx = 0;
		gbc_chckbxAllowDataCollection.gridy = 2;
		pnlStorage.add(chckbxAllowDataCollection, gbc_chckbxAllowDataCollection);
		chckbxAllowDataCollection.setSelected(configuration.isAllowDataCollection());

		return pnlStorage;
	}

	private JPanel getNetworkPanel() {

		Configuration configuration = liveShiftGUI.getLiveShiftApplication().getConfiguration();

		JPanel pnlNetworkSettings = new JPanel();

		GridBagLayout gbl_pnlNetworkSettings = new GridBagLayout();
		gbl_pnlNetworkSettings.columnWidths = new int[] { 0 };
		gbl_pnlNetworkSettings.rowHeights = new int[] { 0, 0 };
		gbl_pnlNetworkSettings.columnWeights = new double[] { 1.0 };
		gbl_pnlNetworkSettings.rowWeights = new double[] { 0.0, 0.0 };
		pnlNetworkSettings.setLayout(gbl_pnlNetworkSettings);

		JPanel pnlNetwork = new JPanel();
		GridBagConstraints gbc_pnlNetwork = new GridBagConstraints();
		gbc_pnlNetwork.fill = GridBagConstraints.BOTH;
		gbc_pnlNetwork.insets = new Insets(0, 0, 5, 0);
		gbc_pnlNetwork.anchor = GridBagConstraints.NORTHWEST;
		gbc_pnlNetwork.gridx = 0;
		gbc_pnlNetwork.gridy = 0;
		pnlNetworkSettings.add(pnlNetwork, gbc_pnlNetwork);
		pnlNetwork.setBorder(new TitledBorder(null, "Network", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		GridBagLayout gbl_pnlNetwork = new GridBagLayout();
		gbl_pnlNetwork.columnWidths = new int[] { 180, 0, 120 };
		gbl_pnlNetwork.rowHeights = new int[] { 0, 0, 0, 0, 0 };
		gbl_pnlNetwork.columnWeights = new double[] { 0.0, 1.0, 0.0 };
		gbl_pnlNetwork.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0 };
		pnlNetwork.setLayout(gbl_pnlNetwork);

		JLabel lblInterface_1 = new JLabel("Interface");
		GridBagConstraints gbc_lblInterface_1 = new GridBagConstraints();
		gbc_lblInterface_1.anchor = GridBagConstraints.WEST;
		gbc_lblInterface_1.insets = new Insets(0, 0, 5, 5);
		gbc_lblInterface_1.gridx = 0;
		gbc_lblInterface_1.gridy = 0;
		pnlNetwork.add(lblInterface_1, gbc_lblInterface_1);

		cmbInterface = new JComboBox();
		GridBagConstraints gbc_cmbInterface = new GridBagConstraints();
		gbc_cmbInterface.insets = new Insets(0, 0, 5, 5);
		gbc_cmbInterface.fill = GridBagConstraints.HORIZONTAL;
		gbc_cmbInterface.gridx = 1;
		gbc_cmbInterface.gridy = 0;
		gbc_cmbInterface.gridwidth = 2;
		Enumeration<NetworkInterface> nets;
		cmbInterface.addItem("Any");
		try {
			nets = NetworkInterface.getNetworkInterfaces();
			for (NetworkInterface netint : Collections.list(nets)) {
				if (netint.isUp())
					cmbInterface.addItem(netint.getName());
			}
		} catch (SocketException e) {
			logger.error("unable to load network interfaces", e);
		}
		cmbInterface.setSelectedItem(configuration.getNetworkInterface());
		pnlNetwork.add(cmbInterface, gbc_cmbInterface);

		JLabel lblPpPort_1 = new JLabel("P2P port");
		GridBagConstraints gbc_lblPpPort_1 = new GridBagConstraints();
		gbc_lblPpPort_1.insets = new Insets(0, 0, 5, 5);
		gbc_lblPpPort_1.anchor = GridBagConstraints.WEST;
		gbc_lblPpPort_1.gridx = 0;
		gbc_lblPpPort_1.gridy = 1;
		pnlNetwork.add(lblPpPort_1, gbc_lblPpPort_1);

		txtP2pPort = new JTextField();
		GridBagConstraints gbc_txtP2pPort = new GridBagConstraints();
		gbc_txtP2pPort.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtP2pPort.insets = new Insets(0, 0, 5, 5);
		gbc_txtP2pPort.gridx = 1;
		gbc_txtP2pPort.gridy = 1;
		gbc_txtP2pPort.gridwidth = 2;
		pnlNetwork.add(txtP2pPort, gbc_txtP2pPort);
		txtP2pPort.setText(Integer.toString(configuration.getP2pPort()));

		JLabel lblPeerName = new JLabel("Peer name");
		GridBagConstraints gbc_lblPeerName = new GridBagConstraints();
		gbc_lblPeerName.anchor = GridBagConstraints.WEST;
		gbc_lblPeerName.insets = new Insets(0, 0, 5, 5);
		gbc_lblPeerName.gridx = 0;
		gbc_lblPeerName.gridy = 2;
		pnlNetwork.add(lblPeerName, gbc_lblPeerName);

		txtPeerName = new JTextField();
		GridBagConstraints gbc_txtPeerName = new GridBagConstraints();
		gbc_txtPeerName.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtPeerName.insets = new Insets(0, 0, 5, 5);
		gbc_txtPeerName.gridx = 1;
		gbc_txtPeerName.gridy = 2;
		gbc_txtPeerName.gridwidth = 2;
		pnlNetwork.add(txtPeerName, gbc_txtPeerName);
		txtPeerName.setText(configuration.getPeerName());

		JLabel lblUploadRateblockssecond = new JLabel("Upload rate (kbyte/s)");
		GridBagConstraints gbc_lblUploadRateblockssecond = new GridBagConstraints();
		gbc_lblUploadRateblockssecond.anchor = GridBagConstraints.WEST;
		gbc_lblUploadRateblockssecond.insets = new Insets(0, 0, 5, 5);
		gbc_lblUploadRateblockssecond.gridx = 0;
		gbc_lblUploadRateblockssecond.gridy = 3;
		pnlNetwork.add(lblUploadRateblockssecond, gbc_lblUploadRateblockssecond);

		txtUploadRate = new JTextField();
		GridBagConstraints gbc_spnUploadRate = new GridBagConstraints();
		gbc_spnUploadRate.fill = GridBagConstraints.HORIZONTAL;
		gbc_spnUploadRate.insets = new Insets(0, 0, 5, 5);
		gbc_spnUploadRate.gridx = 1;
		gbc_spnUploadRate.gridy = 3;
		Integer uploadRate = configuration.getUploadRate();
		if (uploadRate!=null) {
			txtUploadRate.setText(uploadRate.toString());
		}
		pnlNetwork.add(txtUploadRate, gbc_spnUploadRate);
		
		JButton btnTestUploadRate = new JButton("Test Upload Rate");
		GridBagConstraints gbc_btnTestUploadRate = new GridBagConstraints();
		gbc_btnTestUploadRate.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnTestUploadRate.insets = new Insets(0, 0, 5, 5);
		gbc_btnTestUploadRate.gridx = 2;
		gbc_btnTestUploadRate.gridy = 3;
		pnlNetwork.add(btnTestUploadRate, gbc_btnTestUploadRate);
		final SettingsFrame that = this;
		btnTestUploadRate.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				try {
					txtUploadRate.setText(Long.toString(liveShiftGUI.testUploadRate()));
				}
				catch (RuntimeException re) {
					JOptionPane.showMessageDialog(that, "Unable to test your upload rate. Check your Internet connection.", "Test Upload Rate", JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		
		JLabel lblNtpServer = new JLabel("NTP server");
		GridBagConstraints gbc_lblNtpServer = new GridBagConstraints();
		gbc_lblNtpServer.anchor = GridBagConstraints.WEST;
		gbc_lblNtpServer.insets = new Insets(0, 0, 0, 5);
		gbc_lblNtpServer.gridx = 0;
		gbc_lblNtpServer.gridy = 4;
		pnlNetwork.add(lblNtpServer, gbc_lblNtpServer);

		txtNtpServer = new JTextField();
		GridBagConstraints gbc_txtNtpServer = new GridBagConstraints();
		gbc_txtNtpServer.insets = new Insets(0, 0, 0, 5);
		gbc_txtNtpServer.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtNtpServer.gridx = 1;
		gbc_txtNtpServer.gridy = 4;
		gbc_txtNtpServer.gridwidth = 2;
		pnlNetwork.add(txtNtpServer, gbc_txtNtpServer);
		txtNtpServer.setText(configuration.getNtpServer());

		JPanel pnlConnect = new JPanel();
		GridBagConstraints gbc_pnlConnect = new GridBagConstraints();
		gbc_pnlConnect.fill = GridBagConstraints.BOTH;
		gbc_pnlConnect.insets = new Insets(0, 0, 5, 0);
		gbc_pnlConnect.anchor = GridBagConstraints.NORTHWEST;
		gbc_pnlConnect.gridx = 0;
		gbc_pnlConnect.gridy = 1;
		pnlNetworkSettings.add(pnlConnect, gbc_pnlConnect);
		pnlConnect.setBorder(new TitledBorder(null, "Connection", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		GridBagLayout gbl_pnlConnect = new GridBagLayout();
		gbl_pnlConnect.columnWidths = new int[] { 180, 0 };
		gbl_pnlConnect.rowHeights = new int[] { 0, 0, 0 };
		gbl_pnlConnect.columnWeights = new double[] { 0.0, 1.0 };
		gbl_pnlConnect.rowWeights = new double[] { 0.0, 0.0, 0.0 };
		pnlConnect.setLayout(gbl_pnlConnect);

		JLabel lblBootstrapLabel = new JLabel("Bootstrap peer");
		GridBagConstraints gbc_lblBootstrapLabel = new GridBagConstraints();
		gbc_lblBootstrapLabel.anchor = GridBagConstraints.WEST;
		gbc_lblBootstrapLabel.insets = new Insets(0, 0, 5, 5);
		gbc_lblBootstrapLabel.gridx = 0;
		gbc_lblBootstrapLabel.gridy = 0;
		pnlConnect.add(lblBootstrapLabel, gbc_lblBootstrapLabel);

		txtBootstrapPeer = new JTextField();
		GridBagConstraints gbc_txtBootstrapPeer = new GridBagConstraints();
		gbc_txtBootstrapPeer.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtBootstrapPeer.insets = new Insets(0, 0, 5, 5);
		gbc_txtBootstrapPeer.gridx = 1;
		gbc_txtBootstrapPeer.gridy = 0;
		pnlConnect.add(txtBootstrapPeer, gbc_txtBootstrapPeer);
		txtBootstrapPeer.setText(configuration.getBootstrapAddress());

		GridBagConstraints gbc_btnConnect = new GridBagConstraints();
		gbc_btnConnect.insets = new Insets(0, 0, 5, 0);
		gbc_btnConnect.gridx = 2;
		gbc_btnConnect.gridy = 0;
		// pnlConnect.add(btnConnect, gbc_btnConnect);

		chckbxConnectAtStartup = new JCheckBox("Connect automatically at startup");
		GridBagConstraints gbc_chckbxConnectAtStartup = new GridBagConstraints();
		gbc_chckbxConnectAtStartup.anchor = GridBagConstraints.WEST;
		gbc_chckbxConnectAtStartup.gridwidth = 2;
		gbc_chckbxConnectAtStartup.insets = new Insets(0, 0, 0, 5);
		gbc_chckbxConnectAtStartup.gridx = 0;
		gbc_chckbxConnectAtStartup.gridy = 1;
		pnlConnect.add(chckbxConnectAtStartup, gbc_chckbxConnectAtStartup);
		chckbxConnectAtStartup.setSelected(configuration.isAutoConnectOnStartup());

		chckbxEnableUPnp = new JCheckBox("Enable uPNP");
		GridBagConstraints gbc_chckbxEnableUPNP = new GridBagConstraints();
		gbc_chckbxEnableUPNP.anchor = GridBagConstraints.WEST;
		gbc_chckbxEnableUPNP.gridwidth = 2;
		gbc_chckbxEnableUPNP.insets = new Insets(0, 0, 0, 5);
		gbc_chckbxEnableUPNP.gridx = 0;
		gbc_chckbxEnableUPNP.gridy = 2;
		pnlConnect.add(chckbxEnableUPnp, gbc_chckbxEnableUPNP);
		chckbxEnableUPnp.setSelected(configuration.isUpnpEnabled());

		return pnlNetworkSettings;
	}

	public void updateValues() {
		String encoder = "";
		switch (liveShiftGUI.getLiveShiftApplication().getConfiguration().getEncoderName()) {
			case Vlcj:
				encoder = "Integrated VLC (hidden)";
				break;
			case ExtVlc_hidden:
				encoder = "Integrated VLC (hidden)";
				break;
			case ExtVlc_visible:
				encoder = "External VLC (visible)";
				break;
			case Dummy:
				encoder = "Dummy";
		}
		cmbEncoder.setSelectedItem(encoder);

		Configuration configuration = liveShiftGUI.getLiveShiftApplication().getConfiguration();

		txtEncoderPort.setText(Integer.toString(configuration.getEncoderPort()));

		String player = "";
		switch (liveShiftGUI.getLiveShiftApplication().getConfiguration().getPlayerName()) {
			case ExtVlc:
				player = "External VLC";
				break;
			case Vlcj:
				player = "Integrated VLC";
				break;
			case Dummy:
				player = "Dummy Player (for testing)";
		}
		cmbPlayer.setSelectedItem(player);

		txtPlayerPort.setText(Integer.toString(configuration.getPlayerPort()));

		txtVlcPath.setText(liveShiftGUI.getLiveShiftApplication().getConfiguration().getVlcPath());
		txtUserVlcLibPath.setText(liveShiftGUI.getLiveShiftApplication().getConfiguration().getUserVlcLibPath());

		txtStorageFolder.setText(configuration.getStorageDir());

		txtMaxStoragesizeBlocks.setText(Long.toString(liveShiftGUI.getLiveShiftApplication().getConfiguration().getStorageSizeLimit()));

		chckbxAllowDataCollection.setSelected(configuration.isAllowDataCollection());

		cmbInterface.setSelectedItem(configuration.getNetworkInterface());

		txtP2pPort.setText(Integer.toString(configuration.getP2pPort()));

		txtPeerName.setText(configuration.getPeerName());

		Integer uploadRate = configuration.getUploadRate();
		if (uploadRate!=null) {
			txtUploadRate.setText(configuration.getUploadRate().toString());
		}

		txtNtpServer.setText(configuration.getNtpServer());

		txtBootstrapPeer.setText(configuration.getBootstrapAddress());

		chckbxConnectAtStartup.setSelected(configuration.isAutoConnectOnStartup());
	}

	private boolean unsavedChanges() {
		// TODO every setting
		if (txtBootstrapPeer.getText().compareTo(liveShiftGUI.getLiveShiftApplication().getConfiguration().getBootstrapAddress()) != 0)
			return true;
		else
			return false;
	}

	private void saveChanges() {
		LiveShiftApplication application = liveShiftGUI.getLiveShiftApplication();
		Configuration configuration = application.getConfiguration();

		configuration.setVlcPath(txtVlcPath.getText());
		configuration.setUserVlcLibPath(txtUserVlcLibPath.getText());
		configuration.setNetworkInterface(cmbInterface.getSelectedItem().toString());
		configuration.setP2pPort(Integer.parseInt(txtP2pPort.getText()));
		configuration.setPeerName(txtPeerName.getText());
		if (!txtUploadRate.getText().equals("")) {
			configuration.setUploadRate(Integer.valueOf(txtUploadRate.getText()));
		}
		configuration.setNtpServer(txtNtpServer.getText());
		configuration.setBootstrapAddress(txtBootstrapPeer.getText());
		configuration.setAutoConnectOnStartup(chckbxConnectAtStartup.isSelected());
		configuration.setUpnpEnabled(chckbxEnableUPnp.isSelected());
		configuration.setPlayerPort(Integer.parseInt(txtPlayerPort.getText()));

		// encoder
		if (cmbEncoder.getSelectedItem().toString().compareTo("External VLC (hidden)") == 0)
			configuration.setEncoderName(EncoderName.ExtVlc_hidden);
		else if (cmbEncoder.getSelectedItem().toString().compareTo("External VLC (visible)") == 0)
			configuration.setEncoderName(EncoderName.ExtVlc_visible);
		else if (cmbEncoder.getSelectedItem().toString().compareTo("Integrated VLC (hidden)") == 0)
			configuration.setEncoderName(EncoderName.Vlcj);
		else
			configuration.setEncoderName(EncoderName.Dummy);

		configuration.setEncoderPort(Integer.parseInt(txtEncoderPort.getText()));

		// player
		if (cmbPlayer.getSelectedItem().toString().compareTo("External VLC") == 0) {
			configuration.setPlayerName(PlayerName.ExtVlc);
		} else if (cmbPlayer.getSelectedItem().toString().compareTo("Integrated VLC") == 0) {
			configuration.setPlayerName(PlayerName.Vlcj);
		} else if (cmbPlayer.getSelectedItem().toString().compareTo("Dummy Player (for testing)") == 0) {
			configuration.setPlayerName(PlayerName.Dummy);
		}

		// player protocol
		for (StreamingProtocol playerProtocol : StreamingProtocol.values()) {
			if (cmbPlayerProtocol.getSelectedItem().toString().equals(playerProtocol.name())) {
				configuration.setPlayerProtocol(playerProtocol);
				break;
			}
		}

		long storageSizeLimit = Long.parseLong(txtMaxStoragesizeBlocks.getText());
		configuration.setStorageSizeLimit(storageSizeLimit);
		configuration.setStorageDir(txtStorageFolder.getText());
		configuration.setAllowDataCollection(chckbxAllowDataCollection.isSelected());

		configuration.saveUserConfiguration();

		logger.info("settings changed");
	}

	private void close() {
		dispose();
	}

	public void showTab(int tab) {
		this.tabbedPane.setSelectedIndex(tab);
	}

}
