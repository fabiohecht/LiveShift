package net.liveshift.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;

import net.liveshift.configuration.Configuration;
import net.liveshift.configuration.PublishConfiguration;



public class PublishFrame extends JDialog {

	final private PublishConfiguration publishConfiguration;
	final private LiveShiftGUI liveShiftGUI;

	private static final long serialVersionUID = -2755217973681141150L;
	private JPanel contentPane;
	private JTextField txtChannelName;
	private JTextField txtSourceFile;
	private JTextField txtSourceDevice;
	private JTextField txtSourceNetwork;
	private JTextField txtVlcCommand;
	private JSpinner spnSubstreams;
	private JTextArea txtDescription;
	private JRadioButton rdbtnSourceFile;
	private JRadioButton rdbtnSourceDevice;
	private JRadioButton rdbtnSourceNetwork;
	private JCheckBox cbFileLoop;

	private JFileChooser fcSourceFile = new JFileChooser();

	/**
	 * Create the frame.
	 */
	public PublishFrame(final LiveShiftGUI liveShiftGUI, final PublishConfiguration publishConfiguration) {

		this.publishConfiguration = publishConfiguration;
		this.liveShiftGUI = liveShiftGUI;

		setTitle("LiveShift - Publish Channel");
		setBounds(100, 100, 500, 400);
		this.setLocationRelativeTo(null);
		this.setModalityType(ModalityType.APPLICATION_MODAL);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

		JPanel channelInformationPanel = new JPanel();
		channelInformationPanel.setBorder(new TitledBorder(null, "Channel Information", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		contentPane.add(channelInformationPanel);
		GridBagLayout gbl_panel = new GridBagLayout();
		gbl_panel.columnWidths = new int[] { 0, 0, 0, 0, 0 };
		gbl_panel.rowHeights = new int[] { 0, 0, 0 };
		gbl_panel.columnWeights = new double[] { 0.0, 1.0, 0.0, 0.0, Double.MIN_VALUE };
		gbl_panel.rowWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		channelInformationPanel.setLayout(gbl_panel);

		JLabel lblName = new JLabel("Name");
		GridBagConstraints gbc_lblName = new GridBagConstraints();
		gbc_lblName.insets = new Insets(0, 0, 5, 5);
		gbc_lblName.anchor = GridBagConstraints.EAST;
		gbc_lblName.gridx = 0;
		gbc_lblName.gridy = 0;
		channelInformationPanel.add(lblName, gbc_lblName);

		txtChannelName = new JTextField();
		GridBagConstraints gbc_txtChannelName = new GridBagConstraints();
		gbc_txtChannelName.insets = new Insets(0, 0, 5, 5);
		gbc_txtChannelName.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtChannelName.gridx = 1;
		gbc_txtChannelName.gridy = 0;
		channelInformationPanel.add(txtChannelName, gbc_txtChannelName);
		txtChannelName.setText(this.publishConfiguration.getName());

		JLabel lblSubstreams = new JLabel("Substreams");
		GridBagConstraints gbc_lblSubstreams = new GridBagConstraints();
		gbc_lblSubstreams.insets = new Insets(0, 0, 5, 5);
		gbc_lblSubstreams.gridx = 2;
		gbc_lblSubstreams.gridy = 0;
		channelInformationPanel.add(lblSubstreams, gbc_lblSubstreams);

		spnSubstreams = new JSpinner();
		spnSubstreams.setModel(new SpinnerNumberModel((int)this.publishConfiguration.getSubstream(), 1, Configuration.MAX_NUMBER_OF_SUBSTREAMS, 1));
		GridBagConstraints gbc_spinner = new GridBagConstraints();
		gbc_spinner.insets = new Insets(0, 0, 5, 5);
		gbc_spinner.gridx = 3;
		gbc_spinner.gridy = 0;
		channelInformationPanel.add(spnSubstreams, gbc_spinner);

		JLabel lblDescription = new JLabel("Description");
		GridBagConstraints gbc_lblDescription = new GridBagConstraints();
		gbc_lblDescription.anchor = GridBagConstraints.EAST;
		gbc_lblDescription.insets = new Insets(0, 0, 0, 5);
		gbc_lblDescription.gridx = 0;
		gbc_lblDescription.gridy = 1;
		channelInformationPanel.add(lblDescription, gbc_lblDescription);

		txtDescription = new JTextArea();
		GridBagConstraints gbc_textArea = new GridBagConstraints();
		gbc_textArea.gridwidth = 3;
		gbc_textArea.insets = new Insets(0, 0, 0, 5);
		gbc_textArea.fill = GridBagConstraints.BOTH;
		gbc_textArea.gridx = 1;
		gbc_textArea.gridy = 1;
		JScrollPane scpChannelDescription = new JScrollPane(txtDescription);
		channelInformationPanel.add(scpChannelDescription, gbc_textArea);
		txtDescription.setText(this.publishConfiguration.getDescription());

		JPanel sourcePanel = new JPanel();
		sourcePanel.setBorder(new TitledBorder(null, "Source", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		contentPane.add(sourcePanel);
		GridBagLayout gbl_panel_1 = new GridBagLayout();
		gbl_panel_1.columnWidths = new int[] { 0, 0, 0, 0 };
		gbl_panel_1.rowHeights = new int[] { 0, 0, 0 };
		gbl_panel_1.columnWeights = new double[] { 0.0, 1.0, 0.0, 0.0 };
		gbl_panel_1.rowWeights = new double[] { 0.0, 0.0, 0.0 };
		sourcePanel.setLayout(gbl_panel_1);

		//source: file
		rdbtnSourceFile = new JRadioButton("File");
		rdbtnSourceFile.setSelected(true);
		GridBagConstraints gbc_rdbtnSourceFile = new GridBagConstraints();
		gbc_rdbtnSourceFile.anchor = GridBagConstraints.WEST;
		gbc_rdbtnSourceFile.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnSourceFile.gridx = 0;
		gbc_rdbtnSourceFile.gridy = 0;
		sourcePanel.add(rdbtnSourceFile, gbc_rdbtnSourceFile);

		txtSourceFile = new JTextField();
		GridBagConstraints gbc_txtSourceFile = new GridBagConstraints();
		gbc_txtSourceFile.insets = new Insets(0, 0, 5, 5);
		gbc_txtSourceFile.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtSourceFile.gridx = 1;
		gbc_txtSourceFile.gridy = 0;
		sourcePanel.add(txtSourceFile, gbc_txtSourceFile);
		txtSourceFile.getDocument().addDocumentListener(new DocumentListener() {
			
			@Override
			public void removeUpdate(DocumentEvent e) {
				rdbtnSourceFile.setSelected(true);
			}
			
			@Override
			public void insertUpdate(DocumentEvent e) {
				rdbtnSourceFile.setSelected(true);
			}
			
			@Override
			public void changedUpdate(DocumentEvent e) {
				rdbtnSourceFile.setSelected(true);
			}
		});

		if (this.publishConfiguration.getFileSource() != null) {
			txtSourceFile.setText(this.publishConfiguration.getFileSource().getAbsolutePath());
		}

		JButton btnSelectFile = new JButton("Select File");
		btnSelectFile.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				fcSourceFile.setFileFilter(new FileFilter() {

					@Override
					public String getDescription() {
						return "*.avi;*.mpg;*.mpeg";
					}

					@Override
					public boolean accept(File f) {
						if (f.isDirectory())
							return true;
						String name = f.getName();
						int pos = name.lastIndexOf('.');
						String ext = name.substring(pos + 1).toLowerCase();
						return ext.compareTo("avi") == 0 || ext.compareTo("mpg") == 0 || ext.compareTo("mpeg") == 0;
					}
				});
				fcSourceFile.showOpenDialog(new JFrame());
				File selFile = fcSourceFile.getSelectedFile();
				if (selFile != null) {
					txtSourceFile.setText(selFile.getAbsolutePath());
					rdbtnSourceFile.setSelected(true);
				}
			}
		});
		GridBagConstraints gbc_btnSelectFile = new GridBagConstraints();
		gbc_btnSelectFile.insets = new Insets(0, 0, 5, 5);
		gbc_btnSelectFile.gridx = 2;
		gbc_btnSelectFile.gridy = 0;
		sourcePanel.add(btnSelectFile, gbc_btnSelectFile);

		cbFileLoop = new JCheckBox("Loop file");
		GridBagConstraints gbc_cbFileLoop = new GridBagConstraints();
		gbc_cbFileLoop.gridwidth = 2;
		gbc_cbFileLoop.insets = new Insets(0, 0, 5, 5);
		gbc_cbFileLoop.anchor = GridBagConstraints.NORTHWEST;
		gbc_cbFileLoop.gridx = 3;
		gbc_cbFileLoop.gridy = 0;
		sourcePanel.add(cbFileLoop, gbc_cbFileLoop);

		//source: device

		rdbtnSourceDevice = new JRadioButton("Device");
		GridBagConstraints gbc_rdbtnSourceDevice = new GridBagConstraints();
		gbc_rdbtnSourceDevice.anchor = GridBagConstraints.WEST;
		gbc_rdbtnSourceDevice.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnSourceDevice.gridx = 0;
		gbc_rdbtnSourceDevice.gridy = 1;
		sourcePanel.add(rdbtnSourceDevice, gbc_rdbtnSourceDevice);

		txtSourceDevice = new JTextField();
		GridBagConstraints gbc_txtSourceDevice = new GridBagConstraints();
		gbc_txtSourceDevice.insets = new Insets(0, 0, 5, 5);
		gbc_txtSourceDevice.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtSourceDevice.gridwidth = 2;
		gbc_txtSourceDevice.gridx = 1;
		gbc_txtSourceDevice.gridy = 1;
		sourcePanel.add(txtSourceDevice, gbc_txtSourceDevice);
		txtSourceDevice.setText(this.publishConfiguration.getDeviceSource());
		txtSourceDevice.getDocument().addDocumentListener(new DocumentListener() {
			
			@Override
			public void removeUpdate(DocumentEvent e) {
				rdbtnSourceDevice.setSelected(true);
			}
			
			@Override
			public void insertUpdate(DocumentEvent e) {
				rdbtnSourceDevice.setSelected(true);
			}
			
			@Override
			public void changedUpdate(DocumentEvent e) {
				rdbtnSourceDevice.setSelected(true);
			}
		});
		
		JLabel lblDeviceHint = new JLabel("e.g. v4l2:///dev/video0");
		GridBagConstraints gbc_lblDeviceHint = new GridBagConstraints();
		gbc_lblDeviceHint.insets = new Insets(0, 0, 5, 5);
		gbc_lblDeviceHint.anchor = GridBagConstraints.WEST;
		gbc_lblDeviceHint.gridx = 3;
		gbc_lblDeviceHint.gridy = 1;
		sourcePanel.add(lblDeviceHint, gbc_lblDeviceHint);

		
		//source: network
		
		rdbtnSourceNetwork = new JRadioButton("Network");
		GridBagConstraints gbc_rdbtnSourceNetwork = new GridBagConstraints();
		gbc_rdbtnSourceNetwork.anchor = GridBagConstraints.WEST;
		gbc_rdbtnSourceNetwork.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnSourceNetwork.gridx = 0;
		gbc_rdbtnSourceNetwork.gridy = 2;
		sourcePanel.add(rdbtnSourceNetwork, gbc_rdbtnSourceNetwork);

		ButtonGroup btgSource = new ButtonGroup();
		btgSource.add(rdbtnSourceFile);
		btgSource.add(rdbtnSourceDevice);
		btgSource.add(rdbtnSourceNetwork);

		txtSourceNetwork = new JTextField();
		GridBagConstraints gbc_txtSourceNetwork = new GridBagConstraints();
		gbc_txtSourceNetwork.insets = new Insets(0, 0, 5, 5);
		gbc_txtSourceNetwork.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtSourceNetwork.gridwidth = 2;
		gbc_txtSourceNetwork.gridx = 1;
		gbc_txtSourceNetwork.gridy = 2;
		sourcePanel.add(txtSourceNetwork, gbc_txtSourceNetwork);
		txtSourceNetwork.setText(this.publishConfiguration.getNetworkSource());
		txtSourceNetwork.getDocument().addDocumentListener(new DocumentListener() {
			
			@Override
			public void removeUpdate(DocumentEvent e) {
				rdbtnSourceNetwork.setSelected(true);
			}
			
			@Override
			public void insertUpdate(DocumentEvent e) {
				rdbtnSourceNetwork.setSelected(true);
			}
			
			@Override
			public void changedUpdate(DocumentEvent e) {
				rdbtnSourceNetwork.setSelected(true);
			}
		});

		JLabel lblNetworkHint = new JLabel("e.g. multicast address");
		GridBagConstraints gbc_lblNetworkHint = new GridBagConstraints();
		gbc_lblNetworkHint.insets = new Insets(0, 0, 5, 5);
		gbc_lblNetworkHint.anchor = GridBagConstraints.WEST;
		gbc_lblNetworkHint.gridx = 3;
		gbc_lblNetworkHint.gridy = 2;
		sourcePanel.add(lblNetworkHint, gbc_lblNetworkHint);

		
		JPanel pnlPublishSettings = new JPanel();
		pnlPublishSettings.setBorder(new TitledBorder(null, "Publish Settings", TitledBorder.LEADING, TitledBorder.TOP, null, null));

		GridBagLayout gbl_panel_3 = new GridBagLayout();
		gbl_panel_3.columnWidths = new int[] { 0, 0 };
		gbl_panel_3.rowHeights = new int[] { 0 };
		gbl_panel_3.columnWeights = new double[] { 0.0, 1.0 };
		gbl_panel_3.rowWeights = new double[] { 0.0 };
		pnlPublishSettings.setLayout(gbl_panel_3);

		JLabel lblVlcCommand = new JLabel("VLC Command");
		GridBagConstraints gbc_lblVlcCommand = new GridBagConstraints();
		gbc_lblVlcCommand.anchor = GridBagConstraints.WEST;
		gbc_lblVlcCommand.insets = new Insets(0, 0, 0, 5);
		gbc_lblVlcCommand.gridx = 0;
		gbc_lblVlcCommand.gridy = 0;
		pnlPublishSettings.add(lblVlcCommand, gbc_lblVlcCommand);

		txtVlcCommand = new JTextField();
		GridBagConstraints gbc_txtVlcCommand = new GridBagConstraints();
		gbc_txtVlcCommand.fill = GridBagConstraints.WEST;
		gbc_txtVlcCommand.insets = new Insets(0, 0, 0, 5);
		gbc_txtVlcCommand.gridx = 1;
		gbc_txtVlcCommand.gridy = 0;
		gbc_txtVlcCommand.fill = GridBagConstraints.HORIZONTAL;
		pnlPublishSettings.add(txtVlcCommand, gbc_txtVlcCommand);
		txtVlcCommand.setText(this.publishConfiguration.getVlcParameters());

		contentPane.add(pnlPublishSettings);

		contentPane.add(Box.createVerticalGlue());
		
		JPanel buttonPanel = new JPanel();
		contentPane.add(buttonPanel);
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

		buttonPanel.add(Box.createHorizontalGlue());

		JButton btnClose = new JButton("      Close     ");
		btnClose.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				close();
			}
		});
		buttonPanel.add(btnClose);
		buttonPanel.add(Box.createHorizontalStrut(5));
		
		JButton btnPublish = new JButton("    Publish     ");
		btnPublish.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				publishConfiguration.setDescription(txtDescription.getText());
				publishConfiguration.setVlcParameters(txtVlcCommand.getText());
				publishConfiguration.setFileLoop(cbFileLoop.isSelected());

				publishConfiguration.setName(txtChannelName.getText());
				publishConfiguration.setSubstream(Byte.parseByte(spnSubstreams.getValue().toString()));

				String encMsg = liveShiftGUI.checkVlcEncoder();
				if (encMsg == "") {
					
					if (rdbtnSourceFile.isSelected()) {
						publishConfiguration.setFileSource(new File(txtSourceFile.getText()));
						liveShiftGUI.publishChannelFile(publishConfiguration.getName(),	publishConfiguration.getSubstream(), publishConfiguration.getDescription(), publishConfiguration.getFileSource().getAbsolutePath());
					}
					else if (rdbtnSourceDevice.isSelected()) {						
						publishConfiguration.setDeviceSource(txtSourceDevice.getText());
						liveShiftGUI.publishChannelDevice(publishConfiguration.getName(), publishConfiguration.getSubstream(), publishConfiguration.getDescription(), "v4l2://" + publishConfiguration.getDeviceSource());
					}
					else if (rdbtnSourceNetwork.isSelected()) {
						publishConfiguration.setNetworkSource(txtSourceNetwork.getText());
						liveShiftGUI.publishChannelNetwork(publishConfiguration.getName(), publishConfiguration.getSubstream(), publishConfiguration.getDescription(), "", publishConfiguration.getNetworkSource());
					}
					else {
						JOptionPane.showMessageDialog(contentPane, "Please select a file or device to publish.");
					}
		
					close();
				}
				else {
					JOptionPane.showMessageDialog(liveShiftGUI.getPublishFrame(), "Unable to start encoder.\n" + encMsg);
				}
			}
		});
		buttonPanel.add(btnPublish);
		buttonPanel.add(Box.createHorizontalStrut(5));

	}

	private void close() {
		dispose();
	}
}
