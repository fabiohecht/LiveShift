package net.liveshift.player;

import java.awt.Canvas;
import java.io.IOException;

import net.liveshift.configuration.Configuration;
import net.liveshift.configuration.Configuration.StreamingProtocol;
import net.liveshift.core.LiveShiftApplication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 *
 * @author Fabio Victora Hecht
 */

public class ExtVLCPlayer implements Player {

	final private static Logger logger = LoggerFactory.getLogger(ExtVLCPlayer.class);

	private Process vlcService;
	private String options = null;

	private final String vlcPath;
	
	/**
	 * Constructor sets path, receiving port and standard options, but does'nt start the thread.
	 * 
	 */
	public ExtVLCPlayer(final String vlcPath, int playerPort) {
		
		logger.debug("extvlc player - new instance - vlcpath:" + vlcPath);
		
		this.vlcPath = vlcPath;

		if (logger.isDebugEnabled()) logger.debug("in constructor");
		
		this.options = " -vvv udp://@:" + playerPort;
	}	
	
	
	/**
	 * Start VLC player in a new process.
	 * @param vlcLocation		Path to the player.
	 * @param options			Init options for the player.
	 * @throws IOException		
	 */
	private void startService(String options) throws IOException {
		
		logger.debug("extvlc player - start service - options:" + options);
		
		if (logger.isDebugEnabled()) logger.debug("in startService("+options+")");
		
		String[] optionsArray = options.trim().split(" ");
		
		String[] args = new String[optionsArray.length+1];
		args[0] = this.vlcPath;
		
		System.arraycopy(optionsArray, 0, args, 1, optionsArray.length);
		
		vlcService = ignoreProcessStreams(Runtime.getRuntime().exec(args));		
	}
	
	/**
	 * Destroys the old player process and start a new one.
	 * @param vlcLocation		Path to the player.
	 * @param options			Init options for the player.
	 * @throws IOException
	 */
	private void restartService(String options) throws IOException {

		if (logger.isDebugEnabled()) logger.debug("in restartService("+options+")");
		
		vlcService.destroy();
		vlcService = Runtime.getRuntime().exec(vlcPath + options);
	}
	
	/**
	 * Ignore the input stream and error stream of the process.
	 * @param process	Process to ignore his input stream and error stream
	 * @return
	 */
	private Process ignoreProcessStreams(Process process) {

		if (logger.isDebugEnabled()) logger.debug("in ignoreProcessStreams("+process.toString()+")");
		
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

		if (logger.isDebugEnabled()) logger.debug("in ignoreProcessInputStream("+process.toString()+")");
		
		new InputStreamIgnorer(process.getInputStream());
		return process;
	}
	
	/**
	 * Ignore the error stream of the process.
	 * @param processq		Process to ignore his error stream
	 * @return
	 */
	private Process ignoreProcessErrorStream(Process process) {

		if (logger.isDebugEnabled()) logger.debug("in ignoreProcessErrorStream("+process.toString()+")");
		
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

	
	public String getOptions() {
		return options;
	}
	
	@Override
	public void shutdown() {

		if (logger.isDebugEnabled()) logger.debug("in destroy()");
		
		if (vlcService!=null)
			vlcService.destroy();
	}

	@Override
	public void initializeLocalStream(String localIpAddress, int playerPort, StreamingProtocol protocol) {
		
		logger.debug("extvlc player - init");
		
		if (logger.isDebugEnabled()) logger.debug("in playLocalStream()");
		
		options = protocol.name()+"://"+localIpAddress+"@:" + playerPort;
		
		//args[0] = "-vvv"; // //set source
		//args[1] = "bailey.mpg";
		//args[2] = "--sout"; //set destination
    	//args[3] = "'#standard{access=udp,mux=ts,dst=192.168.1.34:1234}'";
		//args[3] = "#standard{access=udp):standard{mux=ts}:standard{dst=192.168.1.34:1234}";
		//args[3] = "#standard{mux=ts,access=udp,dst=192.168.1.34:1234}";
		//args[3] = "#standard{access=udp,mux=ts,dst=192.168.1.34:1234}";
		//args[3] = "#rtp{mux=ts,dst=192.168.1.34,port=1234,sap}";
    	//args[4] = "--ttl"; //set time to live
    	//args[5] = "100";
		
	}

	@Override
	public void stopPlayer() {
		if (this.vlcService!=null) {
			this.vlcService.destroy();
		}
		this.vlcService=null;
	}



	@Override
	public void pausePlayer() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void play() {
		if (this.vlcService==null) {

			try {
				startService(this.options);
				/*
				String debugMsg = SERVICE_NAME + " started with options " + "'" + this.options +"'";
				SystemMsgHandler.handleDebugMessage(debugMsg, "vlc");
				debugMsg = "VLC: " + this.vlcPath + this.options;
				SystemMsgHandler.handleDebugMessage(debugMsg, "vlc");
				*/
			} 
			catch (IOException e) { 
				e.printStackTrace(); 
				LiveShiftApplication.quit("ioexception");
			}
		}
	}
	
	@Override
	public void setCanvas(Canvas jvcanvas) {
		// TODO Auto-generated method stub
		
	}
	
	public void toggleFullscreen(){
		
	}

	@Override
	public float getVolumePercent() {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public void setVolumeUp(int amount) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void setVolumeDown(int amount) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void setVolumePercent(float volumePercent) {
		// TODO Auto-generated method stub
		
	}
}
