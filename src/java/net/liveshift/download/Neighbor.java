package net.liveshift.download;

import java.util.Comparator;
import java.util.List;

import net.liveshift.core.PeerId;
import net.liveshift.signaling.messaging.SegmentBlock;
import net.liveshift.storage.SegmentBlockMap;
import net.liveshift.storage.SegmentIdentifier;
import net.liveshift.time.Clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A Neighbor is a peer which may have something interesting
 * A peer is only considered a neighbor after US_REQUEST and US_REPLY messages have been exchanged, along with SegmentBlockMaps
 * 
 * @author fabio
 *
 */

public class Neighbor implements Comparable<Neighbor> {

	final private static Logger logger = LoggerFactory.getLogger(Neighbor.class);

	private static final long SEND_NOT_INTERESTED_AFTER_NOT_INTERESTED_TIME_MILLIS = 3500L;
	private static final long CACHE_TIMEOUT = 3000;
	
	private final PeerId peerId;
	private final Candidate candidate;
	private final SegmentIdentifier segmentIdentifier;
	private final BlockRequestQueue blockRequestQueue;  //just to calculate interest for ranking
	private ProbabilisticSegmentBlockMap probabilisticSegmentBlockMap;
	
	private long lastDownloadTimeMillis;  //disconnects before it times out if a slot is granted
	private long notReallyInterestingSince = -1;  //prevents that a NOT_INTERESTED is sent too soon: allows the neighbor to be not interesting for a while before N_I is sent
	private boolean interesting;

	private long timeoutTimeMillis;

	private long numInterestingBlocksCacheTime = 0;
	private int numInterestingBlocksCached = -1;

	/**
	 * Adds a new neighbor = a peer with which this peer is in the upload queue, waiting to get a slot
	 */
	public Neighbor(final PeerId peerId, final SegmentIdentifier segmentIdentifier, final SegmentBlockMap segmentBlockMap, final Candidate candidate, final long timeoutMillis, final Tuner tuner) {
		if (logger.isDebugEnabled()) logger.debug("in Neighbor("+peerId.getName()+")");
		
		this.peerId = peerId;
		this.candidate = candidate;
		this.segmentIdentifier = segmentIdentifier;
		this.probabilisticSegmentBlockMap = new ProbabilisticSegmentBlockMap(segmentBlockMap, segmentIdentifier, peerId, tuner);
		
		long currentTime = Clock.getMainClock().getTimeInMillis(false);
		this.lastDownloadTimeMillis = currentTime;
		this.timeoutTimeMillis = currentTime + timeoutMillis;
		
		this.blockRequestQueue = tuner.getBlockRequestQueue();
	}
	
	public SegmentIdentifier getSegmentIdentifier() {
		return this.segmentIdentifier;
	}
	
	public ProbabilisticSegmentBlockMap getBlockMap() {
		return this.probabilisticSegmentBlockMap;
	}

	@Override
	public String toString() {
		return "pId:" + this.peerId  + " bm:"+this.probabilisticSegmentBlockMap+" si:["+segmentIdentifier+"] to="+this.timeoutTimeMillis;
	}

	@Override
	public int hashCode() {
		//this should identify this peer properly
		
		return this.peerId.hashCode();
	}
	
	@Override
	public boolean equals(Object n1) {
		
		if(!(n1 instanceof Neighbor))
			return false;
		
		Neighbor n0=(Neighbor)n1;
		return n0.peerId.equals(this.peerId) && n0.segmentIdentifier.equals(this.segmentIdentifier);
	}
	
	public PeerId getPeerId() {
		return peerId;
	}
		
	void setBlockInBlockMap(final int blockNumber, boolean doHave, final float rate) {
		if (this.probabilisticSegmentBlockMap==null)
			logger.warn("neighbor has no block map !? "+this);
		else
			this.probabilisticSegmentBlockMap.addSegmentBlockMapUpdateVector(blockNumber, doHave, rate);
	}
	
	/**
	 * returns the difference between the given block (normally the latest block we have)
	 * and the newest block the peer claims to have, contiguously counting
	 * from the given block
	 * 
	 * a positive value means the neighbor is behind the reference time
	 * negative is good (neighbor is ahead of us)
	 * 
	 * @param referenceBlockNumber
	 * @return the delay (in blocks) or Integer.MIN_VALUE if no block was found.
	 */
	int getDelay(final int referenceBlockNumber) {
		
		if (this.probabilisticSegmentBlockMap==null) {
			logger.warn("this.segmentBlockMap==null");
			return Integer.MIN_VALUE;
		}
		
		int lastSetBlock = this.probabilisticSegmentBlockMap.getLastSetBit(referenceBlockNumber==-1?0:referenceBlockNumber);
		
		if (lastSetBlock==-1)  //i.e. neighbor block map is empty
			return Integer.MIN_VALUE;
		else
			return referenceBlockNumber - lastSetBlock;
	}

	long getLastDownloadAgoMillis() {
		return Clock.getMainClock().getTimeInMillis(false) - this.lastDownloadTimeMillis;
	}
	void setLastDownloadNow() {
		this.lastDownloadTimeMillis = Clock.getMainClock().getTimeInMillis(false);
	}

