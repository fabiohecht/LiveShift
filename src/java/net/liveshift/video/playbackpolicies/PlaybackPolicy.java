package net.liveshift.video.playbackpolicies;

import net.liveshift.storage.Segment;


public interface PlaybackPolicy {

	public enum PlayingDecision { PLAY, STALL, SKIP }
	public enum PlaybackPolicyType { SKIPSTALL, RDT, RETRY, RATIO, SMART_RETRY, CATCHUP }

	public void clearWindow(long startTime, long perfectPlayTimeMs);
	public void addBlockMap(final Segment segment);	
	
	public PlayingDecision getPlayDecision();
	public int getNumBlocksToSkip();  //if PlayingDecision=SKIP, gets how many blocks to skip (>0)

	public float shareNextBlock();
	public int getBlocksBuffered();
}
