package net.liveshift.gui;

import java.awt.Graphics;

import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VolumeBar extends JPanel {
	
	final private static Logger logger = LoggerFactory.getLogger(VolumeBar.class);
			
	private static final long serialVersionUID = 248869861945500910L;
	
	private float volumePercent;
	
	public VolumeBar() {
		this.setOpaque(false);
	}
	
	public void setVolumeLevel(float volumePercent) {
		this.volumePercent = volumePercent;
		this.repaint();
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		float height = this.getHeight();
		int divideDesc = (int)((height/100)*(100-volumePercent));
		int divideAsc = (int)((height/100)*(volumePercent));
		
		if (logger.isDebugEnabled()) {
			logger.debug(divideDesc+"|"+divideAsc);
		}

		if (divideAsc < 2) {
			
			g.setColor(Design.TEXT_COLOR_LIGHT);
			g.fillRoundRect(0, 0, this.getWidth(), this.getHeight(),2,2);
			
			if (volumePercent<100) {
				g.setColor(Design.TEXT_COLOR_DARK);
				g.fillRoundRect(0, 0, this.getWidth(), divideDesc,2,2);
				g.fillRect(0, 2, this.getWidth(), divideDesc);
			}
		}
		else {
			
			g.setColor(Design.TEXT_COLOR_DARK);
			g.fillRoundRect(0, 0, this.getWidth(), this.getHeight(),2,2);
			
			if (volumePercent>0) {
				g.setColor(Design.TEXT_COLOR_LIGHT);
				g.fillRoundRect(0, divideDesc, this.getWidth(), divideAsc,2,2);
				
				g.fillRect(0, divideDesc, this.getWidth(), divideAsc-2);
			}
			
		}
	}
}
