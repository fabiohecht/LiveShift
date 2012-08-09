package net.liveshift.download;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import net.liveshift.configuration.Configuration;
import net.liveshift.core.PeerId;
import net.liveshift.p2p.P2PAddress;
import net.liveshift.signaling.messaging.SegmentBlock;
import net.liveshift.storage.Segment;
import net.liveshift.storage.SegmentBlockMap;
import net.liveshift.storage.SegmentIdentifier;
import net.liveshift.time.Clock;
import net.liveshift.util.TriggableScheduledExecutorService;
import net.liveshift.video.VideoPlayer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 * Keeps a neighbor list for a group of SegmentDescriptions
 * 
 * Candidates are peers that came from the DHT, they are periodically asked for
 * an upload slot Neighbors are peers that have put this peer in their upload
 * queue. this peer has received its block map and receives updates in form of
 * have messages. eventually it may grant an upload slot.
 * 
 * @author fabio, draft
 * @author Kevin Leopold
 * 
 */
public class NeighborList {

	final private static Logger	logger = LoggerFactory.getLogger(NeighborList.class);

	final static long TUNED_SEGMENT_MAINTENANCE_FREQUENCY_MILLIS = 1000L;
	private static final int GRANT_BEFORE_WAIT_RETRIES = 4; // sort of obsolete with the new flow control
	private static final long GRANT_BEFORE_WAIT_DELAY_MS = 50L; // * try

	final static int CANDIDATES_PER_SEGMENT_INITIAL = 50;
	static final int CANDIDATES_PER_SEGMENT_INCREMENT = 5;
	static final int CANDIDATES_PER_SEGMENT_MAXIMUM = 150;

	final static int NEIGHBORS_PER_SEGMENT_INITIAL = 25;
	static final int NEIGHBORS_PER_SEGMENT_INCREMENT = 25;
	static final int NEIGHBORS_PER_SEGMENT_MAXIMUM = 100;

	private final ConcurrentHashMap<SegmentIdentifier, TunedSegment> tunedSegments; // current interest (segments from currently tuned channel)
	private final Tuner tuner;
	private final TriggableScheduledExecutorService tunedSegmentMaintenanceExecutorService;


