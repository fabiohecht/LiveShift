package net.liveshift.gui;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateCheck {
	
	private static final Logger logger = LoggerFactory.getLogger(UpdateCheck.class);
		
	final private LiveShiftGUI liveShiftGUI;


	public UpdateCheck(final LiveShiftGUI liveShiftGUI) {
		this.liveShiftGUI=liveShiftGUI;
	}

	public boolean isNewVersionAvailable() {
		String serverVersion = getServerVersion();
		String currentVersion = liveShiftGUI.getCurrentVersion();
		int sv_main = Integer.parseInt(serverVersion.split("\\.")[0]);
		int sv_sub = Integer.parseInt(serverVersion.split("\\.")[1]);
		int cv_main = Integer.parseInt(currentVersion.split("\\.")[0]);
		int cv_sub = Integer.parseInt(currentVersion.split("\\.")[1]);
		return sv_main > cv_main || (sv_main == cv_main && sv_sub > cv_sub);
	}

	private String getServerVersion() {
		try {
			String updateServer = liveShiftGUI.getLiveShiftApplication()
					.getConfiguration()
					.getUpdateServer();
			
			if (updateServer==null) {
				logger.warn("No update server defined. Not checking for updates.");
			}
			
			BufferedInputStream in = new BufferedInputStream(new URL(
					updateServer + "/liveshift.version").openStream());
			byte data[] = new byte[1024];
			int count = in.read(data, 0, 1024);
			String version = "0.0";
			if (count > 0) {
				version = new String(data);
			}
			return version;
		} catch (MalformedURLException e) {
			return "0.0";
		} catch (IOException e) {
			return "0.0";
		}
	}

	public boolean isUpdateServerOnline() {
		URL u;
		HttpURLConnection huc;
		try {
			u = new URL(liveShiftGUI.getLiveShiftApplication()
					.getConfiguration().getUpdateServer()
					+ "/liveshift.version");
			huc = (HttpURLConnection) u.openConnection();
			huc.setRequestMethod("GET");
			huc.setConnectTimeout(300);
			huc.connect();
			return huc.getResponseCode() == 200;
		} catch (MalformedURLException e) {
			return false;
		} catch (IOException e) {
			return false;
		}
	}
}
