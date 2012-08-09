package net.liveshift.core;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class EnvironmentCheck {

	private static org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(EnvironmentCheck.class);

	public static boolean checkInternetConnection() {
		try {
			Enumeration<NetworkInterface> interfaces = NetworkInterface
					.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				NetworkInterface interf = interfaces.nextElement();

				if (interf.isUp() && !interf.isLoopback())
					return true;
			}
		} catch (SocketException e) {
			e.printStackTrace();
			logger.info("check internet connection failed", e);
			return false;
		}
		return false;
	}

}
