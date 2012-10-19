package net.liveshift.download;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import net.liveshift.core.PeerId;
import net.liveshift.incentive.IncentiveMechanism;
import net.liveshift.incentive.IncentiveMechanism.IncentiveMechanismType;
import net.liveshift.p2p.P2PAddress;
import net.liveshift.signaling.messaging.SegmentBlock;
import net.liveshift.storage.Segment;
import net.liveshift.storage.SegmentBlockMap;
import net.liveshift.storage.SegmentIdentifier;
import net.liveshift.time.Clock;
import net.liveshift.util.ExecutorPool;
import net.liveshift.util.MovingAverage;
import net.liveshift.util.TriggableScheduledExecutorService;
import net.liveshift.util.Utils;
import net.liveshift.video.VideoPlayer;
import net.tomp2p.futures.BaseFutureAdapter;
import net.tomp2p.futures.FutureTracker;
import net.tomp2p.storage.TrackerData;

import org.jboss.netty.util.internal.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class TunedSegment {
	final private static Logger logger = LoggerFactory.getLogger(TunedSegment.class);

	private static final int NEIGHBOR_HIGH_DELAY_SECONDS	= 8;
	private static final long NEIGHBOR_SEND_NOT_INTERESTED_AFTER_NOT_INTERESTING_FOR_MILLIS	= 2500;
	private static final long TUNED_SEGMENT_MAINTENANCE_FREQUENCY_MILLIS = 1000L;
	private static final int FAILURE_WINDOW_SIZE_SECONDS = 5;
	private static final int FAILURES_PER_WINDOW_THRESHOLD = 4;
	private static final long TRACKER_GETTING_CANDIDATES_TIME_LIMIT_MILLIS = 4000L;

	private final Tuner tuner;
	private final Segment segment;
	private final ConcurrentHashMap<PeerId, Candidate> candidates;
	private final ConcurrentHashMap<PeerId, Neighbor> neighbors;
	private final ConcurrentHashMap<PeerId, BlockRequester> blockRequesters;
	private final ConcurrentHashMap<PeerId, MovingAverage> failureCounter;
	
	private final NeighborList neighborList;
	final private MovingAverage incomingBlockRate = new MovingAverage(Clock.getMainClock(), 3, SegmentBlock.getBlocksPerSecond()*1000);
	private final ReentrantLock searchCandidatesLock = new ReentrantLock();
	private long trackerGettingCandidatesSince = -1;
	
	final private AtomicBoolean isShutDown = new AtomicBoolean(false);

	private long timeLimitTryOldNeighbors = -1;

	private TriggableScheduledExecutorService tunedSegmentMaintenanceExecutorService;

	final private Callable<Boolean> tunedSegmentMaintenanceRunner = new Callable<Boolean>() {
		@Override
		public Boolean call() {
			try {
				//performs some measurements and gets rid of bad candidates, neighbors etc 
				//searches for new neighbors candidates and tries new upload slots
				searchCandidatesAndRequestUploadSlots();
				
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
			return true;
		}
	};

	TunedSegment (final Segment segment, final Tuner tuner,  final NeighborList neighborList) {
		
		if (logger.isDebugEnabled()) logger.debug("constructing: "+segment);
		
		this.segment= segment;
		this.neighbors = new ConcurrentHashMap<PeerId,Neighbor>();
		this.candidates = new ConcurrentHashMap<PeerId, Candidate>();
		this.blockRequesters = new ConcurrentHashMap<PeerId, BlockRequester>();
		this.failureCounter = new ConcurrentHashMap<PeerId, MovingAverage>();
		
		this.tuner = tuner;
		this.neighborList = neighborList;
		
	}
	
	void scheduleTasks() {
		this.tunedSegmentMaintenanceExecutorService = new TriggableScheduledExecutorService(this.tunedSegmentMaintenanceRunner, 0, 0,
				TUNED_SEGMENT_MAINTENANCE_FREQUENCY_MILLIS, Clock.getMainClock(), "SearchCandidatesRequestSlots["+this.segment.getSegmentIdentifier().toString()+"]");
	}
	
	/**
	 * searches for possible candidates for each of the segmentdescriptions this
	 * object is responsible for
	 * @throws InterruptedException 
	 */
	private void searchCandidatesAndRequestUploadSlots() throws InterruptedException {

		if (logger.isDebugEnabled()) logger.debug("in searchCandidatesAndRequestUploadSlots()");
		
		if (logger.isDebugEnabled()) logger.debug("will try lock");
		if (searchCandidatesLock.tryLock()) {
			try {
				if (logger.isDebugEnabled()) logger.debug("got lock");
	
				if (!this.neighborList.isDoneWithPreviousTunedSegment(this.segment.getSegmentIdentifier()) && this.candidates.size() > 0 && (this.timeLimitTryOldNeighbors == -1||this.timeLimitTryOldNeighbors < Clock.getMainClock().getTimeInMillis(false))) {
					
					//there are candidates carried over from last tunedsegment which will be tried first before going out to get new ones
					this.sendSubscribeAndInterested();

					//imposes a limit not to prevent looking for further candidates forever
					this.timeLimitTryOldNeighbors = this.segment.getSegmentIdentifier().getStartTimeMS() - Clock.getMainClock().getTimeInMillis(false) + 2000;
				}
				else {
		
					// removes neighbors that timed out
					this.removeTimedOutNeighbors();
		
					// removes candidates if we have too many
					this.removeLameCandidatesAndNeighbors();
		
					// searches for new candidates
					boolean requestUploadSlotsNow = this.searchCandidates();
					
					if (requestUploadSlotsNow)
						this.sendSubscribeAndInterested();
				}
		
		//					if (logger.isDebugEnabled()) logger.debug("not looking for new candidates now");
		
				// pings them to find out latency -- that is to be minimized
				//tunedSegment.pingCandidates();  TODO  problem was that it needed a connection just to ping them, kind of a waste
			}
			finally {
				this.searchCandidatesLock.unlock();
			}
		}
		else
			if (logger.isDebugEnabled()) logger.debug("lock was already locked");
		
		if (logger.isDebugEnabled()) logger.debug("done searchCandidatesAndRequestUploadSlots()");
	}

	@Override
	public String toString() {
		return "(TS si:"+this.getSegment().toString()+" c:"+this.candidates.size()+" n:"+this.neighbors.size()+" blockRequesters:"+this.blockRequesters.size()+")";
	}
	
	public Candidate addCandidate(final PeerId peerId) {
		
		Candidate candidate = new Candidate(peerId);
	
		this.candidates.put(peerId, candidate);
		this.failureCounter.put(peerId, new MovingAverage(Clock.getMainClock(),FAILURE_WINDOW_SIZE_SECONDS,1000));
	
		return candidate;
	}

	public void addNeighbor(final PeerId peerId, final SegmentBlockMap segmentBlockMap, final long timeoutMillis) {
	
		Neighbor neighbor = this.neighbors.get(peerId);
		Candidate candidate = this.candidates.get(peerId);
		
		if (candidate==null) {
			//adds new candidate
			candidate = this.addCandidate(peerId);
		}
		
		if (neighbor == null && segmentBlockMap!=null) {
			if (logger.isDebugEnabled()) logger.debug("adding a new neighbor: "+peerId);
			
			neighbor = new Neighbor(peerId, this.segment.getSegmentIdentifier(), segmentBlockMap, candidate, timeoutMillis, tuner);
			this.neighbors.put(peerId, neighbor);
			
			//sees if there is an initial reputation/history value to be used
			IncentiveMechanism incentiveMechanism = this.tuner.getVideoSignaling().getIncentiveMechanism();
			
			if (incentiveMechanism!=null && incentiveMechanism.getIncentiveMechanismType() == IncentiveMechanismType.PSH)
				incentiveMechanism.increaseReputation(peerId, incentiveMechanism.getInitialHistory(peerId.getName()), 0);

			//checks interest immediately
			this.checkInterestAndSendInterested(neighbor);
			
		}
		else if (neighbor == null) {
			logger.warn("tried to add a new neighbor ("+peerId+") with null block map. not allowed.");
			
		}
		else {
			if (this.blockRequesters.containsKey(peerId)) {
				if (logger.isDebugEnabled()) logger.warn("neighbor to be updated ("+candidate.toString()+") is already granting upload slot. doing nothing.");
				
			}
			else {
				if (logger.isDebugEnabled()) logger.debug("updating existing neighbor: "+candidate.toString());
				
				if (segmentBlockMap!=null)
					neighbor.replaceSegmentBlockMap(segmentBlockMap, tuner);
				
				neighbor.setTimeOutMillis(timeoutMillis);
			}
		}
	}
	
	/*
	 * does nothing with the long uploadSlotTimeoutS
	 * the other peer shall let us know when the slot isn't granted anymore
	 * TODO in the future, I guess, it could use the TO info for planning purposes 
	 */
	public boolean setUploadSlotGranted(final PeerId peerId, final long uploadSlotTimeoutMillis) {
		
		Neighbor neighbor = this.neighbors.get(peerId);
		Candidate candidate = this.candidates.get(peerId);
		
		if (candidate==null) {
			if (logger.isDebugEnabled()) logger.debug("trying to add a neighbor ("+peerId.getName()+") that's not even a candidate! it could be that the Subscribed message is a bit delayed. will retry later.");
			return false;
		}
		if (neighbor==null) {
			if (logger.isDebugEnabled()) logger.debug("received upload slot from a non-neighbor ("+peerId.getName()+") :/ it could be that the Subscribed message is a bit delayed. will retry later. I got: "+this.neighbors.toString());
			return false;
		}
		
		neighbor.setTimeOutMillis(neighbor.getTimeOutLeftMillis() + uploadSlotTimeoutMillis);
		neighbor.setLastDownloadNow();
		
		//creates a new block scheduler only if one was not already created
		BlockRequester blockRequester = this.blockRequesters.get(peerId);
		if (blockRequester==null) {
			blockRequester = new BlockRequester(this.segment, neighbor, this.tuner);
			
			this.blockRequesters.put(peerId, blockRequester);
		}
		
		return true;
	
	}
	
	public void removeCandidate(final PeerId peerId) {
		
		if (logger.isDebugEnabled()) logger.debug("removing candidate ["+peerId+"] from ["+this+"]");
		
		this.removeNeighbor(peerId, 10000);
		
		Candidate candidate = this.candidates.get(peerId);
		if (candidate!=null) {
			this.candidates.remove(peerId);
		}
	}
	
	public void removeNeighbor(final PeerId peerId, final long timeForNextRequestMillis) {
	
		if (logger.isDebugEnabled()) logger.debug("will removeNeighbor("+peerId+","+timeForNextRequestMillis+") from ["+this+"]");
		
		Neighbor neighbor = this.neighbors.get(peerId);
		
		if (neighbor!=null) {
			
			//if it is granting an upload slot
			BlockRequester blockRequester = this.blockRequesters.get(peerId);
			if (blockRequester!=null)
				blockRequester.shutdown();
			
			neighbor.shutdown();
			
			this.blockRequesters.remove(peerId);
			
			this.neighbors.remove(peerId);
		}
		
		//sets next try for obtaining upload slot
		Candidate candidate = this.candidates.get(peerId);
		if (candidate!=null)
			candidate.setTimeNextSubscribe(timeForNextRequestMillis);
		
	}

	public void removeUsGranterKeepNeighbor(final PeerId peerId, final long timeNextSubscribeMillis) {
		
		if (logger.isDebugEnabled()) logger.debug("removeUsGranterKeepNeighbor("+peerId+","+timeNextSubscribeMillis+") in ["+this+"]");
	
		Candidate candidate = this.candidates.get(peerId);
		Neighbor neighbor = this.neighbors.get(peerId);

		if (candidate==null) {
			logger.warn("removed from an upload slot of a peer which is not a candidate! not allowed!");
			//maybe get rid of the peer and start all over
			return;
		}
		if (neighbor==null) {
			logger.warn("removed from an upload slot of a peer which is not a neighbor! not allowed!");
			//maybe get rid of the peer and start all over
			return;
		}
		
		BlockRequester blockRequester = this.blockRequesters.get(peerId);
		if (blockRequester!=null)
			blockRequester.shutdown();
		else
			logger.warn("trying to shutdown ["+peerId+" on "+this+"] found no block requesters");
		
		this.blockRequesters.remove(peerId);
		
		neighbor.setTimeOutMillis(timeNextSubscribeMillis);
		
	}
	
	void removeTimedOutNeighbors() {
		Set<PeerId> neighborsToRemove = new HashSet<PeerId>();
		
		for (Neighbor neighbor : this.neighbors.values()) {
			if (logger.isDebugEnabled()) logger.debug("checking timeout of neighbor ["+neighbor+"]");
			
			if (neighbor.isTimedOut())
				neighborsToRemove.add(neighbor.getPeerId());
		}
		for (PeerId neighborPeerId : neighborsToRemove) {
			if (logger.isDebugEnabled()) logger.debug("neighbor ["+neighborPeerId+"] has timed out! removing it from neighbor list.");

			this.removeNeighbor(neighborPeerId, 0);
			
		}
	}

	public Segment getSegment() {
		return segment;
	}

	public boolean isCandidate(PeerId peerId) {
		return this.candidates.containsKey(peerId);
	}

	public boolean isNeighbor(PeerId peerId) {
		return this.neighbors.containsKey(peerId);
	}

	private void sendSubscribe() throws InterruptedException {

		TreeSet<Candidate> candidatesToSendSubscribe = this.getCandidatesToSendSubscribe();
		
		if (candidatesToSendSubscribe==null || candidatesToSendSubscribe.isEmpty()) {
			if (logger.isDebugEnabled()) logger.debug("no candidates to send Subscribe");
		}
		else {
			Iterator<Candidate> iter = candidatesToSendSubscribe.descendingIterator();
			while (iter.hasNext()) {
				Candidate candidate = iter.next();
				if (logger.isDebugEnabled()) logger.debug("will send Subscribe to candidate "+candidate);
	
				candidate.setTimeLastSubscribeNow();
	
				this.tuner.getVideoSignaling().sendSubscribe(this.segment.getSegmentIdentifier(), candidate.getPeerId());
				
				//avoids requesting to too many peers before knowing if they grant us a slot
				Thread.sleep(30);
				
				if (this.hasEnoughUsGranters()) {
					if (logger.isDebugEnabled()) logger.debug("hasEnoughUsGranters returned true, stopping sending Subscribes");
					break;
				}
			}
		}
		
		if (logger.isDebugEnabled()) logger.debug("is done");

	}

	void removeLameCandidatesAndNeighbors() {
		
		if (logger.isDebugEnabled()) logger.debug("running");
		
		
		//candidates that were tried several times but never gave us anything
		//will leave space for new ones to be loaded from the DT
		Set<Candidate> candidatesToRemove = new HashSet<Candidate>();
		for (Candidate candidate: this.candidates.values()) {
			if (!candidate.isCleanUpPrevented() && candidate.shouldBeRemoved()) {
				if (logger.isDebugEnabled()) logger.debug("candidate ("+candidate+") shouldBeRemoved and will");
				
				candidatesToRemove.add(candidate);
			}
		}
		for (Candidate candidate: candidatesToRemove) {
			this.removeCandidate(candidate.getPeerId());
		}
		
		//neighbors with high delay (low chance of becoming interesting soon)
		//TODO should be highest... maybe get rid of top 20% ? keep a certain number... but not the best, we may be bad enough that all the best ones reject us
		//SortedSet<Neighbor> sortedNeighbors = new TreeSet<Neighbor>();
		
		long playTime = this.tuner.getApplication().getPlayTimeMillis();
		if (SegmentIdentifier.getSegmentNumber(playTime)==this.segment.getSegmentIdentifier().getSegmentNumber()) {
			int playBlockNumber = SegmentBlock.getBlockNumber(playTime);
			if (playTime > -1) {
				Map<Neighbor, Integer> neighborsWithHighDelay = new HashMap<Neighbor, Integer>();
				for (Neighbor neighbor : this.neighbors.values()) {
					int delayS = neighbor.getDelay(playBlockNumber);
					
					if (logger.isDebugEnabled()) logger.debug("checking minimum playout lag of neighbor ("+neighbor+") = ("+delayS+")");
					
					if (neighbor.getCandidate().isCleanUpPrevented()) {
						if (logger.isDebugEnabled()) logger.debug("neighbor ("+neighbor+") = ("+delayS+") isCleanUpPrevented");
						continue;
					}
					
					if (delayS > NEIGHBOR_HIGH_DELAY_SECONDS)
						neighborsWithHighDelay.put(neighbor,delayS);
					//else
					//	sortedNeighbors.add(neighbor);
				}
			
				for (Entry<Neighbor, Integer> entry : neighborsWithHighDelay.entrySet()) {
					PeerId peerId = entry.getKey().getPeerId();
					if (!this.isUsGranter(peerId)) {
						if (logger.isDebugEnabled()) logger.debug("removing neighbor ("+peerId+") because play delay is too high ("+entry.getValue()+">"+NEIGHBOR_HIGH_DELAY_SECONDS+")");
	
						//disconnect the neighbor (downgrade to candidate, will try again later)
						this.tuner.getVideoSignaling().sendDisconnect(this.segment.getSegmentIdentifier(), peerId, true, false);
						this.removeNeighbor(peerId, Math.min(entry.getValue()*1000,10000));
						
					}
				}
			}
		}
		
	}
	
	/**
	 * returns a set of Candidates which should be sent a Subscribe message (therefore potentially becoming neighbors)
	 * 
	 * gives priority to new ones
	 * 
	 * @return
	 */
	private TreeSet<Candidate> getCandidatesToSendSubscribe() {

		if (this.hasEnoughUsGranters()) {
			if (logger.isDebugEnabled()) logger.debug("hasEnoughUsGranters returned true, skipping");

			return null;
		}
		else 
			if (logger.isDebugEnabled()) logger.debug("hasEnoughUsGranters returned false, running");

		int currentNeighbors = this.getNeighbors().size();
		int newNeighborsNeeded = currentNeighbors==0?NeighborList.NEIGHBORS_PER_SEGMENT_INITIAL:(NeighborList.NEIGHBORS_PER_SEGMENT_INITIAL-currentNeighbors>NeighborList.NEIGHBORS_PER_SEGMENT_INCREMENT?NeighborList.NEIGHBORS_PER_SEGMENT_INITIAL-currentNeighbors:NeighborList.NEIGHBORS_PER_SEGMENT_INCREMENT);
		
		if (currentNeighbors>=NeighborList.NEIGHBORS_PER_SEGMENT_MAXIMUM) {
			logger.warn("we need more neighbors but we reached the max! "+currentNeighbors+">="+NeighborList.NEIGHBORS_PER_SEGMENT_MAXIMUM);
			
			//TODO kick out some of them? at the moment, waits for them to timeout and relies that other candidates will be tried
			
			return null;
		}
		
		newNeighborsNeeded = Math.min(newNeighborsNeeded+currentNeighbors,NeighborList.NEIGHBORS_PER_SEGMENT_MAXIMUM);

		if (newNeighborsNeeded <= 0) {
			if (logger.isDebugEnabled()) logger.debug("no new neighbors needed");
			
			return null;
		}
		else {
			if (logger.isDebugEnabled()) logger.debug(newNeighborsNeeded+" new neighbors needed");
		
			TreeSet<Candidate> candidatesCompared = new TreeSet<Candidate>();
			for (Candidate candidate : this.candidates.values()) 
				if (candidate!=null) {
					//if (logger.isDebugEnabled()) logger.debug("will consider sending US requests to: "+candidate);
					if (!this.isNeighbor(candidate.getPeerId()) && candidate.mayBeSentSubscribe()) {
						candidatesCompared.add(candidate);
					}
				}

			TreeSet<Candidate> candidatesToReturn = new TreeSet<Candidate>();
			Iterator<Candidate> iter = candidatesCompared.descendingIterator();  //highest priority ones
			while (iter.hasNext()) {
				Candidate candidate = iter.next();
				if (candidate!=null) {
					candidatesToReturn.add(candidate);
					newNeighborsNeeded--;
				}
			}
			
			return candidatesToReturn;
		}
	}
	
	/**
	 * needs new neighbors if there are not enough granters
	 * 
	 * @return
	 */
	private boolean hasEnoughUsGranters() {
		
		Tuner tuner = this.tuner;
		if (tuner==null)
			return true;
		
		VideoPlayer videoPlayer = tuner.getVideoPlayer();
		if (videoPlayer==null)
			return true;
		
		int goodRate = this.segment.getSegmentIdentifier().getChannel().getNumSubstreams()*SegmentBlock.getBlocksPerSecond();
		float incomingRate = this.getIncomingBlockRate();
		boolean playRateOk = videoPlayer.isCurrentRateEnough();
		boolean hasNoScheduledBlocks = tuner.getBlockRequestQueue().isEmpty();
		int numBlockRequesters = this.blockRequesters.size();
		
		if (logger.isDebugEnabled())
			logger.debug("play rate OK:"+playRateOk+" | hasNoScheduledBlocks="+hasNoScheduledBlocks+" | incoming rate: "+incomingRate +">="+goodRate+" | BR:"+numBlockRequesters );
		
		if (hasNoScheduledBlocks)
			return true;
/*
		if (this.blockRequesters.size() < TARGET_NUM_GRANTERS_PER_SCHEDULED_BLOCK)
			return false;
*/
		//if can't play, doesn't wait for the moving average below (tries to find the missing block)
		if (!playRateOk)
			return false;
		
		//if there are scheduled blocks but no ganters (reacts faster because the incoming rate is moving average)
		if (numBlockRequesters==0 && !hasNoScheduledBlocks)
			return false;
		
		//rate-based
		if (incomingRate >= goodRate)
			return true;
		else
			return false;
		/*
		//at least 2 granters per scheduled block -- the problem is that many times noone has the newest blocks

		List<BlockRequest> scheduledBlocks = this.tuner.getBlockRequestQueue().snapshot();
		for (BlockRequest blockRequest : scheduledBlocks) {
			if (this.segment.getSegmentIdentifier().equals(blockRequest.getSegmentIdentifier())) {
				
				int count = 0;
				for (BlockRequester blockRequester : this.blockRequesters.values()) {
					if (blockRequester.getNeighbor().getBlockMap().get(blockRequest.getBlockNumber())) {
						count++;
					}
				}
				if (count < TARGET_NUM_GRANTERS_PER_SCHEDULED_BLOCK)
					return false;
			}
		}
		
		return true;
		*/
	}
	
	private boolean isUsGranter(PeerId peerId) {
		return this.blockRequesters.containsKey(peerId);
	}

	public Neighbor getNeighbor(PeerId peerId) {
		return this.neighbors.get(peerId);
	}
	
	Map<PeerId, Neighbor> getNeighbors() {
		return this.neighbors;
	}

	void shutdown() {
		
		if (this.isShutDown.compareAndSet(false, true)) {
		
			if (logger.isDebugEnabled()) logger.debug("about to shutdown: "+this.toString());
			
			//stops tasks
			this.tunedSegmentMaintenanceExecutorService.shutdown();
	
			//sends DISCONNECT to all neighbors
			Set<PeerId> neighborsToRemove = new HashSet<PeerId>(this.neighbors.size());
			for(PeerId peerId : this.neighbors.keySet()) {
				this.tuner.getVideoSignaling().sendDisconnect(this.segment.getSegmentIdentifier(), peerId, true, false);
		
				//removes as candidate, neighbor, usgranter
				neighborsToRemove.add(peerId);
			}
			
			for(PeerId peerId: neighborsToRemove)
				this.removeCandidate(peerId);
			
			//kills BlockRequesters
			for (BlockRequester blockRequester : this.blockRequesters.values()) {
				blockRequester.shutdown();
			}

			if (logger.isDebugEnabled()) logger.debug("done shutdown: "+this.toString());
			
		}
		else {
			if (logger.isDebugEnabled()) logger.warn("is already or in the process of shutdown: "+this.toString());
		}
	}

	public Set<PeerId> getNeighborsPeerIds() {
		return this.neighbors.keySet();
	}


	public void signalFailure(final P2PAddress p2pAddress) {
		
		if (logger.isDebugEnabled()) logger.debug("signaling failure for p2paddress "+p2pAddress);

		Set<PeerId> candidatesToRemove = new HashSet<PeerId>();
		Set<PeerId> candidatesToSendDisconnect = new HashSet<PeerId>();

		for (PeerId peerId : this.candidates.keySet())
			if (peerId!=null)
				if (peerId.getDhtId().equals(p2pAddress)) {
					
					MovingAverage failureCounter = this.failureCounter.get(peerId);
					
					if (failureCounter == null) {
						logger.warn("no failure counter for p2paddress "+p2pAddress);
						return;
					}
					failureCounter.inputValue(1);
					
					if (logger.isDebugEnabled()) logger.debug("failureCounter of peerId "+peerId+" is "+failureCounter);
					
					if (failureCounter.getSum()>=FAILURES_PER_WINDOW_THRESHOLD) {
						if (logger.isDebugEnabled()) logger.debug("failureCounter of peerId "+peerId+" is above threshold ("+FAILURES_PER_WINDOW_THRESHOLD+"), marking to remove as neighbor");

						candidatesToRemove.add(peerId);
						if (failureCounter.getSum()==FAILURES_PER_WINDOW_THRESHOLD)
							candidatesToSendDisconnect.add(peerId);
					}
			}
			

		for (PeerId peerId : candidatesToRemove) {
			this.removeCandidate(peerId);
			if (candidatesToSendDisconnect.contains(peerId))
				this.tuner.getVideoSignaling().sendDisconnect(this.segment.getSegmentIdentifier(), peerId, true, false);
		}
			
	}

	public void signalFailure(final PeerId peerId) {

		if (logger.isDebugEnabled()) logger.debug("signaling failure for PeerID "+peerId);
		
		Set<PeerId> candidatesToRemove = new HashSet<PeerId>();
		Set<PeerId> candidatesToSendDisconnect = new HashSet<PeerId>();

		for (PeerId candidatePeerId : this.candidates.keySet())
			if (candidatePeerId!=null)
				if (candidatePeerId.equals(peerId)) {					
					MovingAverage failureCounter = this.failureCounter.get(candidatePeerId);
					
					if (failureCounter == null) {
						logger.warn("no failure counter for PeerID "+peerId);
						return;
					}
					failureCounter.inputValue(1);
					
					if (logger.isDebugEnabled()) logger.debug("failureCounter of peerId "+candidatePeerId+" is "+failureCounter);
					
					if (failureCounter.getSum()>=FAILURES_PER_WINDOW_THRESHOLD) {
						if (logger.isDebugEnabled()) logger.debug("failureCounter of peerId "+peerId+" is above threshold ("+FAILURES_PER_WINDOW_THRESHOLD+"), marking to remove as neighbor");

						candidatesToRemove.add(candidatePeerId);
						if (failureCounter.getSum()==FAILURES_PER_WINDOW_THRESHOLD)
							candidatesToSendDisconnect.add(candidatePeerId);
					}
				}
		
		for (PeerId candidatePeerId : candidatesToRemove) {
			this.removeCandidate(candidatePeerId);
			if (candidatesToSendDisconnect.contains(candidatePeerId))
				this.tuner.getVideoSignaling().sendDisconnect(this.segment.getSegmentIdentifier(), candidatePeerId, true, false);
		}
		
	}
	/*
	void pingCandidates() {
	
		for (Candidate candidate : this.candidates.values())
			if (candidate!=null)
				if (candidate.shouldPing()) {
					if (logger.isDebugEnabled()) logger.debug("pinging candidate "+candidate);
					
					candidate.setLatencyMillis(this.tuner.getVideoSignaling().sendPingGetLatency(candidate.getPeerId()));
					
					this.candidatesCompared.remove(candidate);  //needs to remove it and add again to reorder
					this.candidatesCompared.add(candidate);
				}
				else
					if (logger.isDebugEnabled()) logger.debug("should NOT ping candidate "+candidate);
		
	}
	*/
	private void checkInterestingNeighborsAndSendInterested() {
		
		if (this.hasEnoughUsGranters()) {
			if (logger.isDebugEnabled()) logger.debug("hasEnoughUsGranters returned true, skipping");
			return;
		}
		else 
			if (logger.isDebugEnabled()) logger.debug("hasEnoughUsGranters returned false, running");

		if (this.segment.getSegmentIdentifier().getStartTimeMS() < Clock.getMainClock().getTimeInMillis(false)) {
			
			TreeSet<Neighbor> sortedNeighbors = new TreeSet<Neighbor>(this.neighbors.values());
			Iterator<Neighbor> reverseIterator = sortedNeighbors.descendingIterator();
			while (reverseIterator.hasNext()) {
				Neighbor neighbor = reverseIterator.next();
				if (neighbor!=null)
					this.checkInterestAndSendInterested(neighbor);
			}
		}
		else
			if (logger.isDebugEnabled()) logger.debug("not running yet, segment starts in the future: "+this.getSegment().getSegmentIdentifier());			
	}
	
	private void checkInterestAndSendInterested(final Neighbor neighbor) {
		
		boolean hasEnoughUsGranters = this.hasEnoughUsGranters();

		if (logger.isDebugEnabled()) logger.debug("checking interest for neighbor "+neighbor);
		
		boolean isInterestingNow = this.tuner.getBlockRequestQueue().hasInterestingBlocks(neighbor);
				
		//takes also activity into account, since it happens that every time this runs it looks uninteresting because blocks are being downloaded as soon as the neighbor has them
		if (!isInterestingNow && neighbor.isInteresting() && neighbor.getLastDownloadAgoMillis() < NEIGHBOR_SEND_NOT_INTERESTED_AFTER_NOT_INTERESTING_FOR_MILLIS)
			isInterestingNow=true;
		
		boolean shouldSendNotInterested = neighbor.shouldSendNotInterestedYet(isInterestingNow);
		
		if (isInterestingNow && !neighbor.isInteresting()) {
			
			if (hasEnoughUsGranters) {
				if (logger.isDebugEnabled()) logger.debug("neighbor is interesting, but we haveEnoughUsGranters "+neighbor);
			}
			else {
				if (logger.isDebugEnabled()) logger.debug("decided to send INTERESTED to ["+neighbor+"]");
				neighbor.setInteresting(true);
				neighbor.setLastDownloadNow();
				
				ExecutorPool.getGeneralExecutorService().submit(new Runnable() {
					@Override
					public void run() {
						try {
							if (!tuner.getVideoSignaling().sendInterested(segment.getSegmentIdentifier(), neighbor.getPeerId(), false)) {
								logger.warn("sending INTERESTED to ["+neighbor+"] failed");
								neighbor.setInteresting(false);
							}
						} catch (Exception e) {
							// just so it doesn't die silently if an unhandled exception happened
							logger.error("error sending INTERESTED to ["+neighbor+"] :"+e.getMessage());
							e.printStackTrace();
						}
					}
				});
			}
		}
		else if (!isInterestingNow && neighbor.isInteresting()) {
			
			//about to time out
			if (shouldSendNotInterested) {
				if (logger.isDebugEnabled()) logger.debug("["+neighbor+"] is for quite some time not interesting anymore");
			}
			
			/* already timed out for sure if tdif>tout
			if (this.isUsGranter(neighbor.getPeerId()) && neighbor.getLastDownloadAgoMillis() > UploadSlot.INACTIVITY_TIMEOUT_S*1000) {
				if (logger.isDebugEnabled()) logger.debug("["+neighbor+"] is about to time out since the last download "+neighbor.getLastDownloadAgoMillis() +">"+ (UploadSlot.INACTIVITY_TIMEOUT_S*1000));
				shouldSendNotInterested=true;
			}
			*/
			
			if (shouldSendNotInterested) {
				if (logger.isDebugEnabled()) logger.debug("decided to send NOT_INTERESTED to ["+neighbor+"]");
				neighbor.setInteresting(false);
				this.tuner.getVideoSignaling().sendInterested(this.segment.getSegmentIdentifier(), neighbor.getPeerId(), true);
			}
			else
				if (logger.isDebugEnabled()) logger.debug("decided not to send anything to ["+neighbor+"] (a) isInterestingNow="+isInterestingNow+" shouldSendNotInterested="+shouldSendNotInterested);

		}
		else
			if (logger.isDebugEnabled()) logger.debug("decided not to send anything to ["+neighbor+"] (b) isInterestingNow="+isInterestingNow+" shouldSendNotInterested="+shouldSendNotInterested);
	}

	public void checkInterestAndSendInterested(final PeerId peerId) {
		Neighbor neighbor = this.neighbors.get(peerId);
		
		if (neighbor!=null && !neighbor.isInteresting())
			this.checkInterestAndSendInterested(neighbor);
	}

	Candidate getCandidate(PeerId peerId) {
		return this.candidates.get(peerId);
	}
	
	public Set<PeerId> getUsGranters() {
		return this.blockRequesters.keySet();
	}

	public Set<PeerId> getCandidates() {
		return this.candidates.keySet();
	}
	
	/**
	 * returns the candidates that gave us blocks in this tunedsegment
	 * useful to be passed to the next tunedsegment, so we try to keep the same neighborhood
	 * 
	 * @return
	 */
	Set<PeerId> getTopCandidates() {
		//TODO? have a threshold > 0?
		Set<PeerId> out = new HashSet<PeerId>();
		for (Candidate candidate : this.candidates.values())
			if (candidate.getNumReceivedBlocks() > 0)
				out.add(candidate.getPeerId());
		return out;
	}

	void incrementReceivedBlocks(final PeerId peerId) {
		
		//increments also segment incoming block rate
		this.incomingBlockRate.inputValue(1);
		
		Candidate candidate = this.candidates.get(peerId);
		if (null==candidate) {
			if (logger.isDebugEnabled()) logger.debug("candidate not found when incrementReceivedBlocks("+peerId+")");
			return;
		}
		candidate.incrementNumReceivedBlocks();
	}
	
	/**
	 * looks for potential new neighbors for the tuned segment
	 * 
	 * @param tunedSegment
	 * @return whether to requestUploadSlots now (true) or not (false - it may
	 *         do it once the operation is done)
	 */
	boolean searchCandidates() {

		if (this.hasEnoughUsGranters()) {
			if (logger.isDebugEnabled()) logger.debug("hasEnoughUsGranters returned true, skipping");

			return false;
		}
		
		int candidateCount = this.candidates.size();
		if (candidateCount >= NeighborList.CANDIDATES_PER_SEGMENT_MAXIMUM) {
			logger.warn("maximum of "+NeighborList.CANDIDATES_PER_SEGMENT_MAXIMUM+" candidates reached. not looking for more.");

			return true;
		}
		final int candidatesNeeded = NeighborList.CANDIDATES_PER_SEGMENT_INITIAL < candidateCount?NeighborList.CANDIDATES_PER_SEGMENT_INCREMENT:(NeighborList.CANDIDATES_PER_SEGMENT_INITIAL - candidateCount);
		
		if (candidatesNeeded > 0) {

			if (logger.isDebugEnabled())
				logger.debug(" needs "+candidatesNeeded+" new candidates (has " + candidateCount + "/" + NeighborList.CANDIDATES_PER_SEGMENT_INITIAL + "+) in " + this.toString());

			
			if (this.trackerGettingCandidatesSince>-1 && this.trackerGettingCandidatesSince + TRACKER_GETTING_CANDIDATES_TIME_LIMIT_MILLIS > Clock.getMainClock().getTimeInMillis(false)) {
				logger.debug(" is still waiting for the tracker, not requesting more candidates now");
				
				return false;
			}
			
			this.trackerGettingCandidatesSince = Clock.getMainClock().getTimeInMillis(false);
			
			// LOOKS IN THE DHT for candidates
			// List<PeerId> peers = new ArrayList<PeerId>();
			try {
				FutureTracker ft = this.tuner.getDht().getPeerList(this.getSegment().getSegmentIdentifier(), candidatesNeeded);
				ft.addListener(new BaseFutureAdapter<FutureTracker>() {

					@Override
					public void operationComplete(FutureTracker future) throws Exception {
						
						trackerGettingCandidatesSince = -1;
						
						if (future.isSuccess()) {
							Set<PeerId> result = fillPeerID(segment.getSegmentIdentifier(), candidatesNeeded, future.getTrackers());
							receiveCandidates(result);
						} else {
							logger.error("error in searchCand: " + future.getFailedReason());
						}
						
						Runnable runner = new Runnable() {
							
							@Override
							public void run() {

								try {
									if (logger.isDebugEnabled()) logger.debug("will try lock");
									if (searchCandidatesLock.tryLock()) {
										try {
											if (logger.isDebugEnabled()) logger.debug("got lock");
											
											sendSubscribeAndInterested();
										} catch (InterruptedException e) {
											logger.warn("interrupted !?");
										}
										finally {
											searchCandidatesLock.unlock();
										}
									}
									else {
										if (logger.isDebugEnabled()) logger.debug("lock was already locked");
									}
									
									if (logger.isDebugEnabled()) logger.debug("is done");
								}
								catch (Exception e) {
									logger.error("got exception when requestUploadSlotsAndSendUsUpdates: "+e.getMessage());
									e.printStackTrace();
								}
							}
						};
						
						ExecutorPool.getGeneralExecutorService().execute(runner);
						
					}
				});

			} catch (IOException e) {
				logger.warn(" I/O error when getting candidates from DHT");

				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}

			return false;
		} else {
			if (logger.isDebugEnabled())
				logger.debug("already have enough candidates (" + candidateCount + "/" + NeighborList.CANDIDATES_PER_SEGMENT_INITIAL + ")");
			return true;
		}
	}

	//TODO move this to Peer.java (since it deals with TrackerData)
	private Set<PeerId> fillPeerID(final SegmentIdentifier segmentIdentifier, final int howMany, final Collection<TrackerData> collection) throws ClassNotFoundException, IOException {
		
		if (logger.isDebugEnabled())
			logger.debug("got " + collection.size() + " results from DT: " + collection.toString() + " for si:" + segmentIdentifier+" (will filter them and reduce to "+howMany+")");

		if (collection.size()==0)
			return null;
		
		Set<PeerId> incomingPeers = new HashSet<PeerId>(collection.size() - 1);
		Set<PeerId> currentCandidates = this.candidates.keySet();

		PeerId myPeerId = this.tuner.getDht().getMyId();

		for (Iterator<TrackerData> iter = collection.iterator(); iter.hasNext();) {
			TrackerData trackerData = iter.next();
			byte[] attachment = trackerData.getAttachement();
			if (attachment!=null) {
				
				String candidateName = new String(attachment); 
				
				PeerId pid = new PeerId(trackerData.getPeerAddress(),candidateName);
				
				if (!pid.equals(myPeerId) && !currentCandidates.contains(pid)) {
					incomingPeers.add(pid);
				}
			}
			else {
				logger.warn("null attachment! this shouldn't happen, maybe TomP2P has problems with attachments?");
			}
		}
		
		int usefulPeersCount = incomingPeers.size();

		if (howMany >= usefulPeersCount)
			return incomingPeers;

		//returns random subset
		int resultSize = 0;
		Set<PeerId> result = new HashSet<PeerId>();
		
		for (Iterator<PeerId> iter = incomingPeers.iterator(); iter.hasNext() && resultSize < howMany;) {
			PeerId peerId = iter.next();

			if (Utils.getRandomFloat() < (float)howMany/usefulPeersCount) {
				result.add(peerId);
				resultSize++;
			}
			usefulPeersCount--;
			
		}
		return result;
	}
	
	private void sendSubscribeAndInterested() throws InterruptedException {

		// requests upload slots -- if we don't have enough yet
		this.sendSubscribe();

		// sends UsUpdates
		this.checkInterestingNeighborsAndSendInterested();
	}

	private void receiveCandidates(final Set<PeerId> peers) {
		if (peers==null||peers.isEmpty()) {
			if (logger.isDebugEnabled())
				logger.debug(" no new candidates for this segment for " + this.getSegment().getSegmentIdentifier());
		}
		else {
			Iterator<PeerId> iter = peers.iterator();

			PeerId peerId;
			while (iter.hasNext()) {

				peerId = iter.next();

				if (logger.isDebugEnabled())
					logger.debug(" got candidate from DHT: " + peerId.toString());

				// adds if it's not already there
				if (this.isCandidate(peerId)) {
					if (logger.isDebugEnabled())
						logger.debug(" was already a candidate for " + this.getSegment().getSegmentIdentifier() + " : " + peerId.toString());
				} else if (this.isNeighbor(peerId)) {
					if (logger.isDebugEnabled())
						logger.debug(" was already a neighbor for " + this.getSegment().getSegmentIdentifier() + " : " + peerId.toString());
				} else {
					if (logger.isDebugEnabled())
						logger.debug(" was NOT alread a candidate for " + this.getSegment().getSegmentIdentifier() + " : " + peerId.toString() + " - adding");

					this.addCandidate(peerId);
				}
			}
		}
	}

	public float getIncomingBlockRate() {
		return this.incomingBlockRate.getAverage(1,2);
	}

	public void blockDownloaded(final PeerId sender, final int blockNumber) {
		BlockRequester blockRequester = this.blockRequesters.get(sender);
		if (blockRequester==null)
			return;
		blockRequester.blockDownloaded(blockNumber);
	}

	public void removeFromInterested(final PeerId peerId, final int timeoutMillis) {
		Neighbor neighbor = this.neighbors.get(peerId);

		if (neighbor!=null) {
			
			//if it is granting an upload slot
			BlockRequester blockRequester = this.blockRequesters.get(peerId);
			if (blockRequester!=null)
				blockRequester.shutdown();
			this.blockRequesters.remove(peerId);
			
			neighbor.setInteresting(false);
			neighbor.setTimeOutMillis(timeoutMillis);
		}
	}
	
	public int howManyNeighborsHaveBlock(int blockNumber) {
		int count = 0;
		for (Neighbor neighbor : this.neighbors.values()) {
			if (neighbor.hasBlock(blockNumber)) {
				count++;
			}
		}
		return count;
	}

	public int howManyUsGrantersHaveBlock(int blockNumber) {
		int count = 0;
		for (BlockRequester blockRequester : this.blockRequesters.values()) {
			if (blockRequester.hasBlock(blockNumber)) {
				count++;
			}
		}
		return count;
	}
}