package net.liveshift.download;


public interface IncomingBlockRateHandler {
	public void incrementIncomingBlockRate();
	public float getIncomingBlockRate();	
}
