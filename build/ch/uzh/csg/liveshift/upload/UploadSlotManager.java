package net.liveshift.upload;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.liveshift.configuration.Configuration;
import net.liveshift.core.Channel;
import net.liveshift.core.PeerId;
import net.liveshift.download.NeighborList;
import net.liveshift.p2p.P2PAddress;
import net.liveshift.signaling.VideoSignaling;
import net.liveshift.signaling.messaging.BlockRequestMessage;
import net.liveshift.signaling.messaging.SegmentBlock;
import net.liveshift.signaling.messaging.BlockReplyMessage.BlockReplyCode;
import net.liveshift.storage.SegmentIdentifier;
import net.liveshift.time.Clock;
import net.liveshift.upload.VectorHaveCollector.VectorHaveSender;
import net.liveshift.util.ExecutorPool;
import net.liveshift.util.MovingAverage;


/**
 * Limits the the upload bandwidth
 * by only allowing a certain amount of peers to be served at the same time (in slots)
 * and queueing all other requests
 * 
 * A requesting peer can exchange a maximum number of requested data when gaining a slot (PER_PEER_REQUESTS).
 * 
 * @author Fabio Hecht, Thomas Bocek, Kevin Leopold
 *
 */
public class UploadSlotManager implements VectorHaveSender {
	
	final private static Logger logger = LoggerFactory.getLogger(UploadSlotManager.class);

	//private static final long PING_REQUESTERS_INTERVAL_MILLIS = 5000L;
	private static final float OVERHEAD_ACCELLERATION_FACTOR = .1F;
	private static final long MINIMUM_GRANT_TIME_BEFORE_PREEMPTION = 500L;
	private static final int WINDOW_SIZE = 5;

	private static final int INTIAL_UPLOAD_SLOTS = 5;
	protected static final int MINIMUM_UPLOAD_SLOTS = 1;
	protected static final float MINIMUM_UPLOAD_BANDWIDTH_USED = .5F;

    //holds the incoming upload slot requests
	private final SubscribersQueue subscribersQueue;
	
    //holds the incoming block requests
	private BlockRequestPipeline pipelineBlockRequest;
	
	//the slots (threads that consume requests from the queue)
	private final Map<Integer, UploadSlot> uploadSlots;
	private final Map<Subscriber, UploadSlot> grantedUploadSlots;
	
	//to send us_reply messages
	private final VideoSignaling videoSignaling;
	private int numUploadSlots;
	final float globalBlockUploadRateBandwidth;
	
	//to send vector haves
	final VectorHaveCollectors vectorHaveCollectors;
	
	private final Map<PeerId, MovingAverage> failureCounter;
	private final Map<PeerId, MovingAverage> sentBlocks;

	final private ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

	private Lock ratingLock = new ReentrantLock(true);
	final private MovingAverage achievedUploadRate;
	private final MovingAverage bandwidthAllocation = new MovingAverage(Clock.getMainClock(), 1, 1000);
	final private Map<Channel,MovingAverage> achievedUploadRatePerChannel = new HashMap<Channel,MovingAverage>();
	final private boolean noHaveSuppression;
	final private int numSubscribers;

	final String peerName;  //useful for debugging
	private NeighborList neighborList;  //for HAVE suppression

	private int lastUploadSlotNumber = 0;

	
	public UploadSlotManager(final VideoSignaling videoSignaling, final Configuration configuration) {
		this.videoSignaling = videoSignaling;
		
		this.globalBlockUploadRateBandwidth = configuration.getUploadRate();

		this.achievedUploadRate = new MovingAverage(Clock.getMainClock(), WINDOW_SIZE+1, 1000);
		
		logger.info("subscriber queue size is "+(configuration.numSubscribers));
		this.subscribersQueue = new SubscribersQueue(configuration.numSubscribers, configuration.uploadQueuePreempt, videoSignaling);
		
		this.uploadSlots = new ConcurrentHashMap<Integer, UploadSlot>(INTIAL_UPLOAD_SLOTS*2);
		this.grantedUploadSlots = new ConcurrentHashMap<Subscriber,UploadSlot>(4);
		this.failureCounter = new HashMap<PeerId, MovingAverage>();
		this.sentBlocks = new HashMap<PeerId, MovingAverage>();
		
		this.peerName = configuration.getPeerName();
		
		this.scheduleUploadSlotRequestTimeoutCheck(configuration.noDynamicUploadSlotManagement);
		this.vectorHaveCollectors = new VectorHaveCollectors(this, configuration.vectorHavesEnabled);
		
		this.noHaveSuppression = configuration.noHaveSuppression;
		this.numSubscribers = configuration.numSubscribers;
	}

