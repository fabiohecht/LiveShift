package net.liveshift.gui;

import java.awt.FlowLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.liveshift.core.LiveShiftApplication.PlaybackStatus;
import net.liveshift.core.LiveShiftApplication.PlayerStatus;
import net.liveshift.time.Clock;
import net.liveshift.util.Utils;


public class StatusPanel extends JPanel {
	private static final long serialVersionUID = -6583553481212701442L;
	
	private JLabel networkLbl;
	private JLabel publishLbl;
	private JLabel playingLbl;

	public StatusPanel(){
		//this.setBackground(new Color(0, 0, 0));
		this.setOpaque(false);
		this.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 5));
		
		networkLbl = new JLabel("Network: Not connected");
		networkLbl.setForeground(Design.TEXT_COLOR_LIGHT);		
		publishLbl = new JLabel("Publish: Not publishing");
		publishLbl.setForeground(Design.TEXT_COLOR_LIGHT);		
		playingLbl = new JLabel("Playing: Not playing");
		playingLbl.setForeground(Design.TEXT_COLOR_LIGHT);
		
		this.add(networkLbl);
		this.add(publishLbl);
		this.add(playingLbl);
	}
	
	public void setNetworkText(String text) {
		networkLbl.setText("Network: " + text);
	}
	public void setPublishText(String text) {
		publishLbl.setText("Publish: " + text);
	}

	public void playingStateChanged(String channelName, PlayerStatus playerStatus, PlaybackStatus playbackStatus, long playTime) {
		String status = "Playing: "+Utils.shortenString(channelName,20)+" ";
		
		switch (playerStatus) {
			case PAUSED:
				status += "[PAUSE] ";
				status += "at " + Clock.formatDatetime(playTime);
				break;
			case STOPPED:
				status += "[STOP] ";
				break;
			case PLAYING:

				status += "[PLAY] ";

				if (playbackStatus==PlaybackStatus.STALLED) {
					status += "STALLED at ";
				}
				status += Clock.formatDatetime(playTime);
		}

		playingLbl.setText(status);
	}
}

