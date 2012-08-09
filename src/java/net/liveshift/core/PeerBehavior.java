package net.liveshift.core;

public interface PeerBehavior {
	public boolean shouldChurn(float churnProbability);
	public int getChannelNumber();
	public long getTimeShiftRange(long range0, long range1);
	public int getHoldingTimeS();
	public boolean next();
}