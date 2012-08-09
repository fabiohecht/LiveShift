package net.liveshift.gui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.liveshift.core.LiveShiftApplication;

public class PlayControlPanel extends JPanel {

	private static final Logger logger = LoggerFactory.getLogger(PlayControlPanel.class);

	private static final long serialVersionUID = -214614149481961778L;

	protected static final int FF_RW_TIME_SECONDS = 15;  //rewind and ff in seconds
	protected static final int VOLUME_INCREMENT = 10;  //in percentage out of the maximum volume possible
	private static final int SPACING = 4;

	final private LiveShiftGUI liveShiftGUI;
	private JButton pauseBtn;
	private JButton stopBtn;
	private JButton fastforwardBtn;
	private JButton rewindBtn;
	private JSpinner timeshiftDateTime;
	private JButton playLiveBtn;
	private JButton playTsBtn;
	private JButton fullScreenBtn;
	private JButton volumeUpBtn;
	private JButton volumeDownBtn;
	private VolumeBar volumeBar;

	public PlayControlPanel(final LiveShiftGUI liveShiftGUI, final PlayerPanel playerPanel) {

		this.liveShiftGUI = liveShiftGUI;
		
		this.setOpaque(false);
		this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

		this.add(Box.createHorizontalGlue());
		this.add(Box.createHorizontalStrut(SPACING));

		rewindBtn = new LiveShiftButton(Design.getInstance().getIcon(Design.ICON_REWIND), true);
		rewindBtn.setToolTipText("Rewind "+FF_RW_TIME_SECONDS+" seconds");
		rewindBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				liveShiftGUI.getLiveShiftApplication().rewind(FF_RW_TIME_SECONDS*1000);
			}
		});
		this.add(rewindBtn);
		this.add(Box.createHorizontalStrut(SPACING));

		fastforwardBtn = new LiveShiftButton(Design.getInstance().getIcon(
				Design.ICON_FASTFORWARD), true);
		fastforwardBtn.setToolTipText("Fast-forward "+FF_RW_TIME_SECONDS+" seconds");
		fastforwardBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				liveShiftGUI.getLiveShiftApplication().fastForward(FF_RW_TIME_SECONDS*1000);
			}
		});
		
		pauseBtn = new LiveShiftButton(Design.getInstance().getIcon(Design.ICON_PAUSE), true);
		pauseBtn.setToolTipText("Pause/resume");
		pauseBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				LiveShiftApplication application = liveShiftGUI.getLiveShiftApplication();
				liveShiftGUI.pause();
				
				logger.debug(application.getPlayerStatus().name());
				logger.debug(String.valueOf(application.getPlayTimeMillis()));
			} 
		});
		this.add(pauseBtn);
		this.add(Box.createHorizontalStrut(SPACING));

		stopBtn = new LiveShiftButton(Design.getInstance().getIcon(Design.ICON_STOP), true);
		stopBtn.setToolTipText("Stop");
		stopBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				playerPanel.setPlaying(false);

				LiveShiftApplication application = liveShiftGUI.getLiveShiftApplication();
				liveShiftGUI.stop();
				
				logger.debug(application.getPlayerStatus().name());
				logger.debug(String.valueOf(application.getPlayTimeMillis()));
			}
		});
		this.add(stopBtn);
		this.add(Box.createHorizontalStrut(SPACING));

		this.add(fastforwardBtn);
		this.add(Box.createHorizontalStrut(SPACING));

		playTsBtn = new LiveShiftButton(Design.getInstance().getIcon(Design.ICON_TIMESHIFT), true);
		playTsBtn.setToolTipText("Play time-shifted stream");
		playTsBtn.addActionListener(new ActionListener() {
			//play time shift
			@Override
			public void actionPerformed(ActionEvent arg0) {
				playerPanel.setPlaying(true);
				checkPlayer();

				Date timeShiftTime = (Date) timeshiftDateTime.getValue();
				
				liveShiftGUI.switchToSelectedChannel(timeShiftTime.getTime());
			}

		});
		
		playLiveBtn = new LiveShiftButton(Design.getInstance().getIcon(
				Design.ICON_PLAY), true);
		playLiveBtn.setToolTipText("Play live stream");
		playLiveBtn.addActionListener(new ActionListener() {
			//play live stream
			@Override
			public void actionPerformed(ActionEvent arg0) {
				playerPanel.setPlaying(true);
				checkPlayer();

				liveShiftGUI.switchToSelectedChannel(0);
			}
		});
		this.add(playLiveBtn);
		this.add(Box.createHorizontalStrut(SPACING));

		this.add(playTsBtn);
		this.add(Box.createHorizontalStrut(SPACING));
		
		timeshiftDateTime = new JSpinner();
		timeshiftDateTime.setModel(new SpinnerDateModel());
		timeshiftDateTime.setEditor(new JSpinner.DateEditor(timeshiftDateTime, "dd/MM/yyyy HH:mm:ss"));
		timeshiftDateTime.setOpaque(false);
		timeshiftDateTime.getEditor().setOpaque(false);
		timeshiftDateTime.setBorder(null);
		timeshiftDateTime.setBackground(null);
		timeshiftDateTime.setPreferredSize(new Dimension(150,25));
		timeshiftDateTime.setMaximumSize(new Dimension(150,25));
		this.add(timeshiftDateTime);
		this.add(Box.createHorizontalStrut(SPACING));
		
		fullScreenBtn = new LiveShiftButton(Design.getInstance().getIcon(Design.ICON_FULL_SCREEN), true);
		fullScreenBtn.setToolTipText("Full screen");
		fullScreenBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				liveShiftGUI.toggleFullScreen();
				logger.debug("setFullScreen");
			}
		});
		this.add(fullScreenBtn);
		this.add(Box.createHorizontalStrut(SPACING));

		volumeUpBtn = new LiveShiftButton(Design.getInstance().getIcon(Design.ICON_VOLUME_UP), true);
		volumeUpBtn.setToolTipText("Volume up");
		volumeUpBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				LiveShiftApplication application = liveShiftGUI.getLiveShiftApplication();
				
				application.setVolumeUp(VOLUME_INCREMENT);
				logger.debug("setVolumeUp("+VOLUME_INCREMENT+")");
			}
		});
		this.add(volumeUpBtn);
		this.add(Box.createHorizontalStrut(SPACING));

		volumeDownBtn = new LiveShiftButton(Design.getInstance().getIcon(Design.ICON_VOLUME_DOWN), true);
		volumeDownBtn.setToolTipText("Volume down");
		volumeDownBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				LiveShiftApplication application = liveShiftGUI.getLiveShiftApplication();
				
				application.setVolumeDown(VOLUME_INCREMENT);
				logger.debug("setVolumeDown("+VOLUME_INCREMENT+")");
			}
		});
		this.add(volumeDownBtn);
		this.add(Box.createHorizontalStrut(SPACING));

		volumeBar = new VolumeBar();
		volumeBar.setPreferredSize(new Dimension(5, 32));
		volumeBar.setMaximumSize(new Dimension(5, 32));
		//volumeBar.setAlignmentY(Component.BOTTOM_ALIGNMENT);
		this.add(volumeBar);
		
		this.add(Box.createHorizontalGlue());

	}

	public void checkPlayer() {
		String playMsg = liveShiftGUI.checkVlcPlayerUsage();
		if (playMsg != "") {
			JOptionPane.showMessageDialog(this, "Unable to start player.\n" + playMsg);
		}
	}

	public void setVolumeLevel(float volumePercent) {
		this.volumeBar.setVolumeLevel(volumePercent);
	}
}
