package net.liveshift.gui;

import java.awt.Color;

import javax.swing.Icon;
import javax.swing.JButton;

public class LiveShiftButton extends JButton {

	/**
	 * 
	 */
	private static final long serialVersionUID = 817728939057808299L;

	public LiveShiftButton(Icon buttonIcon, boolean enabled) {
		super(buttonIcon);
		this.setBorder(null);
		this.setOpaque(false);
		this.setContentAreaFilled(false);
		this.setBorderPainted(false);
		this.setBackground(new Color(0, 0, 0, 0));
		this.setEnabled(enabled);
	}
}
