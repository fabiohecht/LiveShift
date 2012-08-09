package net.liveshift.encoder;

import java.io.IOException;

import net.liveshift.configuration.Configuration.StreamingProtocol;
import net.liveshift.core.LiveShiftApplication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
*
* @author Fabio Victora Hecht
* 
*/
public class ExtVLCEncoder extends Thread implements Encoder {

	final private static Logger logger = LoggerFactory.getLogger(ExtVLCEncoder.class);

	private final String vlcPath;
	private String vlcParameters;
	private boolean vlcHidden;
	
	private Process vlcService;

	public ExtVLCEncoder(final String vlcParameters, String vlcPath, final boolean vlcHidden) {
		this.vlcPath = vlcPath;
		if (vlcParameters.equals("")) {
			this.vlcParameters = "-I rc --sout #transcode{vcodec=h264,vb=800,scale=0.25}:duplicate{dst=std{access=udp,mux=ts,dst=127.0.0.1:$encoderPort}}";
		}
		else {
			this.vlcParameters = vlcParameters;
		}
		
		this.vlcHidden = vlcHidden;
	}

	@Override
	public void run() {
		try {
			startService();
			/*
			String debugMsg = SERVICE_NAME + " started with options " + "'" + this.options +"'";
			SystemMsgHandler.handleDebugMessage(debugMsg, "vlc");
			debugMsg = "VLC: " + this.vlcPath + this.options;
			SystemMsgHandler.handleDebugMessage(debugMsg, "vlc");
			*/
		} catch (IOException e) { 
			e.printStackTrace(); 
			LiveShiftApplication.quit("IOException");
		}
	}
	
	/**
	 * Start VLC player in a new process.
	 * @param vlcLocation		Path to the player.
	 * @param options			Init options for the player.
	 * @throws IOException		
	 */
	private void startService() throws IOException {
		String command = vlcPath + " " + vlcParameters;
		if(vlcHidden)
			command += " -I dummy";
		logger.debug(command);
		this.vlcService = ignoreProcessStreams(Runtime.getRuntime().exec(command));		
	}
	
	/**
	 * Ignore the input stream and error stream of the process.
	 * @param process	Process to ignore his input stream and error stream
	 * @return
	 */
	private Process ignoreProcessStreams(Process process) {
		ignoreProcessInputStream(process);
		ignoreProcessErrorStream(process);
		return process;
	}
	
	/**
	 * Ignore the input stream of the process.
	 * @param processq		Process to ignore his input stream.
	 * @return
	 */
	private Process ignoreProcessInputStream(Process process) {
		new InputStreamIgnorer(process.getInputStream());
		return process;
	}
	
	/**
	 * Ignore the error stream of the process.
	 * @param processq		Process to ignore his error stream
	 * @return
	 */
	private Process ignoreProcessErrorStream(Process process) {
		new InputStreamIgnorer(process.getErrorStream());
		return process;
	}
	
	static class InputStreamIgnorer implements Runnable {
		java.io.InputStream in;

		InputStreamIgnorer(java.io.InputStream in) {
			this.in = in;
			new Thread(this, "InputStreamIgnorer Thread").start();
		}

		@Override
		public void run() {
			try {
				while (in.read() != -1) {}
			} catch (java.io.IOException ex) {}
		}
	}

	@Override
	public void createNewStreamingSourceFromFile(String fileName, String destinationIPAddress, int encoderPort, StreamingProtocol encoderProtocol) {
		if (logger.isDebugEnabled()) logger.debug("about to open and stream fileName |"+fileName+"|");

		this.vlcParameters = fileName+" "+this.vlcParameters.replace("$protocol", encoderProtocol.name().toLowerCase()).replace("$encoderPort", String.valueOf(encoderPort));
	}
	
	@Override
	public void shutdown() {
		if(this.vlcService != null)
		this.vlcService.destroy();
	}

	@Override
	public void startEncoding() {
		this.start();
	}


	@Override
	public void createNewStreamingSourceFromDevice(String deviceName, String destinationIPAddress, int encoderPort, StreamingProtocol encoderProtocol) {
		if (logger.isDebugEnabled()) logger.debug("about to open and stream fileName |"+deviceName+"|");

		this.vlcParameters = deviceName+" "+this.vlcParameters.replace("$protocol", encoderProtocol.name().toLowerCase()).replace("$encoderPort", String.valueOf(encoderPort));
		
	}


}
