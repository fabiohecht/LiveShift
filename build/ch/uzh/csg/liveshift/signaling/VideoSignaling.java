package net.liveshift.signaling;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.liveshift.configuration.Configuration;
import net.liveshift.core.PeerId;
import net.liveshift.core.Stats;
import net.liveshift.download.BlockRequest;
import net.liveshift.download.NeighborList;
import net.liveshift.incentive.IncentiveMechanism;
import net.liveshift.p2p.P2PAddress;
import net.liveshift.p2p.PeerFailureNotifier;
import net.liveshift.signaling.messaging.*;
import net.liveshift.storage.SegmentBlockMap;
import net.liveshift.storage.SegmentIdentifier;
import net.liveshift.storage.SegmentStorage;
import net.liveshift.time.Clock;
import net.liveshift.upload.UploadSlotManager;

/**
 *
 * @author Fabio Victora Hecht
 */
public class VideoSignaling implements PeerFailureNotifier {

	final private static Logger logger = LoggerFactory.getLogger(VideoSignaling.class);
	
	protected final MessageSender messageSender;
	protected final MessageListener messageListener;
	protected final UploadSlotManager uploadSlotManager;
	protected final PeerId myPeerId;  //the sender of the messages!
	protected final IncentiveMechanism	incentiveMechanism;
	protected final SegmentStorage segmentStorage;
	protected final MessageFactory messageFactory;
	protected final boolean freerider;

	private Stats stats;

	public VideoSignaling(final MessageSender messageSender, final MessageListener messageListener, final SegmentStorage segmentStorage, final PeerId myPeerId, final IncentiveMechanism incentiveMechanism, final Configuration configuration, final MessageFactory messageFactory) {
		
		if (logger.isDebugEnabled()) logger.debug("in constructor");
		
		this.incentiveMechanism = incentiveMechanism;
		this.messageSender = messageSender;
		this.messageListener = messageListener;
		this.segmentStorage = segmentStorage;
		this.uploadSlotManager = new UploadSlotManager(this, configuration);
		this.myPeerId = myPeerId;
		this.freerider = configuration.freeride;
		
		this.messageFactory = messageFactory;
	}

	/**
	 * @return the requestManager
	 */
	public UploadSlotManager getUploadSlotManager() {
		return uploadSlotManager;
	}
	
	public void sendSubscribe(final SegmentIdentifier segmentIdentifier, final PeerId peerId) {
		if (logger.isDebugEnabled()) logger.debug("in sendSubscribe("+segmentIdentifier.toString()+", "+peerId.toString()+")");

		SubscribeMessage request = new SubscribeMessage(segmentIdentifier, myPeerId, peerId);
		
		this.messageFactory.putPeerId(peerId);
		
		this.messageSender.sendMessage(request, peerId, false);
	}
	
	public void sendSubscribed(final SegmentIdentifier segmentIdentifier, final SegmentBlockMap segmentBlockMap, final int timeoutMillis, final PeerId peerId, final boolean not) {
		if (logger.isDebugEnabled()) logger.debug("in sendSubscribed("+segmentIdentifier+","+timeoutMillis+","+peerId+")");
		
		SubscribedMessage message = new SubscribedMessage(segmentIdentifier, segmentBlockMap, timeoutMillis, myPeerId, peerId, not);
		
		this.messageSender.sendMessage(message, peerId, false);
	}
	public void sendQueued(final SegmentIdentifier segmentIdentifier, final int timeoutMillis, final PeerId peerId, final boolean not) {
		if (logger.isDebugEnabled()) logger.debug("in sendQueued("+segmentIdentifier+","+timeoutMillis+","+peerId+","+not+")");
		
		QueuedMessage message = new QueuedMessage(segmentIdentifier, timeoutMillis, myPeerId, peerId, not);
		
		this.messageSender.sendMessage(message, peerId, false);
	}
	public void sendGranted(final SegmentIdentifier segmentIdentifier, final int timeoutMillis, final int timeoutInactiveMillis, final PeerId peerId, final boolean not) {
		if (logger.isDebugEnabled()) logger.debug("in sendGranted("+segmentIdentifier+","+timeoutMillis+","+peerId+","+not+")");
		
		GrantedMessage message = new GrantedMessage(segmentIdentifier, timeoutMillis, timeoutInactiveMillis, myPeerId, peerId, not);
		
		this.messageSender.sendMessage(message, peerId, false);
	}
	