	private void scheduleUploadSlotRequestTimeoutCheck(final boolean noDynamicUploadSlotManagement) {
		/*
		Runnable runner = new Runnable() {			
			@Override
			public void run() {
				try {
					pingRequesters();

				} catch (Exception e) {
					// just so it doesn't die silently if an unhandled exception happened
					e.printStackTrace();
				}

			}
		};		
		this.scheduledExecutorService.scheduleWithFixedDelay(runner, PING_REQUESTERS_INTERVAL_MILLIS, PING_REQUESTERS_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
		 */
		Runnable uploadSlotManagementRunner = new Runnable() {
			@Override
			public void run() {
				
				logger.info("achieved upload rate at "+ achievedUploadRate.getBoxTimeFloor(1) +" is "+achievedUploadRate.getAverage(1,1)+"/"+globalBlockUploadRateBandwidth);
				
				synchronized (achievedUploadRatePerChannel) {
					for (Entry<Channel, MovingAverage> channelMovingAverageEntry : achievedUploadRatePerChannel.entrySet()) {
						MovingAverage channelMovingAverage = channelMovingAverageEntry.getValue();
						float channelMovingAverageAverage =	channelMovingAverage.getAverage(1,1);
						if (channelMovingAverageAverage>0F)
							logger.info("achieved upload rate for channel ("+channelMovingAverageEntry.getKey().getName()+") at "+ channelMovingAverage.getBoxTimeFloor(1) +" is "+channelMovingAverageAverage+"/"+globalBlockUploadRateBandwidth);
					}
				}
				
				try {
					if (!noDynamicUploadSlotManagement) {

						//automatic upload slot adaptation
						float achievedRate= achievedUploadRate.getAverage();
						
						if (logger.isDebugEnabled()) logger.debug("avg="+achievedRate+" gur="+ globalBlockUploadRateBandwidth+" #US="+numUploadSlots+" window="+achievedUploadRate.toString());
						
						if (achievedRate < globalBlockUploadRateBandwidth*MINIMUM_UPLOAD_BANDWIDTH_USED && getIdleUploadSlot()==null) {
							if (logger.isDebugEnabled()) logger.debug("would ADD a slot, "+achievedRate+" < "+ (globalBlockUploadRateBandwidth*MINIMUM_UPLOAD_BANDWIDTH_USED));
							//createNewUploadSlot();
						}
						else if (numUploadSlots > MINIMUM_UPLOAD_SLOTS && achievedRate >= globalBlockUploadRateBandwidth) {
							if (logger.isDebugEnabled()) logger.debug("would REMOVE a slot, "+achievedRate +">="+ globalBlockUploadRateBandwidth);
							//removeUploadSlot(true, false);
						}
					}
				} catch (Exception e) {
					// just so it doesn't die silently if an unhandled exception happened
					e.printStackTrace();
				}
				
				//gets rid of requests in pipeline for slots that are not granted anymore
				cleanupBlockRequestPipeline();
			}
		};
		this.scheduledExecutorService.scheduleWithFixedDelay(uploadSlotManagementRunner, 2, 2, TimeUnit.SECONDS);

	}

	protected void cleanupBlockRequestPipeline() {
		
		if (logger.isDebugEnabled()) logger.debug("will cleanupBlockRequestPipeline");
		
		Collection<BlockRequestMessage> ungrantedRequests = this.pipelineBlockRequest.getAllUngranted();
		
		for (BlockRequestMessage ungrantedRequest : ungrantedRequests) {
			
			if (logger.isDebugEnabled()) logger.debug("kicking out "+ungrantedRequest);
			
			synchronized (ungrantedRequest) {
				ungrantedRequest.setReplyMessage(this.getVideoSignaling().getBlockReply(BlockReplyCode.NO_SLOT, new SegmentBlock(ungrantedRequest.getSegmentIdentifier(), ungrantedRequest.getBlockNumber()), ungrantedRequest.getSender()), ungrantedRequest.getSender());
				ungrantedRequest.setProcessed();
				ungrantedRequest.notify();
			}
		}
		
		if (logger.isDebugEnabled()) logger.debug("done cleanupBlockRequestPipeline");
	}
/*
	protected void pingRequesters() {
		for (UploadSlotRequest uploadSlotRequest : this.subscribeQueue.snapshot())
			//could use uploadSlotRequest.getSegmentIdentifier also for an improvement version of Ping that also checks interest
			this.videoSignaling.sendPing(uploadSlotRequest.getRequesterPeerId());
	}
*/

	UploadSlot getIdleUploadSlot() {
		for (UploadSlot uploadSlot : this.uploadSlots.values()) {
			Subscriber subscriber = uploadSlot.getUploadSlotRequest();
			if (subscriber==null) {
				return uploadSlot;
			}
		}
		return null;
	}
	
