package net.liveshift.encoder;

import java.util.Set;

import net.liveshift.configuration.Configuration.StreamingProtocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;


import uk.co.caprica.vlcj.binding.LibVlc;
import uk.co.caprica.vlcj.discovery.NativeDiscovery;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.headless.HeadlessMediaPlayer;
import uk.co.caprica.vlcj.runtime.RuntimeUtil;

public class VLCJEncoder extends Thread implements Encoder {
	
	final private static Logger logger = LoggerFactory.getLogger(ExtVLCEncoder.class);
	
	private String vlcParameters;
	private String source;
	
	private MediaPlayerFactory mediaPlayerFactory;
	private HeadlessMediaPlayer mediaPlayer;
	
	public VLCJEncoder(final String vlcParameters, final Set<String> pathToVlcLibs) {
		if (logger.isDebugEnabled()) {
			logger.debug("vlcj encoder - new instance");
		}
		this.vlcParameters = vlcParameters;

		NativeDiscovery nativeDiscovery = new NativeDiscovery();

		if (!nativeDiscovery.discover()) {
			for (String path : pathToVlcLibs) {
				NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), path);
			}
		    Native.loadLibrary(RuntimeUtil.getLibVlcLibraryName(), LibVlc.class);
		}
		
		this.mediaPlayerFactory = new MediaPlayerFactory();
		mediaPlayer = mediaPlayerFactory.newHeadlessMediaPlayer();

	}

	
	@Override
	public void createNewStreamingSourceFromFile(String fileName, String destinationIPAddress, int encoderPort, StreamingProtocol encoderProtocol) {
		if (logger.isDebugEnabled()) {
			logger.debug("vlcj encoder - new stream - file:" + fileName + " - port:" + encoderPort);
		}
		
		this.source = fileName;
		
		if (this.vlcParameters.equals("")) {
			this.vlcParameters = buildVlcParameters("127.0.0.1", encoderPort, encoderProtocol);
		} else {
			this.vlcParameters = vlcParameters.replace("$protocol", encoderProtocol.name().toLowerCase()).replace("$encoderPort", String.valueOf(encoderPort));
		}
	}

	@Override
	public void createNewStreamingSourceFromDevice(String deviceName, String destinationIPAddress, int encoderPort, StreamingProtocol encoderProtocol) {
		if (logger.isDebugEnabled()) {
			logger.debug("vlcj encoder - new stream - Devie:" + deviceName + " - port:" + encoderPort);
		}
		
		this.source = deviceName;
		
		if (this.vlcParameters.equals("")) {
			this.vlcParameters = buildVlcParameters("127.0.0.1", encoderPort, encoderProtocol);
		} else {
			this.vlcParameters = vlcParameters.replace("$protocol", encoderProtocol.name().toLowerCase()).replace("$encoderPort", String.valueOf(encoderPort));
		}
		
	}
	
	@Override
	public void startEncoding() {
		if (logger.isDebugEnabled()) {
			logger.debug("vlcj encoder - start encoding - file:" + source + " - command:" + vlcParameters);
		}
		
		mediaPlayer.playMedia(source, this.vlcParameters);
		
	}
	
	private String buildVlcParameters(String streamingAddress, int streamingPort, StreamingProtocol protocol) {
		StringBuilder sb = new StringBuilder(60);
		sb.append(":sout=#"+protocol.name().toLowerCase()+"{dst=");
		sb.append(streamingAddress);
		sb.append(",port=");
		sb.append(streamingPort);
		sb.append(",mux=ts}");
		sb.append(",:no-sout-rtp-sap");
		sb.append(",:no-sout-standard-sap");
		sb.append(",:sout-all");
		sb.append(",:sout-keep");
		return sb.toString();
		
	}
	
	@Override
	public void shutdown() {
		mediaPlayer.release();
	}

	
}