	public SubscribedMessage getSubscribed(final SegmentIdentifier segmentIdentifier, final SegmentBlockMap segmentBlockMap, final int timeoutMillis, final PeerId receiver, final boolean not) {
		if (logger.isDebugEnabled()) logger.debug("in getSubscribed("+segmentIdentifier+","+segmentBlockMap+","+timeoutMillis+","+receiver+","+not+")");
		
		SubscribedMessage message = new SubscribedMessage(segmentIdentifier, segmentBlockMap, timeoutMillis, myPeerId, receiver, not);
		
		return message;
	}
	
	public QueuedMessage getQueued(final SegmentIdentifier segmentIdentifier, final int timeoutMillis, final PeerId receiver, final boolean not) {
		if (logger.isDebugEnabled()) logger.debug("in getQueued("+segmentIdentifier+","+timeoutMillis+","+receiver+","+not+")");
		
		QueuedMessage message = new QueuedMessage(segmentIdentifier, timeoutMillis, myPeerId, receiver, not);
		
		return message;
	}
	
	public GrantedMessage getGranted(final SegmentIdentifier segmentIdentifier, final int timeoutMillis, final int timeoutInactiveMillis, final PeerId receiver, final boolean not) {
		if (logger.isDebugEnabled()) logger.debug("in getGranted("+segmentIdentifier+","+timeoutMillis+","+receiver+","+not+")");
		
		GrantedMessage message = new GrantedMessage(segmentIdentifier, timeoutMillis, timeoutInactiveMillis, myPeerId, receiver, not);
		
		return message;
	}
	
	public void sendBlockRequest(final BlockRequest blockRequest, final PeerId receiver) {

		if (logger.isDebugEnabled()) logger.debug("in sendBlockRequest("+blockRequest.toString()+", "+receiver.toString()+")");

		BlockRequestMessage request = new BlockRequestMessage(blockRequest, myPeerId, receiver);

		//stats
		this.getStats().newOutgoingRequest(request, receiver);
			
		this.messageSender.sendMessage(request, receiver, true);
	}

	public BlockReplyMessage getBlockReply(final BlockReplyMessage.BlockReplyCode blockReplyCode, final SegmentBlock segmentBlock, final PeerId receiver) {

		if (logger.isDebugEnabled()) logger.debug("in getBlockReply("+blockReplyCode+", "+segmentBlock+","+receiver+")");
		
		BlockReplyMessage message = new BlockReplyMessage(blockReplyCode, segmentBlock, myPeerId, receiver);

		//stats
		this.getStats().updateOutgoingRequest(message);		

		return message;
	}

	public void sendHave(final SegmentIdentifier segmentIdentifier, final int blockNumber) {
		this.uploadSlotManager.sendHave(segmentIdentifier, blockNumber);
	}

	public void sendHave(final SegmentIdentifier segmentIdentifier, final int blockNumber, final boolean doHave, final int rate, final PeerId peerId) {

		if (logger.isDebugEnabled()) logger.debug("in sendHave("+segmentIdentifier.toString()+", "+peerId.toString()+" b#"+blockNumber+")");
		
		HaveMessage request = new HaveMessage(segmentIdentifier, blockNumber, doHave, rate, myPeerId, peerId);

		this.messageSender.sendMessage(request, peerId, false);
	}
	