	/**
	 * 
	 * @param subscriber
	 * @return the timeout in seconds, 0 if slot is not granted
	 */
	public int getUploadSlotTimeoutMillis(final Subscriber subscriber) {
		
		UploadSlot uploadSlot = null;
		uploadSlot = this.grantedUploadSlots.get(subscriber);
		
		if (uploadSlot!=null) {
			if (logger.isDebugEnabled()) logger.debug("USR ["+subscriber+"] found in US "+uploadSlot);
			return uploadSlot.getTimeoutMillis();
		}
		return 0;
	}

	/**
	 * adds request to queue
	 * 
	 * @param subscriber
	 * @return the timeout in seconds of the request, 0 if failed (queue full)
	 */
	public int addSubscriberGetTimeoutMillis(final Subscriber subscriber) {

		subscriber.resetTimeoutTimer();
		
		//checks if this request is already in the queue (that would be protocol break from the other side)
		Subscriber enqueuedRequest = this.subscribersQueue.get(subscriber);
		if (enqueuedRequest!=null) {
			logger.warn("USR:"+subscriber.toString()+" was already in queue! the other peer may have a bug (requesting twice the same thing, not respecting the timeout). But it could also that a message got lost, or the peer failed and came back.");
			
			return enqueuedRequest.getTimeToTimeoutMillis();
		}		
		// tries to add it
		else if (this.subscribersQueue.offer(subscriber)) {
			
			if (logger.isDebugEnabled()) logger.debug("Request [" + subscriber + "] added to upload queue.");

			//stats
			this.updateStats();
			
			return subscriber.getTimeToTimeoutMillis();
		}
		else {
			// Peer not added to queue - it must be full
			if (logger.isDebugEnabled()) logger.debug("Rejecting request [" + subscriber + "]. Queue is full. Q snapshot: "+this.subscribersQueue.snapshot());
			
			return 0;
		}
	}
	
	/*
	public void addToBlockRequestQueue(final IncomingBlockRequest incomingBlockRequest) throws InterruptedException {
		
		if (logger.isDebugEnabled()) logger.debug("trying to add to queue: "+incomingBlockRequest);
		
		//debug, TODO remove me
		this.debugUploadSlotsSet();

		//gets upload slot
		UploadSlot grantedUploadSlot = null;

		grantedUploadSlot = this.grantedUploadSlotRequests.get(new UploadSlotRequest(incomingBlockRequest.getPeerId(), incomingBlockRequest.getSegmentIdentifier(), this.videoSignaling.getSegmentStorage(), this.videoSignaling.getIncentiveMechanism()));
		
		if (logger.isDebugEnabled()) logger.debug("got GUS="+grantedUploadSlot);
		
		if (grantedUploadSlot==null) {
			incomingBlockRequest.setReply(Reply.NO_SLOT);
		}
		else {
			if (logger.isDebugEnabled()) logger.debug("will addToBlockRequestQueue");
			grantedUploadSlot.addToBlockRequestQueue(incomingBlockRequest);
		}
	}
	*/
	
	void updateStats() {
		List<Subscriber> queueAndUploadSlots = new LinkedList<Subscriber>();
		
		queueAndUploadSlots.addAll(this.grantedUploadSlots.keySet());
		queueAndUploadSlots.addAll(this.subscribersQueue.snapshot());
		
		if (this.videoSignaling.getStats()!=null)
			this.videoSignaling.getStats().updateQueueSnapshot(queueAndUploadSlots, this.videoSignaling.getIncentiveMechanism());
	}

	public void startProcessing() {
		
		try {			
			//creates initial upload slots, which will process the contents of the queue
			for (int i = 1; i <= INTIAL_UPLOAD_SLOTS; i++) {
				this.createNewUploadSlot();
			}
			
		} catch (Exception e) {
			logger.error("Exception in "+this.getClass().toString()+ ":"+ e.getMessage());
			e.printStackTrace();
		}
	}

	private void createNewUploadSlot() {

		int number = this.lastUploadSlotNumber ++;
		numUploadSlots++;

		if (logger.isDebugEnabled()) logger.debug("creating US#"+number);

		UploadSlot uploadSlot = new UploadSlot(number, this);
		uploadSlot.start();
		
		this.uploadSlots.put(uploadSlot.getSlotNumber(), uploadSlot);
		
		//debug, TODO remove me
		this.debugUploadSlotsSet();
		
	}
	
	private void debugUploadSlotsSet() {
		if (logger.isDebugEnabled()) {
			String out="";
			for (UploadSlot uploadSlot2 : this.uploadSlots.values())
				out += uploadSlot2.toString()+":";
			logger.debug("uploadSlots now are: "+out);
		}
	}
	