	public void setInteresting(final boolean interesting) {
		this.interesting = interesting;
		this.lastDownloadTimeMillis = Clock.getMainClock().getTimeInMillis(false);
		this.notReallyInterestingSince = -1;
	}
	public boolean isInteresting() {
		return interesting;
	}
	public boolean shouldSendNotInterestedYet(final boolean interesting) {
		if (interesting) {
			return false;
		}
		else if (this.notReallyInterestingSince == -1) {
			this.notReallyInterestingSince = Clock.getMainClock().getTimeInMillis(false);
			return false;
		}
		else if (this.notReallyInterestingSince + SEND_NOT_INTERESTED_AFTER_NOT_INTERESTED_TIME_MILLIS < Clock.getMainClock().getTimeInMillis(false)) {
			return true;
		}
		else {
			return false;
		}
	}

	void setTimeOutMillis(final long timeoutMillis) {
		if (logger.isDebugEnabled()) logger.debug("setting timeout of ["+this+"] to "+timeoutMillis);
		long currentTime = Clock.getMainClock().getTimeInMillis(false);
		this.timeoutTimeMillis = currentTime + timeoutMillis;
	}
	boolean isTimedOut() {
		//it means that, as a neighbor, it has timed out, therefore removed from receving haves and won't get a slot
		//it is OK to request a slot then
		return this.getTimeOutLeftMillis() < 0;
	}
	public long getTimeOutLeftMillis() {
		long currentTime = Clock.getMainClock().getTimeInMillis(false);
		long timeLeft = (this.timeoutTimeMillis - currentTime);
		
		if (logger.isDebugEnabled()) logger.debug("timeLeft is " + timeLeft);
		
		return timeLeft;
	}

	public void replaceSegmentBlockMap(final SegmentBlockMap segmentBlockMap, final Tuner tuner) {
		this.probabilisticSegmentBlockMap = new ProbabilisticSegmentBlockMap(segmentBlockMap, segmentIdentifier, peerId, tuner);
	}

	@Override
	public int compareTo(Neighbor o) {

		if (this.equals(o))
			return 0;
		
		//given blocks!!
		int givenBlocksDifference = this.candidate.getNumReceivedBlocks() - o.candidate.getNumReceivedBlocks();
		if (givenBlocksDifference!=0)
			return givenBlocksDifference>0?2:-2;
		
		//how many interesting blocks
		int interestDifference = this.getNumInterestingBlocks() - o.getNumInterestingBlocks();
		if (interestDifference!=0)
			return interestDifference>0?2:-2;
			
		int randomTieBreakerDifference = this.candidate.getRandomTieBreaker() - o.candidate.getRandomTieBreaker();
		if (randomTieBreakerDifference!=0)
			return randomTieBreakerDifference>0?3:-3;

		int peerIdDifference = this.getPeerId().getDhtId().compareTo(o.getPeerId().getDhtId());
		if (peerIdDifference!=0)
			return peerIdDifference>0?4:-4;
		return 0;
	}
	
	private int getNumInterestingBlocks() {
		//just a cache for getNumInterestingBlocksReally, since it's used in a comparator
		long currentTime = Clock.getMainClock().getTimeInMillis(false);
		if (this.numInterestingBlocksCached==-1 || currentTime-this.numInterestingBlocksCacheTime>CACHE_TIMEOUT) {
			this.numInterestingBlocksCached=this.getNumInterestingBlocksReally();
			this.numInterestingBlocksCacheTime=currentTime;
		}
		return this.numInterestingBlocksCached;
	}

	private int getNumInterestingBlocksReally() {
		int count=0;
		List<BlockRequest> blocksInQueue = this.blockRequestQueue.snapshot();
		for (BlockRequest blockRequest : blocksInQueue) {
			if (blockRequest.getSegmentIdentifier().equals(this.segmentIdentifier) 
				&& this.probabilisticSegmentBlockMap.get(blockRequest.getBlockNumber())) {
				count++;
			}
		}
		return count;
	}
	
	/*
	 * not used at the moment but may be useful later
	 */
	static class PlayoutLagComparator implements Comparator<Neighbor> {
		
		final long playTime;

		public PlayoutLagComparator(final long playTime) {
			this.playTime = playTime;
		}
		
		@Override
		public int compare(Neighbor o1, Neighbor o2) {
			
			if (SegmentIdentifier.getSegmentNumber(playTime)!=o1.getSegmentIdentifier().getSegmentNumber())
				return 0;
			if (SegmentIdentifier.getSegmentNumber(playTime)!=o2.getSegmentIdentifier().getSegmentNumber())
				return 0;
			
			int playBlockNumber = SegmentBlock.getBlockNumber(playTime);
			
			int lag1 = o1.getDelay(playBlockNumber);
			int lag2 = o2.getDelay(playBlockNumber);
			
			return lag1-lag2;
		}
	}


	public Candidate getCandidate() {
		return this.candidate;
	}

	public void shutdown() {
		logger.debug("in shutdown pid:"+this.peerId);
		this.probabilisticSegmentBlockMap.shutdown();
	}
	
	public boolean hasBlock(int blockNumber) {
		return this.probabilisticSegmentBlockMap.get(blockNumber);
	}
}