	final private Callable<Boolean> tunedSegmentMaintenanceRunner = new Callable<Boolean>() {
		@Override
		public Boolean call() {
			try {
				//gets rid of expired tunedsegments
				checkAndRemoveTunedSegments();
				
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
			return true;
		}
	};

	public NeighborList(final Tuner tuner, final Configuration configuration) {

		if (logger.isDebugEnabled())
			logger.debug("in constructor");

		this.tuner = tuner;
		this.tunedSegments = new ConcurrentHashMap<SegmentIdentifier, TunedSegment>();

		// schedules tasks
		this.tunedSegmentMaintenanceExecutorService = new TriggableScheduledExecutorService(this.tunedSegmentMaintenanceRunner,
				200, 200, TUNED_SEGMENT_MAINTENANCE_FREQUENCY_MILLIS, Clock.getMainClock(), "CheckAndRemoveTunedSegments");
	}
	
	private void checkAndRemoveTunedSegments() {

		if (logger.isDebugEnabled()) logger.debug("in checkAndRemoveTunedSegments() -- we have " + this.tunedSegments.size() + " TunedSegments");

		Iterator<TunedSegment> iter = this.tunedSegments.values().iterator();

		while (iter.hasNext()) {
			TunedSegment tunedSegment = iter.next();

			// checks if tunedSegment is already all here
			if (isToRemoveTunedSegment(tunedSegment)) {

				if (logger.isDebugEnabled())
					logger.debug("removing tuned segment: " + tunedSegment.toString());

				// removes from tunedsegments
				tunedSegment.shutdown();
				iter.remove();
			}
		}
		
		if (logger.isDebugEnabled()) logger.debug("done checkAndRemoveTunedSegments()");
	}

	public boolean isSegmentTuned(SegmentIdentifier segmentIdentifier) {
		return this.tunedSegments.containsKey(segmentIdentifier);
	}

	@Override
	protected void finalize() throws Throwable {
		if (this.tunedSegmentMaintenanceExecutorService != null)
			this.tunedSegmentMaintenanceExecutorService.shutdown();

		super.finalize();
	}

	public void addSegment(final Segment segment) {

		if (logger.isDebugEnabled())
			logger.debug("adding " + segment.toString());

		if (this.tunedSegments.containsKey(segment.getSegmentIdentifier())) {
			logger.warn("refusing to add an already added segment: " + segment);
			return;
		}

		TunedSegment tunedSegment = new TunedSegment(segment, this.tuner, this);
		
		//adds current neighbors to new segment, that should help maintaining neigbors when switching when there are more offers than our candidate count
		SegmentIdentifier segmentIdentifier = segment.getSegmentIdentifier();
		TunedSegment oneBefore = this.tunedSegments.get(new SegmentIdentifier(segmentIdentifier.getChannel(), segmentIdentifier.getSubstream(), segmentIdentifier.getSegmentNumber()-1));
		if (oneBefore != null)
			for (PeerId peerId : oneBefore.getTopCandidates())
				tunedSegment.addCandidate(peerId);
		
		this.tunedSegments.put(segment.getSegmentIdentifier(), tunedSegment);
		
		tunedSegment.scheduleTasks();
	}

	public void reset() {
		if (logger.isDebugEnabled())
			logger.debug("will reset neighbor list -- will shutdown " + this.tunedSegments.size() + " tunedSegments");

		for (TunedSegment tunedSegment : this.tunedSegments.values())
			tunedSegment.shutdown();

		if (logger.isDebugEnabled())
			logger.debug("done shutting down tunedSegments");

		this.tunedSegments.clear();
	}


	void checkAndRemoveOldTunedSegment(final SegmentIdentifier segmentIdentifier) {
		// checks if tunedSegment is already all here
		TunedSegment tunedSegment = this.tunedSegments.get(segmentIdentifier);
		if (tunedSegment != null && isToRemoveTunedSegment(tunedSegment)) {
			tunedSegment.shutdown();
			this.tunedSegments.remove(tunedSegment.getSegment().getSegmentIdentifier());
		}
	}

	/**
	 * looks for potential new neighbors for the tuned segment
	 * 
	 * @param tunedSegment
	 * @return whether to requestUploadSlots now (true) or it will do it once
	 *         the operation is done (false)
	 */
	private boolean isToRemoveTunedSegment(final TunedSegment tunedSegment) {
		if (logger.isDebugEnabled())
			logger.debug("in isToRemoveTunedSegment(" + tunedSegment.toString() + ")");

		// first looks in local storage
		// if it's totally available locally, no need to look for neighbors for this segment!

		VideoPlayer videoPlayer = this.tuner.getVideoPlayer();
		if (videoPlayer == null) {
			logger.warn("no videoPlayer -- too early yet?");
			return false;
		}
		long playTime = videoPlayer.getLastPlayedBlocksStartTimeMs();
		long playSegmentNumber = SegmentIdentifier.getSegmentNumber(playTime);
		long thisSegmentNumber = tunedSegment.getSegment().getSegmentIdentifier().getSegmentNumber();

		SegmentBlockMap mySegmentBlockMap = tunedSegment.getSegment().getSegmentBlockMap();

		if (playSegmentNumber > thisSegmentNumber
				|| (playSegmentNumber == thisSegmentNumber && mySegmentBlockMap.isFull(SegmentBlock.getBlockNumber(playTime)))) {
			// I have all the blocks, I don't need stinky neighbors (or playclock passed it)
			if (logger.isDebugEnabled())
				logger.debug(" all blocks in the future are here already (or playtime is past: " + (playSegmentNumber > thisSegmentNumber) + ") -- removing tuned segment ("
						+ tunedSegment.toString() + ")");

			// unschedules this segment - it's all here already
			return true;
		}
		return false;
	}
	
	private Neighbor getNeighbor(SegmentIdentifier segmentIdentifier, PeerId peerId) {

		TunedSegment tunedSegment = this.tunedSegments.get(segmentIdentifier);

		if (tunedSegment == null) {
			logger.warn("TunedSegment " + segmentIdentifier + " not found when looking for neighbor " + peerId);

			return null;
		} else
			return tunedSegment.getNeighbor(peerId);
	}

	public boolean isNeighbor(SegmentIdentifier segmentIdentifier, PeerId peerId) {
		return this.getNeighbor(segmentIdentifier, peerId) != null;
	}

	/**
	 * returns all neighbors for tunedsegments with start time between the two
	 * given times
	 * 
	 * 
	 * @param timeMillis0
	 * @param timeMillis1
	 * @return
	 */
	public Set<PeerId> getNeighborsForSegmentsStartingBetween(long timeMillis0, long timeMillis1) {
		if (timeMillis0 > timeMillis1) {
			logger.warn(timeMillis0 + ">" + timeMillis1 + " but shouldn't");
			return null;
		}
		Set<PeerId> peerIds = new HashSet<PeerId>();
		long segmentStartTime;

		for (TunedSegment tunedSegment : this.tunedSegments.values()) {
			segmentStartTime = tunedSegment.getSegment().getSegmentIdentifier().getStartTimeMS();
			if (segmentStartTime >= timeMillis0 && segmentStartTime <= timeMillis1)
				peerIds.addAll(tunedSegment.getNeighborsPeerIds());
		}

		return peerIds;
	}

	public boolean isCandidate(PeerId peerId) {

		for (TunedSegment tunedSegment : this.tunedSegments.values())
			if (tunedSegment.isCandidate(peerId))
				return true;
		return false;
	}

	public boolean isInteresting(PeerId peerId) {
		for (TunedSegment tunedSegment : this.tunedSegments.values()) {
			Neighbor neighbor = tunedSegment.getNeighbor(peerId);

			if (neighbor != null && neighbor.isInteresting())
				return true;
		}
		return false;
	}

	/**
	 * updates one block in the block map - triggered by receiving HAVE
	 * 
	 * @param remoteNeighbor
	 * @param segmentIdentifier
	 * @param blockNumber
	 * @param doHave 
	 * @param rate rate expected rate at which new blocks are expected to arrive from blockNumber. -1=don't change the last prediction, 0=clear last prediction, >0=update the last prediction
	 */
	public void updateBlockMap(PeerId peerId, SegmentIdentifier segmentIdentifier, int blockNumber, boolean doHave, float rate) {
		Neighbor neighbor = this.getNeighbor(segmentIdentifier, peerId);
		if (neighbor != null) {
			neighbor.setBlockInBlockMap(blockNumber, doHave, rate);
		}
	}

	/**
	 * Rewards the given neighbor with increased reputation
	 * 
	 * @param peerId
	 */
	public void registerSuccess(PeerId peerId, SegmentBlock segmentBlock) {

		if (logger.isDebugEnabled())
			logger.debug("registering successful block sb:" + segmentBlock + " pid:" + peerId);

		Neighbor neighbor = this.getNeighbor(segmentBlock.getSegmentIdentifier(), peerId);

		if (neighbor == null)
			return;

		// marks activity
		neighbor.setLastDownloadNow();

		// Increase neighbors reputation
		this.tuner.getVideoSignaling().getIncentiveMechanism().increaseReputation(neighbor.getPeerId(), 1, segmentBlock.getTimeMillis());
		
		// increments counters
		this.incrementReceivedBlocks(peerId, segmentBlock.getSegmentIdentifier());
	}

	private void incrementReceivedBlocks(final PeerId peerId, final SegmentIdentifier segmentIdentifier) {
		TunedSegment tunedSegment  = this.tunedSegments.get(segmentIdentifier);
		if (null == tunedSegment)
			return;
		
		tunedSegment.incrementReceivedBlocks(peerId);
	}

	/**
	 * if a block failed to come TODO in the future, maybe punish peer, but
	 * it may be 
	 * 
	 * @param peerId
	 *            the sender, which has failed
	 * 
	 * @param segmentBlock
	 */
	public void registerFailure(final SegmentIdentifier segmentIdentifier, final int blockNumber, final PeerId peerId) {
		
		if (logger.isDebugEnabled())
			logger.debug("registering failed block si:" + segmentIdentifier + " b#" + blockNumber + " pid:" + peerId);

		// reschedules it
		if (logger.isDebugEnabled())
			logger.debug("rescheduling failed block si:" + segmentIdentifier + " b#" + blockNumber);
		this.tuner.getBlockRequestQueue().offer(new BlockRequest(segmentIdentifier, blockNumber));
	}

	/**
	 * called when a peer notifies us that we have an upload slot there
	 * 
	 * @param granter
	 * @param segmentIdentifier
	 * @param uploadSlotTimeoutS
	 */
	public void addUsGranter(final PeerId granter, final SegmentIdentifier segmentIdentifier, final long uploadSlotTimeoutMillis) {
		if (logger.isDebugEnabled())
			logger.debug("setUsGranted(" + granter + "," + segmentIdentifier + ")");

		// adds peer to set of peers uploading to us
		TunedSegment tunedSegment = this.tunedSegments.get(segmentIdentifier);

		if (tunedSegment == null) {
			if (logger.isDebugEnabled())
				logger.warn("GRANTED an upload slot that we don't want (anymore?)");

			// TODO think about what to do.. reply cancelling could be nice
		} else {
			if (logger.isDebugEnabled())
				logger.debug("received an upload slot that we actually want :)");

			int tries = 0;
			boolean success = false;

			while (!success && tries++ < GRANT_BEFORE_WAIT_RETRIES) {

				success = tunedSegment.setUploadSlotGranted(granter, uploadSlotTimeoutMillis);

				if (!success)
					try {
						Thread.sleep(GRANT_BEFORE_WAIT_DELAY_MS * tries);
					} catch (InterruptedException e) {
						if (logger.isDebugEnabled())
							logger.debug("interrupted");
						break;
					}
			}

			if (success) {
				if (logger.isDebugEnabled())
					logger.debug("added US in try " + tries);
			}
			else {
				logger.warn("failed to add US, tried " + tries + " times");

				// TODO what?

			}
		}
	}

	public void addNeighbor(final PeerId peerId, final SegmentIdentifier segmentIdentifier, final SegmentBlockMap segmentBlockMap, final long timeoutMillis) {
		if (logger.isDebugEnabled())
			logger.debug("addNeighbor(" + peerId + "," + segmentIdentifier + "," + segmentBlockMap + "," + timeoutMillis + ")");

		TunedSegment tunedSegment = this.tunedSegments.get(segmentIdentifier);
		if (tunedSegment == null) {
			logger.warn("WAIT an upload slot that we don't want (anymore?)");

			// TODO think about what to do.. reply cancelling could be good
		} else {
			tunedSegment.addNeighbor(peerId, segmentBlockMap, timeoutMillis);

			this.tunedSegmentMaintenanceExecutorService.scheduleNow();
		}
	}

	public void removeUsGranter(final PeerId peerId, final SegmentIdentifier segmentIdentifier, final long timeoutMillis) {

		TunedSegment tunedSegment = this.tunedSegments.get(segmentIdentifier);

		if (tunedSegment == null) {
			logger.warn("TunedSegment " + segmentIdentifier + " not found when trying to remove UsGranter " + peerId);
			if (logger.isDebugEnabled())
				logger.debug("current TunedSegments are: " + this.tunedSegments);
			return;
		}

		tunedSegment.removeUsGranterKeepNeighbor(peerId, timeoutMillis);
	}

	public void removeNeighbor(final PeerId peerId, final SegmentIdentifier segmentIdentifier, final long timeNextTryMillis) {

		TunedSegment tunedSegment = this.tunedSegments.get(segmentIdentifier);

		if (tunedSegment == null) {
			if (logger.isDebugEnabled())
				logger.debug("TunedSegment " + segmentIdentifier + " not found when trying to remove neighbor " + peerId);
			return;
		}

		tunedSegment.removeNeighbor(peerId, timeNextTryMillis);

	}

	/**
	 * removes peer from candidate set, neighbor set, and upload slot granters
	 * set
	 * 
	 * @param sender
	 * @param getsSegmentIdentifier
	 */
	public void removeFromAll(final PeerId peerId, final SegmentIdentifier segmentIdentifier) {
		TunedSegment tunedSegment = this.tunedSegments.get(segmentIdentifier);

		if (tunedSegment == null) {
			if (logger.isDebugEnabled())
				logger.debug("TunedSegment " + segmentIdentifier + " not found when trying to remove neighbor " + peerId);
			return;
		}

		tunedSegment.removeCandidate(peerId);
	}

	public void signalFailure(final P2PAddress p2pAddress) {

		if (logger.isDebugEnabled())
			logger.debug("failure signaled for p2pAddress:" + p2pAddress);

		Iterator<TunedSegment> iter = this.tunedSegments.values().iterator();

		while (iter.hasNext()) {
			TunedSegment tunedSegment = iter.next();

			tunedSegment.signalFailure(p2pAddress);
		}
	}

	public void signalFailure(final PeerId peerID) {

		if (logger.isDebugEnabled())
			logger.debug("failure signaled for PeerId:" + peerID);

		Iterator<TunedSegment> iter = this.tunedSegments.values().iterator();

		while (iter.hasNext()) {
			TunedSegment tunedSegment = iter.next();

			tunedSegment.signalFailure(peerID);
		}
	}

	/**
	 * returns whether the given peer is a neighbor and has the given block in
	 * the given segment
	 * 
	 * @param peerId
	 * @param segmentIdentifier
	 * @param blockNumber
	 * @return
	 */
	public boolean neighborHasBlock(final PeerId peerId, final SegmentIdentifier segmentIdentifier, final int blockNumber) {
		TunedSegment tunedSegment;
		if (null == (tunedSegment = this.tunedSegments.get(segmentIdentifier)))
			return false;
		Neighbor neighbor = tunedSegment.getNeighbor(peerId);
		if (neighbor == null)
			return false;
		
		if (neighbor.getBlockMap()==null) {
			logger.warn("I got a neighbor without a block map: "+neighbor+", that's shouldn't be the case");
			return false;
		}
		
		return neighbor.getBlockMap().get(blockNumber);
	}

	public void checkInterestAndSendInterested(final PeerId peerId, final SegmentIdentifier segmentIdentifier) {
		TunedSegment tunedSegment = this.tunedSegments.get(segmentIdentifier);
		if (tunedSegment != null)
			tunedSegment.checkInterestAndSendInterested(peerId);
	}

	public void addPotentialCandidate(final SegmentIdentifier segmentIdentifier, final PeerId peerId) {
		TunedSegment tunedSegment = this.tunedSegments.get(segmentIdentifier);
		if (tunedSegment == null) {
			//may happen because we are adding as candidates whoever requests slots from us 
			if (logger.isDebugEnabled())
				logger.debug("TS "+segmentIdentifier+" not found when addPotentialCandidate "+ peerId+", have: "+this.tunedSegments + " -- may happen normally when interest asymmetric");

			return;
		}
		if (!tunedSegment.isCandidate(peerId))
			tunedSegment.addCandidate(peerId);
		else if (!tunedSegment.isNeighbor(peerId)) {
			Candidate candidate = tunedSegment.getCandidate(peerId);
			candidate.setTimeNextSubscribe(1000L);
			
			//avoids that it gets rid of it almost immediately due to it potentially temporarily appear to have playback lag (peer will get the slot, request, receive, and send Have to us, which may take a little while)
			candidate.setCleanUpPrevented(1000L);
		}
	}

	public boolean isDoneWithPreviousTunedSegment(final SegmentIdentifier segmentIdentifier) {
		return null==this.tunedSegments.get(new SegmentIdentifier(segmentIdentifier.getChannel(), segmentIdentifier.getSubstream(), segmentIdentifier.getSegmentNumber()-1));
	}

	public Set<PeerId> getRecommendedPeers(SegmentIdentifier segmentIdentifier) {
		TunedSegment tunedSegment  = this.tunedSegments.get(segmentIdentifier);
		if (null == tunedSegment)
			return null;
		
		return tunedSegment.getTopCandidates();
	}

	public void blockDownloaded(final PeerId sender, final SegmentIdentifier segmentIdentifier, final int blockNumber) {
		TunedSegment tunedSegment  = this.tunedSegments.get(segmentIdentifier);
		if (null == tunedSegment)
			return;
		
		tunedSegment.blockDownloaded(sender, blockNumber);
	}

	public void removeFromInterested(final PeerId peerId, final SegmentIdentifier segmentIdentifier, final int timeoutMillis) {
		TunedSegment tunedSegment  = this.tunedSegments.get(segmentIdentifier);
		if (null == tunedSegment) {
			logger.warn("tuned segment not found ("+segmentIdentifier+")");
			return;
		}
		
		tunedSegment.removeFromInterested(peerId, timeoutMillis);

	}
	
	public int howManyNeighborsHaveBlock(final SegmentIdentifier segmentIdentifier, final int blockNumber) {
		TunedSegment tunedSegment  = this.tunedSegments.get(segmentIdentifier);
		if (null == tunedSegment) {
			logger.warn("tuned segment not found ("+segmentIdentifier+")");
			return -1;
		}
		
		return tunedSegment.howManyNeighborsHaveBlock(blockNumber);
	}

	public int howManyUsGrantersHaveBlock(SegmentIdentifier segmentIdentifier, int blockNumber) {
		TunedSegment tunedSegment  = this.tunedSegments.get(segmentIdentifier);
		if (null == tunedSegment) {
			logger.warn("tuned segment not found ("+segmentIdentifier+")");
			return -1;
		}
		
		return tunedSegment.howManyUsGrantersHaveBlock(blockNumber);

	}

}