	public void shutdown() {
		if (logger.isDebugEnabled()) logger.debug("in shutdown()");

		//kills scheduled executor service
		this.scheduledExecutorService.shutdownNow();
		
		List<Subscriber> subscribers = this.subscribersQueue.snapshot();
		
		Map<SegmentIdentifier,Set<PeerId>> recommendedPeers = new HashMap<SegmentIdentifier, Set<PeerId>>();

		//sends disconnect message to all peers in upload queue
		for (Subscriber  subscriber : subscribers) {
			this.videoSignaling.sendDisconnect(subscriber.getSegmentIdentifier(), subscriber.getRequesterPeerId(), false, true);
			
			Set<PeerId> recommendedPeersSet = recommendedPeers.get(subscriber.getSegmentIdentifier());
			if (recommendedPeersSet==null)
				recommendedPeersSet = new HashSet<PeerId>();
			recommendedPeersSet.add(subscriber.getRequesterPeerId());
		}

		//sends peer suggestions
		for (Entry<SegmentIdentifier, Set<PeerId>> recommendedPeersEntry : recommendedPeers.entrySet()) {
			SegmentIdentifier segmentIdentifier = recommendedPeersEntry.getKey();
			Set<PeerId> recommendedPeersSet = recommendedPeersEntry.getValue();
			recommendedPeersSet.addAll(this.neighborList.getRecommendedPeers(segmentIdentifier));
		}
		
		for (Subscriber  subscriber : subscribers) {
			
			Set<PeerId> recommendedPeersSet = recommendedPeers.get(subscriber.getSegmentIdentifier());
			
			if (recommendedPeersSet==null || recommendedPeersSet.size() < 2)  //size 1 would be only the peer itself
				continue;
			
			Set<PeerId> recommendedPeersSetWithoutPeerItself = new HashSet<PeerId>(recommendedPeersSet);
			recommendedPeersSetWithoutPeerItself.remove(subscriber.getRequesterPeerId());
				
			this.videoSignaling.sendPeerSuggestion(subscriber.getSegmentIdentifier(), recommendedPeersSetWithoutPeerItself, subscriber.getRequesterPeerId());
		}
		
		//removes requests from slots
		for (UploadSlot uploadSlot : this.uploadSlots.values()) {
			this.removeUploadSlot(uploadSlot, false, true); 
		}
		this.uploadSlots.clear();

		this.grantedUploadSlots.clear();

		this.vectorHaveCollectors.shutdown();
		
	}

	private void removeUploadSlot(final UploadSlot uploadSlot, final boolean backInQueue, final boolean sendDisconnect) {
		
		if (logger.isDebugEnabled()) logger.debug("shutting down US#"+uploadSlot.getSlotNumber());
		
		uploadSlot.shutdown(backInQueue, sendDisconnect);
		this.uploadSlots.remove(uploadSlot.getSlotNumber());
		numUploadSlots--;
		
		//debug, TODO remove me
		this.debugUploadSlotsSet();
	}
	
	private void removeUploadSlot(final boolean backInQueue, final boolean sendDisconnect) {
		
		//ranks UploadSlotRequests and removes the worst one, according to the comparator
		//preferentially an idle one: if a slot is empty, removes it
		//idea TODO choose a long inactive one. sometimes it will timeout and come back
		TreeSet<UploadSlot> slotCompared = new TreeSet<UploadSlot>();
		for (UploadSlot uploadSlot : this.uploadSlots.values()) {
			
			Subscriber subscriber = uploadSlot.getUploadSlotRequest();
			if (subscriber==null) {  //if one does not have an UploadSlotRequest, removes it (it's idle, no impact)
				this.removeUploadSlot(uploadSlot, backInQueue, sendDisconnect);
				return;
			}
			else {
				slotCompared.add(uploadSlot);
			}
		}
		
		try {
			this.removeUploadSlot(slotCompared.first(), backInQueue, sendDisconnect);
		}
		catch (NullPointerException e) {
			logger.warn("could not really remove an upload slot. better luck next time!");
			//TODO examine why it rarely goes in here. I suspect that the slot might be ungranted between the for loop up here and the deletion
			e.printStackTrace();
		}
	}
	
	Subscriber getNextUploadSlotRequest() throws InterruptedException {
		return this.subscribersQueue.take();
	}
	
    /**
     * gets the content of the queue as a pretty string
     * 
     * @return
     */
	public String getQueueSnapshotString() {
		String out = "";

		for (Subscriber subscriber : this.subscribersQueue.snapshot()) {
			out += subscriber.getRequesterPeerId().getName() 
					+ (subscriber.isInterested()?" (i)":" (!i)")
					+ " [" + subscriber.getSegmentIdentifier();
			if (this.videoSignaling.getIncentiveMechanism()!=null)
				out += " rep:" + this.videoSignaling.getIncentiveMechanism().getReputation(subscriber.getRequesterPeerId(), subscriber.getSegmentIdentifier().getStartTimeMS()) + "], ";
		}
    	
		return out;
	}