	public void sendPing(final PeerId peerId) {

		if (logger.isDebugEnabled()) logger.debug("in sendPing("+peerId.toString()+")");
		
		PingMessage request = this.getPingRequest(peerId);

		this.messageSender.sendMessage(request, peerId, false);
	}
	public long sendPingGetLatency(PeerId peerId) {
		PingMessage request = this.getPingRequest(peerId);

		long t0 = Clock.getMainClock().getTimeInMillis(false);
		this.messageSender.sendMessage(request, peerId, true);
		return Clock.getMainClock().getTimeInMillis(false) - t0;
	}
	
/**
 * this type of ping also COULD check if the peer is still interested in getting the given segmentIdentifier
 * 
 * TODO FUTURE WORK: would need a PingRequest (with the SI) and a PingReply (with boolean and probably also the SI)
 * 
 * @param segmentIdentifier
 * @param requesterPeerId
 *
	public void sendPing(SegmentIdentifier segmentIdentifier, PeerId peerId) {
		if (logger.isDebugEnabled()) logger.debug("in sendPing("+segmentIdentifier.toString()+","+peerId.toString()+")");
		
		PingMessage request = new PingMessage();

		this.messageSender.sendMessage(request, peerId, false);
	}
*/
	/**
	 * Signal other peer to get rid of this one for uploading (always) and downloading (if interestchange is true)
	 * 
	 * @param segmentIdentifier
	 * @param peerId
	 * @param interestChange when true, the other peer also disconnects tuner; otherwise only the neighbor list
	 */
	public void sendDisconnect(final SegmentIdentifier segmentIdentifier, final PeerId peerId, final boolean stopUploading, final boolean stopDownloading) {
		
		if (logger.isDebugEnabled()) logger.debug("in sendDisconnect("+segmentIdentifier.toString()+","+peerId.toString()+",SU="+stopUploading+",SD="+stopDownloading);

		DisconnectMessage request = new DisconnectMessage(segmentIdentifier, stopUploading, stopDownloading, myPeerId, peerId);

		this.messageSender.sendMessage(request, peerId, false);
	}
	
	
	public boolean sendInterested(final SegmentIdentifier segmentIdentifier, final PeerId peerId, final boolean not) {
		if (logger.isDebugEnabled()) logger.debug("in sendInterested("+segmentIdentifier.toString()+", "+peerId.toString()+", "+not+")");
		
		InterestedMessage message = new InterestedMessage(segmentIdentifier, myPeerId, peerId, not);
		
		return this.messageSender.sendMessage(message, peerId, false);
	}

	public void sendPeerSuggestion(final SegmentIdentifier segmentIdentifier, final Set<PeerId> suggestedPeerIds, final PeerId peerId) {
		if (logger.isDebugEnabled()) logger.debug("in sendPeerSuggestion("+segmentIdentifier.toString()+", suggest:"+suggestedPeerIds+", to "+peerId.toString()+")");
		
		PeerSuggestionMessage message = new PeerSuggestionMessage(segmentIdentifier, suggestedPeerIds, this.myPeerId, peerId);
		
		this.messageSender.sendMessage(message, peerId, false);
	}
	public void sendPeerSuggestion(final SegmentIdentifier segmentIdentifier, final PeerId suggestedPeerId, final PeerId peerId) {
		Set<PeerId> suggestedPeerIds = new HashSet<PeerId>();
		suggestedPeerIds.add(suggestedPeerId);
		this.sendPeerSuggestion(segmentIdentifier, suggestedPeerIds, peerId);
	}

	public SegmentStorage getSegmentStorage() {
		return this.segmentStorage;
	}

	public void startListening(final IncomingMessageHandler incomingMessageHandler) {
		
		this.messageListener.registerIncomingMessageHandler(incomingMessageHandler);
		
		try {
			messageListener.startListening();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//also starts upload slots
		this.uploadSlotManager.startProcessing();
	}
	
	void disconnectPeer(final PeerId peerId, final SegmentIdentifier segmentIdentifier) {
		//kills uploads
		this.uploadSlotManager.removePeer(peerId, segmentIdentifier);
	}
	
	@Override
	public void signalFailure(final P2PAddress p2pAddress) {
		this.uploadSlotManager.signalFailure(p2pAddress);
	}
	
	public void disconnect() {
		this.uploadSlotManager.shutdown();
		
		if (this.messageListener!=null)
			this.messageListener.stopListening();
	}


	public Stats getStats() {
		if (this.stats==null)
			logger.error("Stats not set.");
		
		return this.stats;
	}
	public void setStats(Stats stats) {
		this.stats = stats;
	}

	public PingMessage getPingRequest(final PeerId receiver) {
		return new PingMessage(myPeerId, receiver, true);
	}
	public PingMessage getPingReply(PingMessage pingRequest) {
		return new PingMessage(myPeerId, pingRequest.getSender(), false);
	}

	public IncentiveMechanism getIncentiveMechanism() {
		return this.incentiveMechanism;
	}

	public void registerNeighborList(final NeighborList neighborList) {
		this.uploadSlotManager.registerNeighborList(neighborList);
	}
	
	public boolean isFreeRider() {
		return this.freerider;
	}

	MessageFactory getMessageFactory() {
		return this.messageFactory;
	}
}
