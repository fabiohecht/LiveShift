package net.liveshift.gui;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JSpinner.DateEditor;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.border.EmptyBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.liveshift.core.LiveShiftApplication;
import net.liveshift.core.UiListener;

import javax.swing.border.LineBorder;

public class CenterPanel extends JPanel implements PlayerPanel {

	private static final Logger logger = LoggerFactory.getLogger(CenterPanel.class);

	private static final long serialVersionUID = -2224668342419877772L;

	private final LiveShiftGUI liveShiftGUI;

	private JPanel screenPanel;
	private PlayControlPanel playControlPanel;

	private Canvas canvas;

	private Image logoImage = new ImageIcon(LiveShiftGUI.class.getResource(Design.LOGO_BACKGROUND_IMAGE_FILE)).getImage();


	public CenterPanel(final LiveShiftGUI liveShiftGUI) {
		this.liveShiftGUI = liveShiftGUI;
		
		this.setOpaque(false);
		this.setLayout(new BorderLayout());
		
		screenPanel = new JPanel();
		screenPanel.setOpaque(false);
		screenPanel.setLayout(new BoxLayout(screenPanel, BoxLayout.Y_AXIS));
		screenPanel.add(Box.createVerticalStrut(10));
		screenPanel.setBorder(new EmptyBorder(3,3,3,3));
		screenPanel.setVisible(false);  //will be visible when playing
		
		canvas = new Canvas();
		canvas.setBackground(Color.BLACK);

		screenPanel.add(canvas);
		
		this.add(screenPanel, BorderLayout.CENTER);
		
		playControlPanel = new PlayControlPanel(this.liveShiftGUI, this);
		
		this.add(playControlPanel, BorderLayout.SOUTH);
	}

    @Override  
    public void paintComponent(Graphics g){
    	super.paintComponent(g);

    	int width = this.getWidth()-10;
    	int height = this.getHeight()-10-(this.playControlPanel.isVisible()?40:0);
    	
    	if (this.screenPanel.isVisible()) {
	    	g.setColor(Color.BLACK);
	        g.fillRoundRect(0, 10, width, height, 10, 10);
	        int canvasWidth = width-3;
	        int canvasHeight = height-3;
	        
	        canvas.setSize(canvasWidth, canvasHeight);
	        canvas.setPreferredSize(new Dimension(canvasWidth, canvasHeight));
	        canvas.setMaximumSize(new Dimension(canvasWidth, canvasHeight));
	        
    	}
    	else {
	    	g.setColor(Color.WHITE);
	        g.fillRoundRect(0, 10, width, height, 10, 10);
	        g.drawImage(logoImage, width / 2 - logoImage.getWidth(null) / 2, height / 2 - logoImage.getHeight(null) / 2, this);
    	}
    }

	public Canvas getVideoCanvas() {
		return canvas;
	}

	public void setFullscreen(boolean fullscreen) {
		if (fullscreen)
			this.setBounds(liveShiftGUI.getAppBounds(LiveShiftGUI.SubAppPosition.ALL));
		else
			this.setBounds(liveShiftGUI.getAppBounds(LiveShiftGUI.SubAppPosition.MIDDLE));
	}

	public void setButtonsVisible(boolean buttonsVisible) {
		this.playControlPanel.setVisible(buttonsVisible);
	}
	public void setScreenVisible(boolean screenVisible) {
		this.screenPanel.setVisible(screenVisible);

		this.repaint();
	}

	@Override
	public void setPlaying(boolean playing) {
		screenPanel.setVisible(playing);
	}

	public PlayControlPanel getPlayControlPanel() {
		return this.playControlPanel;
	}
}