	VideoSignaling getVideoSignaling() {
		return this.videoSignaling;
	}

	/**
	 * sends have messages to all peers in que queue and to all with an upload slot
	 * 
	 * @param segmentIdentifier
	 * @param blockNumber
	 */
	public void sendHave(final SegmentIdentifier segmentIdentifier, final int blockNumber) {

		if (logger.isDebugEnabled()) logger.debug("in sendHave("+segmentIdentifier+",#"+blockNumber+")");
		
		this.vectorHaveCollectors.blockReceived(this.videoSignaling.getSegmentStorage().getSegment(segmentIdentifier), blockNumber);
		
		if (logger.isDebugEnabled()) logger.debug("done sendHave("+segmentIdentifier+",#"+blockNumber+")");
	}

	@Override
	public void sendHaveMessage(final VectorHave vectorHave) {

		if (logger.isDebugEnabled()) logger.debug("in sendHaveMessage("+vectorHave.toString()+")");
		
		//peers in subscribersQueue + grantedUploadSlots will receive
		Collection<Subscriber> subscribersToSendHave = this.subscribersQueue.snapshotWithTaken();
		subscribersToSendHave.addAll(this.grantedUploadSlots.keySet());
		
		for (Subscriber subscriber : subscribersToSendHave) {
			//if subscribed for the given segmentIdentifier + (scalar) have suppression
			if (subscriber.getSegmentIdentifier().equals(vectorHave.getSegmentIdentifier()) && (this.noHaveSuppression || !(vectorHave.getRate()==0 && this.neighborList.neighborHasBlock(subscriber.getRequesterPeerId(), vectorHave.getSegmentIdentifier(), vectorHave.getBlockNumber())))) {
				subscriber.setVectorHaveSent();
				if (logger.isDebugEnabled()) logger.debug("will send to "+subscriber);
				this.getVideoSignaling().sendHave(vectorHave.getSegmentIdentifier(), vectorHave.getBlockNumber(), vectorHave.doHave(), vectorHave.getRate(), subscriber.getRequesterPeerId());
			}
			else {
				if (logger.isDebugEnabled()) logger.debug("skipping "+subscriber+ " since "+subscriber.getSegmentIdentifier().equals(vectorHave.getSegmentIdentifier())+"&&"+this.noHaveSuppression+"||"+vectorHave.getRate()+"&&"+this.neighborList.neighborHasBlock(subscriber.getRequesterPeerId(), vectorHave.getSegmentIdentifier(), vectorHave.getBlockNumber()));
			}
		}
		
		if (logger.isDebugEnabled()) logger.debug("done sendHaveMessage("+vectorHave.toString()+") "+subscribersToSendHave);
	}

	/**
	 * sends a VectorHave only to new subscribers, that were too late to get the last VectorHave with a prediction 
	 */
	@Override
	public void sendHaveMessageToNewSubscribers(final VectorHave vectorHave) {

		if (logger.isDebugEnabled()) logger.debug("in sendHaveMessageToNewSubscribers("+vectorHave.toString()+")");
		
		//peers in subscribersQueue + grantedUploadSlots will receive
		Collection<Subscriber> subscribersToSendHave = this.subscribersQueue.snapshotWithTaken();
		subscribersToSendHave.addAll(this.grantedUploadSlots.keySet());

		//if subscribed for the given segmentIdentifier + (scalar) have suppression
		for (Subscriber subscriber : subscribersToSendHave) {
			//if subscribed for the given segmentIdentifier + (scalar) have suppression
			if (!subscriber.isVectorHaveSent() && subscriber.getSegmentIdentifier().equals(vectorHave.getSegmentIdentifier()) && (this.noHaveSuppression || !(vectorHave.getRate()==0 && this.neighborList.neighborHasBlock(subscriber.getRequesterPeerId(), vectorHave.getSegmentIdentifier(), vectorHave.getBlockNumber())))) {
				subscriber.setVectorHaveSent();
				if (logger.isDebugEnabled()) logger.debug("will send to "+subscriber);
				this.getVideoSignaling().sendHave(vectorHave.getSegmentIdentifier(), vectorHave.getBlockNumber(), vectorHave.doHave(), vectorHave.getRate(), subscriber.getRequesterPeerId());
			}
			else {
				if (logger.isDebugEnabled()) logger.debug("skipping "+subscriber+ " since "+subscriber.getSegmentIdentifier().equals(vectorHave.getSegmentIdentifier())+"&&"+this.noHaveSuppression+"||"+vectorHave.getRate()+"&&"+this.neighborList.neighborHasBlock(subscriber.getRequesterPeerId(), vectorHave.getSegmentIdentifier(), vectorHave.getBlockNumber()));
			}
 		}
		
		if (logger.isDebugEnabled()) logger.debug("done sendHaveMessageToNewSubscribers("+vectorHave.toString()+") "+subscribersToSendHave);
	}

	
	public void removePeer(final PeerId peerId, final SegmentIdentifier segmentIdentifier) {
		
		if (logger.isDebugEnabled()) logger.debug("in removePeer("+ peerId+","+ segmentIdentifier+")");

		//removes requests from Q
		for (Subscriber subscriber : this.subscribersQueue.snapshot())
			if (subscriber.getSegmentIdentifier().equals(segmentIdentifier) && subscriber.getRequesterPeerId().equals(peerId)) {
				if (logger.isDebugEnabled()) logger.debug("removing USR:"+subscriber);
				this.subscribersQueue.remove(subscriber);
			}
		
		//interrupts upload slots (they will get another request from Q)
		for (UploadSlot uploadSlot : this.uploadSlots.values()) {
			Subscriber subscriber = uploadSlot.getUploadSlotRequest();
			if (subscriber!=null && subscriber.getSegmentIdentifier().equals(segmentIdentifier) && subscriber.getRequesterPeerId().equals(peerId)) {
				if (logger.isDebugEnabled()) logger.debug("removing from US:"+uploadSlot);
				uploadSlot.ungrantSlot(false, true, false, null, subscriber);
			}
		}
	
		//stats
		this.updateStats();
		
		if (logger.isDebugEnabled()) logger.debug("done removePeer("+ peerId+","+ segmentIdentifier+")");
	}

