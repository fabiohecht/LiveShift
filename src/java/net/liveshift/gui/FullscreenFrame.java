package net.liveshift.gui;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.MouseInfo;
import java.awt.Point;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;
import javax.swing.JPanel;

import net.liveshift.time.Clock;
import net.liveshift.util.ExecutorPool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FullscreenFrame extends JFrame implements PlayerPanel {
	
	final private static Logger logger = LoggerFactory.getLogger(FullscreenFrame.class);

	private static final long serialVersionUID = 8708690520714945935L;

	protected static final long CONTROL_AUTOHIDE_TIMEOUT_MILLIS = 5000L;
	
	final PlayControlPanel playControlPanel;
	private Canvas canvas;

	private boolean autohideControls = false;
	private boolean controlsShown = true;
	private long mouseMovedSinceMillis = 0L;
	private Point lastMouseLocation;

	public FullscreenFrame(final LiveShiftGUI liveShiftGUI) {
		
		this.setIconImages(Design.getAppIcons());

		//gradient background
        JPanel backgroundPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics grphcs) {
            	
            	Design.paintGradientBackground(grphcs, getWidth(), getHeight());
            	
                super.paintComponent(grphcs);
            }

        };
        backgroundPanel.setOpaque(false);
        backgroundPanel.setLayout(new BorderLayout());
		
		this.playControlPanel = new PlayControlPanel(liveShiftGUI, this);
		
		this.setLayout(new BorderLayout());

		canvas = new Canvas();
		canvas.setBackground(Color.BLACK);

        this.setContentPane(backgroundPanel);
		backgroundPanel.add(canvas, BorderLayout.CENTER);
		backgroundPanel.add(playControlPanel, BorderLayout.SOUTH);
		
		ExecutorPool.getScheduledExecutorService().scheduleAtFixedRate(controlsAutohide, 400, 400, TimeUnit.MILLISECONDS);

		/*
		 * does not get triggered from the VLC canvas :( so a workaround was implemented with MouseInfo.getPointerInfo().getLocation()
		this.addMouseMotionListener(new MouseMotionListener() {


			@Override
			public void mouseMoved(MouseEvent arg0) {
				logger.debug("mm");
				if (controlsShown) {
					mouseMovedSinceMillis = Clock.getMainClock().getTimeInMillis(false);
				} 
				else {
					setControlsVisibility(true);
				}
				
			}
		});
		*/
	}
	
	private Runnable controlsAutohide = new Runnable() {
		
		@Override
		public void run() {
			try {
				checkMouseMove();
			}
			catch (Exception e) {
				e.printStackTrace();
				logger.error("on controlsAutohide: "+e.getMessage());
			}
		}


	};
	
	void checkMouseMove() {
		
		if (autohideControls) {
			
			Point mouseLocation = MouseInfo.getPointerInfo().getLocation();
			
			if (lastMouseLocation==null || !lastMouseLocation.equals(mouseLocation)) {
				//mouse moved
				if (controlsShown) {
					mouseMovedSinceMillis = Clock.getMainClock().getTimeInMillis(false);
					if (logger.isDebugEnabled()) {
						logger.debug("mouse moved");
					}
				} 
				else {
					setControlsVisibility(true);
				}
			}
			else if (controlsShown) {
				//mouse didn't move
				long mouseInactivityTimeMillis = Clock.getMainClock().getTimeInMillis(false) - mouseMovedSinceMillis;
				if (logger.isDebugEnabled()) {
					logger.debug("controlsAutohide: "+autohideControls+","+controlsShown+","+mouseInactivityTimeMillis);
				}
		
				if (mouseInactivityTimeMillis > CONTROL_AUTOHIDE_TIMEOUT_MILLIS) {
					//auto hide
					setControlsVisibility(false);
				}
			}
			
			lastMouseLocation = mouseLocation;
		}
	}
		
	private void setControlsVisibility(boolean visibility) {
		logger.debug("setControlsVisibility: "+visibility);

		this.playControlPanel.setVisible(visibility);
		
		this.controlsShown=visibility;
	}

	public void setFullscreen(boolean fullscreen) {
		setVisible(false);
		if (fullscreen) {
			dispose();
	
	 		this.setUndecorated(true);
	 		this.setResizable(false);
	 		this.validate();
	 
	 		GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().setFullScreenWindow(this);
	 		
	 		setVisible(true);
		}
		else {
			
		}
	}

	@Override
	public void setPlaying(boolean playing) {
		this.autohideControls = playing;
	}

	public Canvas getVideoCanvas() {
		return this.canvas;
	}

	public PlayControlPanel getPlayControlPanel() {
		return this.playControlPanel;
	}

}
