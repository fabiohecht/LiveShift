package net.liveshift.gui;

import java.awt.Color;
import java.awt.GridBagConstraints;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;


public class HelpFrame extends JFrame {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2409089076602336397L;

	public HelpFrame() {
		this.setTitle("LiveShift - Help");

		this.setBounds(Design.getCenteredPosition(320, 260));
		
		JPanel wrapper = new JPanel();
		wrapper.setBackground(new Color(0, 0, 0));
		
//		GridBagLayout gbl_helpPanel = new GridBagLayout();
//		gbl_helpPanel.columnWidths = new int[]{50, 400, 50};
//		gbl_helpPanel.columnWeights = new double[]{0.0, 0.0, 0.0};
//		gbl_helpPanel.rowHeights = new int[]{60, 60, 60};
//		gbl_helpPanel.rowWeights = new double[]{0.0, 0.0, 0.0};
//		wrapper.setLayout(gbl_helpPanel);
		
		JLabel networkIconLbl = new JLabel();
		GridBagConstraints gbc_networkIconLbl = new GridBagConstraints();
		gbc_networkIconLbl.gridx = 0;
		gbc_networkIconLbl.gridy = 0;
		wrapper.add(networkIconLbl, gbc_networkIconLbl);
		
		JLabel networkLbl = new JLabel("If not connected this button tries to connect you to the currently defined network.");
		networkLbl.setForeground(Design.TEXT_COLOR_LIGHT);
		GridBagConstraints gbc_networkLbl = new GridBagConstraints();
		gbc_networkLbl.gridx = 1;
		gbc_networkLbl.gridy = 0;
		wrapper.add(networkLbl, gbc_networkLbl);
		
		JLabel publicIconLbl = new JLabel();
		GridBagConstraints gbc_publicIconLbl = new GridBagConstraints();
		gbc_publicIconLbl.gridx = 0;
		gbc_publicIconLbl.gridy = 1;
		wrapper.add(publicIconLbl, gbc_publicIconLbl);
		
		JLabel publishLbl = new JLabel("If not connected this button tries to connect you to the currently defined network.");
		publishLbl.setForeground(Design.TEXT_COLOR_LIGHT);
		GridBagConstraints gbc_publishLbl = new GridBagConstraints();
		gbc_publishLbl.gridx = 1;
		gbc_publishLbl.gridy = 1;
		wrapper.add(publishLbl, gbc_publishLbl);
		
		this.add(wrapper);
	}
}
