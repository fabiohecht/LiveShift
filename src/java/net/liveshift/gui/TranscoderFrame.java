package net.liveshift.gui;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.liveshift.core.LiveShiftApplication;


public class TranscoderFrame extends JFrame {
	
	private LiveShiftApplication liveShiftApplication;
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {

			public void run() {
				try {
					TranscoderFrame frame = new TranscoderFrame(new LiveShiftApplication());
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	/**
	 * Create the frame.
	 */
	public TranscoderFrame(LiveShiftApplication liveShiftApplication) {
		this.liveShiftApplication = liveShiftApplication;
		
		setTitle("LiveShift - Transcoder Settings");
		setBounds(100, 100, 500, 400);
		this.setLocationRelativeTo(null);
		
		JPanel panel = new JPanel();
		getContentPane().add(panel, BorderLayout.CENTER);
		
		JLabel lblVCodecLabel = new JLabel("VCodec");
		panel.add(lblVCodecLabel);
		
		JComboBox cbVCodec = new JComboBox();
		cbVCodec.addItem("");
		for(String key : liveShiftApplication.getPublishConfiguration().getTranscoder().getAvailableVCodecs().keySet()){
			cbVCodec.addItem(key);
		}
		cbVCodec.setSelectedItem(liveShiftApplication.getPublishConfiguration().getTranscoder().getVcodec());
		panel.add(cbVCodec);
	}
}