	public void signalFailure(final P2PAddress p2pAddress) {
		
		if (logger.isDebugEnabled()) logger.debug("failure signaled for p2pAddress:"+p2pAddress);
		
		synchronized (this.failureCounter) {
	
			//signals requests from Q
			for (Subscriber subscriber : this.subscribersQueue.snapshot())
				if (subscriber.getRequesterPeerId().getDhtId().equals(p2pAddress)) {
					if (logger.isDebugEnabled()) logger.debug("signaling failure to USR:"+subscriber);
					
					MovingAverage failureCounter = this.failureCounter.get(subscriber.getRequesterPeerId());
					
					if (failureCounter == null)
						this.failureCounter.put(subscriber.getRequesterPeerId(), failureCounter = new MovingAverage(Clock.getMainClock(),10,1000));
						
					failureCounter.inputValue(1);
					
					if (logger.isDebugEnabled()) logger.debug("failureCounter of peerId "+subscriber.getRequesterPeerId()+" is "+failureCounter);
					
					if (failureCounter.getSum()>10) {
						
						if (logger.isDebugEnabled()) logger.debug("failureCounter of peerId "+subscriber.getRequesterPeerId()+" is above threshold, removing USR");
						
						this.subscribersQueue.remove(subscriber);
						this.failureCounter.remove(subscriber.getRequesterPeerId());
					}
				}
		}
		
		//signals upload slots (they will get another request from Q)
		for (UploadSlot uploadSlot : this.uploadSlots.values()) {
			Subscriber subscriber = uploadSlot.getUploadSlotRequest();
			if (subscriber!=null && subscriber.getRequesterPeerId().getDhtId().equals(p2pAddress)) {
				if (logger.isDebugEnabled()) logger.debug("signaling failure to US:"+uploadSlot.toString());
				uploadSlot.signalFailure(subscriber);
			}
		}
	
		//stats
		this.updateStats();
	}

	void setGranted(final Subscriber subscriber, final UploadSlot uploadSlot) {
		
		this.grantedUploadSlots.put(subscriber, uploadSlot);
		this.subscribersQueue.removeFromTakenElements(subscriber);
		
	}

	void setNotGranted(final Subscriber subscriber) {
		
		if (logger.isDebugEnabled()) logger.debug("setNotGranted("+subscriber+")");

		this.grantedUploadSlots.remove(subscriber);
		this.subscribersQueue.removeFromTakenElements(subscriber);

	}
	
	public Subscriber getGrantedUploadSlotRequest(final PeerId peerId, final SegmentIdentifier segmentIdentifier) {
		for (Subscriber subscriber : this.grantedUploadSlots.keySet()) {
			if (peerId.equals(subscriber.getRequesterPeerId()) && segmentIdentifier.equals(subscriber.getSegmentIdentifier()))
				return subscriber;
		}
		return null; //not granted
	}
	
