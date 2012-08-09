package net.liveshift.encoder;

import net.liveshift.configuration.Configuration.StreamingProtocol;


/**
*
* @author Fabio Victora Hecht
* 
*/
public interface Encoder {
	
	public void createNewStreamingSourceFromFile(String fileName, String destinationIPAddress, int encoderPort, StreamingProtocol encoderProtocol);
	public void createNewStreamingSourceFromDevice(String deviceName, String destinationIPAddress, int encoderPort, StreamingProtocol encoderProtocol);
	
	public void startEncoding();
	public void shutdown();
}