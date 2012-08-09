package net.liveshift.download;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import net.liveshift.core.PeerId;
import net.liveshift.time.Clock;
import net.liveshift.util.MovingAverage;
import net.liveshift.util.Utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Candidate implements Comparable<Candidate> {
	
	final private static Logger logger = LoggerFactory.getLogger(Candidate.class);
	
	final static int MINIMUM_US_REQUEST_INTERVAL_TIME_S = 10;  //minimum number of seconds between 2 US_REQUEST messages to a candidate. the value used will be the minimum between this one and the timeout that comes from the peer
	final static int MAX_USELESS_US_REQUESTS = 5;  //maximum number of consecutive US_REQUEST messages that will be sent to this candidate before it will be excluded and new ones will be loaded from the DHT

	private final PeerId peerId;
	final private AtomicInteger numReceivedBlocks;
	final private MovingAverage subscribeCounter;
	
	private long timeNextSubscribeMs;
	private long cleanupPreventedUntil;
	
/*
	private long lastLatencyCheckTimeMillis = 0L;
	private long latencyMillis;
*/
	final private int randomTieBreaker = Utils.getRandomInt();
	
	public Candidate(PeerId peerId) {
		if (logger.isDebugEnabled()) logger.debug("in Candidate("+peerId.getName()+")");
		
		this.peerId = peerId;
		this.numReceivedBlocks = new AtomicInteger();
		this.subscribeCounter = new MovingAverage(Clock.getMainClock(), 5, 1000);
	}
	
	public void setTimeLastSubscribeNow() {
		long currentTime = Clock.getMainClock().getTimeInMillis(false);
		this.timeNextSubscribeMs = currentTime + MINIMUM_US_REQUEST_INTERVAL_TIME_S*1000L;
		
		this.subscribeCounter.inputValue(1); //for rotating
	}
	public void setTimeNextSubscribe(long timeoutMillis) {
		
		if (logger.isDebugEnabled()) logger.debug("setting sending of next Subscribe to after "+this.peerId+" for "+timeoutMillis+"ms ahead");
		
		long currentTime = Clock.getMainClock().getTimeInMillis(false);
		this.timeNextSubscribeMs = currentTime + timeoutMillis;
	}

	boolean mayBeSentSubscribe() {
		
		long currentTime = Clock.getMainClock().getTimeInMillis(false);
		if (logger.isDebugEnabled()) logger.debug("mayBeSentSubscribe "+this.peerId+" has subscribeCounter:"+this.subscribeCounter+" and timeNextSubscribeMs:"+this.timeNextSubscribeMs+(this.timeNextSubscribeMs>=currentTime?">=":"<")+currentTime);
		
		return currentTime >= this.timeNextSubscribeMs;
	}

	public PeerId getPeerId() {
		return this.peerId;
	}
	
	@Override
	public String toString() {
		return this.peerId.toString();
	}

	/**
	 * return whether it is time to ping this candidate
	 * 
	 * 
	 * @return
	 *//*
	boolean shouldPing() {
		long currentTimeMillis = Clock.getMainClock().getTimeInMillis(false);
		return currentTimeMillis - this.lastLatencyCheckTimeMillis  > LATENCY_CHECK_FREQUENCY_MILLIS;
	}
	
	void setLatencyMillis(long pingTimeMillis) {
		if (logger.isDebugEnabled()) logger.debug("setting latency of "+this+" to "+pingTimeMillis+" ms");
		
		this.latencyMillis = pingTimeMillis;
		this.lastLatencyCheckTimeMillis = Clock.getMainClock().getTimeInMillis(false);
	}
	
	long getLatencyMillis() {
		return this.latencyMillis;
	}
	*/
	/*
	void setTimedOut() {
		this.timedoutTimes++;
	}
	int getTimedOutTimes() {
		return this.timedoutTimes;
	}
	*/
	
	void incrementNumReceivedBlocks() {
		this.numReceivedBlocks.incrementAndGet();
	}
	int getNumReceivedBlocks() {
		return this.numReceivedBlocks.intValue();
	}
	
	@Override
	public int compareTo(Candidate o) {
		if (this.equals(o)) 
			return 0;
		
		int subscribeCounterDifference = this.subscribeCounter.getSum() - o.subscribeCounter.getSum();
		if (subscribeCounterDifference!=0) {
			return subscribeCounterDifference>0?-1:1;  //less is more! encourages rotation
		}
		
		/*
		long latencyDifference = this.latencyMillis - o.latencyMillis;
		if (latencyDifference!=0)
			return latencyDifference>0?-2:2;  //less is more!
		*/
			
		//given blocks!!
		int givenBlocksDifference = this.numReceivedBlocks.intValue() - o.numReceivedBlocks.intValue();
		if (givenBlocksDifference!=0)
			return givenBlocksDifference>0?2:-2;
		
		int randomTieBreakerDifference = this.randomTieBreaker - o.randomTieBreaker;
		if (randomTieBreakerDifference!=0)
			return randomTieBreakerDifference>0?3:-3;
		
		int peerIdDifference = this.getPeerId().getDhtId().compareTo(o.getPeerId().getDhtId());
		if (peerIdDifference!=0)
			return peerIdDifference>0?4:-4;
		return 0;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Candidate) {
			Candidate candidate = (Candidate)obj;
			return this.peerId.equals(candidate.peerId);
		}
		return false;
	}

	int getRandomTieBreaker() {
		return this.randomTieBreaker;
	}

	public void setCleanUpPrevented(final long howLongMillis) {
		if (logger.isDebugEnabled()) logger.debug("setCleanUpPrevented "+this.peerId+" for "+howLongMillis);

		this.cleanupPreventedUntil = howLongMillis + Clock.getMainClock().getTimeInMillis(false);
	}
	public boolean isCleanUpPrevented() {
		return this.cleanupPreventedUntil > Clock.getMainClock().getTimeInMillis(false);
	}
	
	public boolean shouldBeRemoved() {
		return this.numReceivedBlocks.get()==0 && this.subscribeCounter.getSum() > MAX_USELESS_US_REQUESTS;
	}
}
