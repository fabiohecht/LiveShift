package net.liveshift.configuration;

import java.io.File;

import net.liveshift.configuration.Configuration.EncoderName;
import net.liveshift.encoder.Encoder;
import net.liveshift.encoder.VLCTranscoder;


public class PublishConfiguration {
	
	public enum PublishSource {
		FILE, DEVICE, NETWORK
	};
	
	private PublishSource publishSource;
    private Byte substream = 1;
    private String name = "My Channel";
    private String description = "My Channel Description";
    private File fileSource;
    private String deviceSource;
    private String networkSource = "239.0.0.1";
    private boolean published;
    private VLCTranscoder transcoder;
    private boolean fileLoop;
    
    // rtp
//    private String vlcCommand = "$file --sout #transcode{vcodec=h264,vb=800,scale=0.25}:duplicate{dst=std{access=rtp,mux=ts,dst=127.0.0.1:$encoderPort}}";
    
    // udp
//    private String vlcCommand = "$file --sout #transcode{vcodec=h264,vb=800,scale=0.25}:duplicate{dst=std{access=udp,mux=ts,dst=127.0.0.1:$encoderPort}}";
    
    // WORKING! only for videos / not devices
    //private String vlcCommand = "$file :sout=#udp{dst=127.0.0.1:$encoderPort,mux=ts}";
        
    // with transcode
    private String vlcParameters = ":sout=#transcode{vcodec=mp2v,vb=0,scale=0,acodec=mp4a,ab=128,channels=2,samplerate=44100}:$protocol{dst=127.0.0.1,port=$encoderPort,mux=ts}";
    
    // without transcode
//    private String vlcCommand = "$file :sout=#udp{dst=127.0.0.1:$encoderPort,mux=ts}";
    
    //private String vlcCommand = ":sout=#transcode{vcodec=h264,vb=800,scale=0.25}duplicate{dst=std{access=udp,mux=ts,dst=127.0.0.1:$encoderPort}}";
    
//    private String vlcCommand = "vdev=\"$file\"" +
//    	" :sout=#transcode{vcodec=h264,vb=800,scale=0.25}:duplicate{dst=std{access=udp,mux=ts,dst=127.0.0.1:$encoderPort}}";
    
    //private String vlcCommand = ":sout=#transcode{vcodec=h264,vb=0,scale=0,acodec=mp4a,ab=128,channels=2,samplerate=44100}:udp{dst=127.0.0.1:$encoderPort} :no-sout-rtp-sap :no-sout-standard-sap :sout-keep";
    
//    private String vlcCommand = "-I rc -vvv v4l2:///dev/video0:height=240:width=320" +
//	" :sout=#duplicate{dst=std{access=udp,mux=ts,dst=127.0.0.1:$encoderPort}}";
        
    public PublishConfiguration() {
//    	this.liveShiftGui = liveShiftGui;
    	
        
        transcoder = new VLCTranscoder().getDefault();
    }

    /**
     * @return the substream
     */
    public Byte getSubstream()
    {
        return substream;
    }

    /**
     * @param substream the substream to set
     */
    public void setSubstream(Byte substream)
    {
        this.substream = substream;
    }

    /**
     * @return the name
     */
    public String getName()
    {
        return name;
    }
    
     /**
     * @param name the name to set
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return the description
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * @return the file
     */
    public File getFileSource()
    {
    	return fileSource;
    }

    /**
     * @param fileSource the file to set
     */
    public void setFileSource(File fileSource)
    {
    	this.publishSource = PublishSource.FILE;
        this.fileSource = fileSource;
    }

    /**
     * @return the device
     */
    public String getDeviceSource()
    {
        return deviceSource;
    }

    /**
     * @param device the device to set
     */
    public void setDeviceSource(String device)
    {
    	this.publishSource = PublishSource.DEVICE;
        this.deviceSource = device;
    }

    /**
     * @return the vlcCommand
     */
    public String getVlcParameters()
    {
//    	return "$file -I dummy" + (getFileLoop() ? " --loop" : "") + " :sout=#transcode{" + getTranscoder().getTranscodeArg() + 
//    		"}:duplicate{dst=udp{mux=ts,dst=127.0.0.1:$encoderPort}} " +
//    		" :no-sout-rtp-sap :no-sout-standard-sap :sout-keep";
    	
//    	return "$file -I dummy :sout=#transcode{vcodec=mp2v,vb=0,scale=0,acodec=mp4a,ab=128,channels=2,samplerate=44100" +
//    			"}:duplicate{dst=udp{mux=ts,dst=127.0.0.1:$encoderPort}} " +
//    			" :no-sout-rtp-sap :no-sout-standard-sap :sout-keep";
    	
//    	return ":sout=#transcode{vcodec=mp2v,vb=0,scale=0,acodec=mp4a,ab=128,channels=2,samplerate=44100" +
//		"}:duplicate{dst=udp{mux=ts,dst=127.0.0.1:$encoderPort}}";
    	
    	return vlcParameters;
    	
    	// working from command line
    	// vlc -I dummy v4l:///dev/video0 :sout='#transcode{vcodec=h264,vb=0,scale=0,acodec=mp4a,ab=128,channels=2,samplerate=44100}:udp{mux=ts,dst=127.0.0.1:1234}' :no-sout-rtp-sap :no-sout-standard-sap :sout-keep
    }

    /**
     * @param vlcParameters the vlcCommand to set
     */
    public void setVlcParameters(String vlcParameters)
    {
        this.vlcParameters = vlcParameters;
    }

    public void setPublished(boolean published) {
    	this.published = published;
    }
    
    public boolean getPublished(){
    	return this.published;
    }
    
    public void setTranscoder(VLCTranscoder transcoder) {
    	this.transcoder = transcoder;
    }
    
    public VLCTranscoder getTranscoder(){
    	return this.transcoder;
    }
	
	public boolean getFileLoop() {
		return fileLoop;
	}

	public void setFileLoop(boolean fileLoop) {
		this.fileLoop = fileLoop;
	}

	public String getNetworkSource() {
		return this.networkSource;
	}
	public void setNetworkSource(String networkSource) {
		this.publishSource = PublishSource.NETWORK;
		this.networkSource = networkSource;
	}
	
	public PublishSource getPublishSource() {
		return this.publishSource;
	}
}