	/**
	 * enforces a global combined rate for all upload slots
	 * called once before the reply of each block
	 * @param channel 
	 */
	public void enforceRate(final Channel channel, final int blockSize) throws InterruptedException {
		
		
		long t0 = Clock.getMainClock().getTimeInMillis(false);
		this.allocateBandwidth(blockSize, 0);
		if (logger.isDebugEnabled()) {
			logger.debug("+> in total took "+(Clock.getMainClock().getTimeInMillis(false)-t0)+" ms");
		}

		this.achievedUploadRate.inputValue(blockSize);
		synchronized (this.achievedUploadRatePerChannel) {
			MovingAverage channelMovingAverage = this.achievedUploadRatePerChannel.get(channel);
			if (channelMovingAverage==null) {
				channelMovingAverage = new MovingAverage(Clock.getMainClock(), 3, this.achievedUploadRate.getBoxSizeMillis());
				this.achievedUploadRatePerChannel.put(channel, channelMovingAverage);
			}
			channelMovingAverage.inputValue(blockSize);
		}
	}
	
	private int allocateBandwidth(int howMuch, int alreadyAllocated) throws InterruptedException {

		if (logger.isDebugEnabled()) logger.debug("waiting for lock: allocateBandwidth("+howMuch+","+alreadyAllocated+") "+this.bandwidthAllocation.toString()+" / "+this.globalBlockUploadRateBandwidth);

		this.ratingLock.lockInterruptibly();
		
		try {

			int allocated = Math.min((int)(this.globalBlockUploadRateBandwidth-this.bandwidthAllocation.getAverage()), howMuch);

			if (logger.isDebugEnabled()) {
				logger.debug("-allocated "+allocated);
			}
			
			if (allocated>0) {
				this.bandwidthAllocation.inputValue(allocated);
			}

			if (howMuch > allocated) {
				long timeToSleep = this.bandwidthAllocation.getTimeToSlideMillis();
				if (logger.isDebugEnabled()) logger.debug("sleeping for "+timeToSleep);
				Thread.sleep(timeToSleep);

				return this.allocateBandwidth(howMuch-allocated, alreadyAllocated+allocated);
			}
			else {
				return alreadyAllocated+allocated;
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			return 0;
		}
		finally {
			this.ratingLock.unlock();
			
			if (logger.isDebugEnabled()) logger.debug("lock unlocked: allocateBandwidth("+howMuch+","+alreadyAllocated+") "+this.bandwidthAllocation.toString()+" / "+this.globalBlockUploadRateBandwidth);
		}
	}

	public int setInterestedGetTimeout(final PeerId peerId, final SegmentIdentifier segmentIdentifier, final boolean interested) {
		
		//if not interested and already has a slot, kicks it out
		//if interested and it could take over a slot, kicks a less-ranked one out (fast react)
		Subscriber subscriber = null;
		
		if (!interested) {
			
			//signals queue
			subscriber = this.subscribersQueue.setInterestedGetSubscriber(peerId, segmentIdentifier, interested);
			
			if (subscriber==null) {
				if (logger.isDebugEnabled()) logger.debug("setInterestedGetSubscriber returned null, nothing to set in queue");
				return 0;
			}
			
			//signals slot
			for (UploadSlot uploadSlot : this.uploadSlots.values()) {
				subscriber = uploadSlot.getUploadSlotRequest();
				if (subscriber!=null && subscriber.getRequesterPeerId().equals(peerId) && subscriber.getSegmentIdentifier().equals(segmentIdentifier)) {
					if (logger.isDebugEnabled()) logger.debug("removing peer from US:"+uploadSlot.toString());
					uploadSlot.ungrantSlot(true, true, false, interested, subscriber);
					break;
				}
			}
			
		}
		else {
			subscriber = this.subscribersQueue.get(peerId, segmentIdentifier);
			
			if (subscriber==null) {
				logger.warn("setInterestedGetUploadSlotRequest returned null, nothing to set");
				return 0;
			}
			
			this.subscribersQueue.setInterested(subscriber, interested);

			UploadSlot leastRankedSlot = null;
			Subscriber leastRankedSlotRequest = null;
			
			for (UploadSlot uploadSlot : this.uploadSlots.values()) {
				Subscriber uploadSlotRequestToCheck = uploadSlot.getUploadSlotRequest();
				
				//if there is an empty slot, or request already got a slot, no preemption is necessary
				if (uploadSlotRequestToCheck==null || uploadSlotRequestToCheck.getRequesterPeerId().equals(subscriber.getRequesterPeerId()) && uploadSlotRequestToCheck.getTimeToTimeoutMillis()>0) {
					if (logger.isDebugEnabled())
						logger.debug((uploadSlotRequestToCheck==null?"there is an empty slot":"peer already got a slot")+", US="+uploadSlot);

					leastRankedSlot = null;
					break;
				}
				else if (uploadSlotRequestToCheck.getRequesterPeerId().equals(subscriber.getRequesterPeerId()) && uploadSlotRequestToCheck.getTimeToTimeoutMillis() < 0) {
					if (logger.isDebugEnabled())
						logger.debug("peer already got a timed out slot, which will be preempted, US="+uploadSlot);

					leastRankedSlot = uploadSlot;
					leastRankedSlotRequest = uploadSlotRequestToCheck;
					break;
				}
				
				//finds out slot to be preempted
				if (logger.isDebugEnabled())
					logger.debug("US "+uploadSlot+" crit1:"+uploadSlotRequestToCheck.compareTo(subscriber)+" crit2:"+(leastRankedSlotRequest==null?"is null":leastRankedSlotRequest.compareTo(uploadSlotRequestToCheck)));
				
				if (uploadSlotRequestToCheck.compareTo(subscriber) < 0
						&& (leastRankedSlotRequest==null || leastRankedSlotRequest.compareTo(uploadSlotRequestToCheck) < 0)) {
					if (logger.isDebugEnabled()) logger.debug("the least ranked now is: "+uploadSlot);
					
					leastRankedSlot = uploadSlot;
					leastRankedSlotRequest = uploadSlotRequestToCheck;
				}
			}
			

			if (leastRankedSlot!=null && !subscriber.isGranted()) {
				
				final UploadSlot finalLeastRankedSlot = leastRankedSlot;
				final Subscriber finalLeastRankedSlotRequest = leastRankedSlotRequest;
				final Subscriber finalNewSubscriber = subscriber;
				
				Runnable runner = new Runnable() {
					
					@Override
					public void run() {
						if (logger.isDebugEnabled()) logger.debug("removing least ranked slot peer from US:"+finalLeastRankedSlot.toString());

						//sends peer a hint to get streams from the one that preempted him
						videoSignaling.sendPeerSuggestion(finalLeastRankedSlotRequest.getSegmentIdentifier(), peerId, finalLeastRankedSlotRequest.getRequesterPeerId());

						//then, kicks him out
						finalLeastRankedSlot.ungrantSlot(true, true, false, interested, finalLeastRankedSlotRequest, finalNewSubscriber);
					}
				};
				
				
				long grantedThisLong = Clock.getMainClock().getTimeInMillis(false) - finalLeastRankedSlotRequest.getGrantedTimeMillis();
				long delay = Math.max(50,MINIMUM_GRANT_TIME_BEFORE_PREEMPTION-grantedThisLong);  //50 is to "guarantee" that the runnable only runs after this thread replies to peer. Using wait/notify seems to be an overkill.
				if (logger.isDebugEnabled()) logger.debug("will preempt US ("+finalLeastRankedSlot+") after "+(MINIMUM_GRANT_TIME_BEFORE_PREEMPTION-grantedThisLong)+"ms");

				ExecutorPool.getScheduledExecutorService().schedule(runner, delay, TimeUnit.MILLISECONDS);
			}
			else
				if (logger.isDebugEnabled()) logger.debug("not preempting any slot");
			
		}
		
		//stats
		this.updateStats();

		return subscriber==null?0:Math.max(0,subscriber.getTimeToTimeoutMillis());
	}

	public void registerNeighborList(final NeighborList neighborList) {
		this.neighborList=neighborList;
	}

	public float getUploadRateBandwidth() {
		return this.globalBlockUploadRateBandwidth;
	}

	public void registerPipelineBlockRequest(final BlockRequestPipeline pipelineBlockRequest) {
		pipelineBlockRequest.registerUploadSlotManager(this);
		this.pipelineBlockRequest = pipelineBlockRequest;
	}

	public BlockRequestMessage getBlockRequest(Subscriber subscriber) throws InterruptedException {
		return this.pipelineBlockRequest.pollFirst(subscriber);
	}

	public Collection<BlockRequestMessage> getAllPendingRequests(Subscriber subscriber) {
		return this.pipelineBlockRequest.getAll(subscriber);
	}

	public boolean hasGrantableUploadSlotRequestsInQueue() {
		return this.subscribersQueue.hasGrantableUploadSlotRequests();
	}

	public void incrementSentBlocks(final PeerId peerId) {
		synchronized (this.sentBlocks) {
			MovingAverage sentBlocksAverage = this.sentBlocks.get(peerId);
			if (sentBlocksAverage==null) {
				sentBlocksAverage = new MovingAverage(Clock.getMainClock(), 5, this.achievedUploadRate.getBoxSizeMillis());
				this.sentBlocks.put(peerId, sentBlocksAverage);
			}
			sentBlocksAverage.inputValue(1);
		}
	}
	
	public int getSentBlocks(final PeerId peerId) {
		synchronized (this.sentBlocks) {
			MovingAverage sentBlocksAverage = this.sentBlocks.get(peerId);
			if (sentBlocksAverage==null)
				return 0;
			else
				return sentBlocksAverage.getSum();
		}
	}

	/**
	 * to be called when (download) interest has changed
	 * to speed things up where appropriate
	 */
	public void notifyInterestChanged() {
		this.vectorHaveCollectors.shutdown();
	}

}