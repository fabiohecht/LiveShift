package net.liveshift.core;

import net.liveshift.util.Utils;


public class UniformPeerBehavior extends ProbabilisticPeerBehavior {
	
	static final int NUM_CHANNELS = 6;

	@Override
	public int getHoldingTimeS() {
		return (int)(MIN_HOLDING_TIME + (Math.abs(Utils.getRandomLong()) % (MAX_HOLDING_TIME-MIN_HOLDING_TIME)));
	}
	
	@Override
	public int getChannelNumber() {
		return Math.abs(Utils.getRandomInt()) % NUM_CHANNELS;
	}

	@Override
	public boolean shouldChurn(float churnProbability) {
		return Math.abs(Utils.getRandomFloat()) < churnProbability;
	}
	
	@Override
	public long getTimeShiftRange(long range0, long range1) {
		long x = getHoldingTimeS();
		return Math.round(range1-(range1-range0)*(x-MIN_HOLDING_TIME)/(double)(MAX_HOLDING_TIME-MIN_HOLDING_TIME));
	}
}
