package net.liveshift.upload;

import java.util.concurrent.atomic.AtomicBoolean;

import net.liveshift.core.PeerId;
import net.liveshift.incentive.IncentiveMechanism;
import net.liveshift.storage.Segment;
import net.liveshift.storage.SegmentIdentifier;
import net.liveshift.time.Clock;



public class Subscriber implements Comparable<Subscriber> {
	
	final private PeerId requesterPeerId;
	final private SegmentIdentifier segmentIdentifier;
	
	final private Segment segment;
	final private IncentiveMechanism incentiveMechanism;
	final private UploadSlotManager uploadSlotManager;

	private boolean interested = false;
	private long blockedUntilTimeMillis = 0;
	private long grantedSince = 0;
	
	private long requestTimeMillis;

	private final AtomicBoolean peerLocked;  //access (read and set) must be syncronized (by UploadSlotRequestQueue)
	private boolean vectorHaveSent;
	
	public Subscriber(final PeerId requester, final SegmentIdentifier segmentIdentifier, final Segment segment, final UploadSlotManager uploadSlotManager, final AtomicBoolean peerLock) {
		
		this.requesterPeerId = requester;
		this.segmentIdentifier = segmentIdentifier;
		
		this.segment = segment;
		this.uploadSlotManager = uploadSlotManager;
		this.incentiveMechanism = uploadSlotManager.getVideoSignaling().getIncentiveMechanism();
		
		this.requestTimeMillis = Clock.getMainClock().getTimeInMillis(false);
		
		this.peerLocked = peerLock;
		this.vectorHaveSent = false;
	}
	
    @Override
    public int compareTo(final Subscriber o) {
       
		if (o==null)
			return 666;
		
		if (this.equals(o))
			return 0;
		
		//not grantable is always low priority >>>>>> WHICH MEANS THAT NOT INTERESTED (case of newcomers, even HU!) IS ALWAYS LOW PRIORITY <<<<<<<<<<<<<<<, but then, large |S| should be advantage
		boolean thisMayBeGranted = this.mayBeGranted();
		boolean oMayBeGranted = o.mayBeGranted();
		if (!thisMayBeGranted && oMayBeGranted)
			return -4;
		if (!oMayBeGranted && thisMayBeGranted)
			return 4;
        
		//peer reputation is first priority: avoids that a good peer loses slot
		if (!this.requesterPeerId.equals(o.requesterPeerId)) {
	        //different peers: peer reputation
	        double repDif;
	        if (incentiveMechanism==null)
	        	repDif = 0;
	        else {
	        	repDif = incentiveMechanism.compareReputations(this.requesterPeerId, o.requesterPeerId, o.segmentIdentifier.getStartTimeMS());
	        
		        if (repDif!=0)
		            return repDif>0?2:-2;
	        }
		}
		else {
			//same peer: oldest (because same peer may have 2 requests: current and next)
			int segmentDif = this.segmentIdentifier.compareTo(o.segmentIdentifier);
			if (segmentDif!=0)
			    return - this.segmentIdentifier.compareTo(o.segmentIdentifier)*3;
	    }
		
		/*
		//interested peers have preference (TEST -- problem is that new ones are always not interested yet)
		boolean thisIsInterested = this.isInterested();
		boolean oIsInterested = o.isInterested();
		if (!thisIsInterested && oIsInterested)
			return -7;
		if (!oIsInterested && thisIsInterested)
			return 7;
		 */

		//peer with most given blocks (from us) first (tries to estabilize connections)
		int blocksGivenDifference = this.uploadSlotManager.getSentBlocks(this.requesterPeerId) - this.uploadSlotManager.getSentBlocks(o.requesterPeerId);
		if (blocksGivenDifference!=0)
			return blocksGivenDifference>0?6:-6;
		
		//"rarest first" (least given segment first)
		if (!this.segmentIdentifier.equals(o.segmentIdentifier)) {
			int timesGivenSlot0 = this.segment.getTimesGivenUploadSlot();
			int timesGivenSlot1 = o.segment.getTimesGivenUploadSlot();
			
			int rarestFirstDif = timesGivenSlot0 - timesGivenSlot1;
			
			if (rarestFirstDif != 0)
			        return rarestFirstDif>0?-1:1;
		}
		
		//if everything else is the same, falls back to a FCFS (helps to keep consistent): higher (more recent) time = bad
		return this.requestTimeMillis - o.requestTimeMillis>0?-5:5;
		
		/*
		 * to consider:
		 
		I want to have some comparison factors that are not strong enough to prevent preemption
		maybe comparator > 100 to preempt?
		will prevent me from using sorted sets to do it, I will need to iterate, maybe write an object to do it
		
		this would help preempting less peers due to stupid reasons... (e.g. FCFS)
		 */ 
	}
	
