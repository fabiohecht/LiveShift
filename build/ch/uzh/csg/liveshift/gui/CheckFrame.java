package net.liveshift.gui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Image;
import java.awt.Dialog.ModalityType;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.border.LineBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.co.caprica.vlcj.runtime.RuntimeUtil;
import net.liveshift.configuration.Configuration;
import net.liveshift.core.EnvironmentCheck;
import net.liveshift.util.Utils;

public class CheckFrame extends JDialog {
	private static final Logger logger = LoggerFactory.getLogger(CheckFrame.class);

	private Image logoImage = new ImageIcon(CheckFrame.class.getResource(Design.LOGO_SPLASH_IMAGE_FILE)).getImage();

	final private LiveShiftGUI liveShiftGUI;

	private static final long serialVersionUID = 4802699987672082216L;
	private JLabel checkInfoLbl;

	public CheckFrame(final LiveShiftGUI liveShiftGUI) {
		
		this.liveShiftGUI = liveShiftGUI;
		
		this.setUndecorated(true);
		this.setTitle("LiveShift");
		this.setBounds(Design.getCenteredPosition(550, 350));
		//this.setModalityType(ModalityType.APPLICATION_MODAL);
		this.setIconImages(Design.getAppIcons());

		this.setLayout(new BorderLayout());

		this.add(new JLabel(new ImageIcon(logoImage)), BorderLayout.CENTER);

		checkInfoLbl = new JLabel();
		checkInfoLbl.setHorizontalTextPosition(JLabel.CENTER);
		checkInfoLbl.setHorizontalAlignment(JLabel.CENTER);
		checkInfoLbl.setBorder(new LineBorder(Design.borderColor));
		this.add(checkInfoLbl, BorderLayout.SOUTH);
	}

	private void downloadUpdate() {
		try {
			long startTime = System.currentTimeMillis();

			System.out.println("Connecting to Mura site...\n");

			String downloadUrl = liveShiftGUI
					.getLiveShiftApplication().getConfiguration()
					.getUpdateServer()
					+ "/liveshift.";
			String ext = "";
			if (RuntimeUtil.isWindows())
				ext += "exe";
			else
				ext += "jar";
			URL url = new URL(downloadUrl + ext);
			url.openConnection();
			InputStream reader = url.openStream();

			FileOutputStream writer = new FileOutputStream(
					"./update-liveshift." + ext);
			byte[] buffer = new byte[153600];
			int totalBytesRead = 0;
			int bytesRead = 0;

			logger.debug("Reading ZIP file 150KB blocks at a time.\n");

			while ((bytesRead = reader.read(buffer)) > 0) {
				writer.write(buffer, 0, bytesRead);
				buffer = new byte[153600];
				totalBytesRead += bytesRead;
			}

			long endTime = System.currentTimeMillis();

			System.out.println("Done. "
					+ (new Integer(totalBytesRead).toString())
					+ " bytes read ("
					+ (new Long(endTime - startTime).toString())
					+ " millseconds).\n");
			writer.close();
			reader.close();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean runChecks(Configuration configuration) {
		Cursor hourglassCursor = new Cursor(Cursor.WAIT_CURSOR);
	    setCursor(hourglassCursor);
	    
		checkInfoLbl.setText("checking connection... ");
		if (EnvironmentCheck.checkInternetConnection())
			checkInfoLbl.setText(checkInfoLbl.getText() + " successful.");
		else {
			checkInfoLbl.setText(checkInfoLbl.getText() + " failure.");
			abort("Please check your internet connection.");
			return false;
		}

		checkInfoLbl.setText("checking for updates...");
		UpdateCheck updateCheck = new UpdateCheck(this.liveShiftGUI);
		if (updateCheck.isUpdateServerOnline()) {
			if (updateCheck.isNewVersionAvailable()) {
				if (JOptionPane.showConfirmDialog(null, "There is a new version of LiveShift available. Would you like to update?", "New Version Available",
						JOptionPane.YES_NO_OPTION) == 0) {
					downloadUpdate();
					liveShiftGUI.getLiveShiftApplication().shutdown();
				}
			}
		}

		checkInfoLbl.setText("checking local vlc...");
		if(Utils.fileExists(liveShiftGUI.getLiveShiftApplication().getConfiguration().getVlcPath())){
			checkInfoLbl.setText("checking local vlc... available");
		} else {
			checkInfoLbl.setText("checking local vlc... not found");
		}

		checkInfoLbl.setText("checking integrated VLC libraries...");
		if (configuration.verifyVlcLibs()){
			checkInfoLbl.setText("checking VLC libraries... available");
		} else {
			checkInfoLbl.setText("checking VLC libraries... not found");
		}

		checkInfoLbl.setText("checks finished.");

		Cursor normalCursor = new Cursor(Cursor.DEFAULT_CURSOR);
	    setCursor(normalCursor);
		return true;
	}

	private void abort(String message) {
		JOptionPane.showConfirmDialog(this, message, "Check Failed",
				JOptionPane.OK_OPTION);

	}
}