	@Override
	public boolean equals(final Object uploadSlotRequest0) {
		
		if (uploadSlotRequest0 instanceof Subscriber) {
			Subscriber subscriber = (Subscriber)uploadSlotRequest0;
			
			return this.requesterPeerId.equals(subscriber.requesterPeerId) &&
				this.segmentIdentifier.equals(subscriber.segmentIdentifier);
		}
		else
			return false;
		
	}
	
	@Override
	public int hashCode() {
		return this.requesterPeerId.hashCode() ^ this.segmentIdentifier.hashCode();
	}

	public PeerId getRequesterPeerId() {
		return this.requesterPeerId;
	}
	public SegmentIdentifier getSegmentIdentifier() {
		return this.segmentIdentifier;
	}
	
	public int getTimeToTimeoutMillis() {
		return (int)(this.requestTimeMillis + (this.isGranted()?UploadSlot.GRANTED_TIMEOUT_MILLIS:(this.interested?UploadSlot.UNGRANTED_INTERESTED_TIMEOUT_MILLIS:UploadSlot.UNGRANTED_NOT_INTERESTED_TIMEOUT_MILLIS)) - Clock.getMainClock().getTimeInMillis(false));
	}

	@Override
	public String toString() {
		return "pid:"+requesterPeerId+" si:"+segmentIdentifier+" reqt:"+requestTimeMillis+" interested:"+interested;
	}
	
	public void setGranted(final boolean granted) {
		if (granted)
			this.grantedSince = Clock.getMainClock().getTimeInMillis(false);
		else {
			this.peerLocked.set(false);
			this.grantedSince = 0;		
		}
	}
	public boolean isGranted() {
		return this.grantedSince > 0;
	}
	public long getGrantedTimeMillis() {
		return this.grantedSince;
	}

	public boolean canSetSlotGranted() {
		if (this.segment==null)
			return false;
		else {
			this.segment.incrementTimesGivenUploadSlot();
			return true;
		}
	}
	
	public void setInterested(final boolean interested) {
		this.interested = interested;
	}
	public boolean isInterested() {
		return interested;
	}

	public void setBlocked(final boolean blocked) {
		if (blocked)
			this.blockedUntilTimeMillis = Long.MAX_VALUE;
		else
			this.blockedUntilTimeMillis = 0L;
	}
	public void setBlocked(final int timeMillis) {
		this.blockedUntilTimeMillis = Clock.getMainClock().getTimeInMillis(false) + timeMillis;
	}
	public boolean isBlocked() {
		return this.blockedUntilTimeMillis > Clock.getMainClock().getTimeInMillis(false);
	}
	
	public boolean mayBeGranted() {
		return !this.isBlocked() && this.interested && this.getTimeToTimeoutMillis() > 0 && !this.segment.getSegmentBlockMap().isEmpty() && (this.isGranted() || !this.isPeerBlocked());
	}

	/**
	 * locks peer so it may not be granted other requests
	 * 
	 * @return
	 */
	public boolean setPeerBlocked() {
		return this.peerLocked.compareAndSet(false, true);
	}
	private boolean isPeerBlocked() {
		return this.peerLocked.get();
	}

	public void resetTimeoutTimer() {
		this.requestTimeMillis = Clock.getMainClock().getTimeInMillis(false);
	}
	
	public boolean isVectorHaveSent() {
		return this.vectorHaveSent;
	}
	public void setVectorHaveSent() {
		this.vectorHaveSent = true;
	}
}
