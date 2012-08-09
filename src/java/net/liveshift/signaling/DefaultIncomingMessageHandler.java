package net.liveshift.signaling;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import net.liveshift.core.PeerId;
import net.liveshift.download.Tuner;
import net.liveshift.p2p.P2PAddress;
import net.liveshift.signaling.messaging.*;
import net.liveshift.signaling.messaging.BlockReplyMessage.BlockReplyCode;
import net.liveshift.storage.Segment;
import net.liveshift.storage.SegmentBlockMap;
import net.liveshift.storage.SegmentIdentifier;
import net.liveshift.time.Clock;
import net.liveshift.upload.BlockRequestPipeline;
import net.liveshift.upload.Subscriber;
import net.liveshift.upload.UploadSlot;
import net.liveshift.util.ExecutorPool;
import net.liveshift.util.MovingAverage;
import net.liveshift.util.SuperPriorityBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DefaultIncomingMessageHandler implements IncomingMessageHandler {
	
	final private static Logger logger = LoggerFactory.getLogger(DefaultIncomingMessageHandler.class);
	
	final protected Tuner tuner;
	private final VideoSignaling videoSignaling;
	
	private final boolean freeride;
	
	private final Set<AbstractMessage> receivedMessages = new HashSet<AbstractMessage>();
	private final MovingAverage receivedMessagesAverage = new MovingAverage(Clock.getMainClock(),3, 1000);
	
	private final Map<PeerId,AtomicBoolean> peerLocks = new HashMap<PeerId, AtomicBoolean>();  //to enforce that only one UploadSlotRequest from a single peer is granted at a time
	
	protected final MessageFlowControl flowControl = new MessageFlowControl();
	
	private final Thread workerTuner;
	private final MessagePipeline pipelineTuner = new MessagePipeline(flowControl);

	private final Thread workerUploadSlotManager;
	private final MessagePipeline pipelineUploadSlotManager = new MessagePipeline(flowControl);
	
	private final BlockRequestPipeline pipelineBlockRequest = new BlockRequestPipeline(flowControl);
	//the workers for pipelineBlockRequest are the UploadSlots

	private boolean running = true;
	
	class MessageProcessor implements Runnable {
		
		final private SuperPriorityBlockingQueue<AbstractMessage> pipeline;
		
		public MessageProcessor(SuperPriorityBlockingQueue<AbstractMessage> pipeline) {
			this.pipeline = pipeline;
		}
		
		@Override
		public void run() {
			while (running) {
				
				AbstractMessage message = null;
				
				try {
					message = pipeline.pollFirst();
					//final AbstractMessage finalMessage = message;

					long t0 = Clock.getMainClock().getTimeInMillis(false);
					logger.debug("Got message: "+message);
					
					processMessage(message);
					
					logger.debug("Done with message: "+message+", took "+(Clock.getMainClock().getTimeInMillis(false)-t0)+" ms");
				}
				catch (Exception e) {
					logger.error("Exception processing message "+e.getMessage()+" caught, continuing with next message");
					e.printStackTrace();
				}
				finally {
					if (message!=null)
						synchronized (message) {
							logger.debug("setting processed: "+message);
							message.setProcessed();
							message.notify();
						}
				}
			}
		}
	}
	
	private Runnable statsReporter = new Runnable() {
			
			@Override
			public void run() {
				try {
					//receivedMessagesAverage
					logger.info("receivedMessagesAverage: "+receivedMessagesAverage.getAverage());
					
					//print which are unprocessed
					long currentTime = Clock.getMainClock().getTimeInMillis(false);

					String debug = "";
					synchronized (receivedMessages) {
						for (AbstractMessage message : receivedMessages)
							debug += message.toString()+"@"+(message.getReceiveTimeMillis()-currentTime)+":";
					}
					if (logger.isDebugEnabled()) logger.debug("unprocessed messages: "+debug);
				} catch (Exception e) {
					// just so it doesn't die silently if an unhandled exception happened
					e.printStackTrace();
				}
			}
		};
	
	private ScheduledFuture<?>	statsReporterScheduledFuture;
	
	public DefaultIncomingMessageHandler(final Tuner tuner, final VideoSignaling videoSignaling, final boolean freeride) {
		this.tuner = tuner;
		this.videoSignaling = videoSignaling;
		this.freeride = freeride;
		
		this.statsReporterScheduledFuture = ExecutorPool.getScheduledExecutorService().scheduleAtFixedRate(statsReporter, 1000, 1000, TimeUnit.MILLISECONDS);
		
		this.workerTuner = new Thread(new MessageProcessor(this.pipelineTuner));
		this.workerTuner.setName("MessageProcessor-Tuner");
		this.workerUploadSlotManager = new Thread(new MessageProcessor(this.pipelineUploadSlotManager));
		this.workerUploadSlotManager.setName("MessageProcessor-USM");
		
		this.workerTuner.start();
		this.workerUploadSlotManager.start();
		
		this.videoSignaling.getUploadSlotManager().registerPipelineBlockRequest(this.pipelineBlockRequest);
	}
	
	@Override
	public void shutdown() {
		this.running = false;
		this.statsReporterScheduledFuture.cancel(false);
	}
	
	/*
	@Override
	public AbstractMessage handleIncomingMessage(final P2PAddress senderP2pAddress, final byte[] incomingByteArray, final int offset) {

		AbstractMessage incomingMessage = this.getMessage(senderP2pAddress, incomingByteArray, offset);
		
		return handleIncomingMessage(incomingMessage);
	}


	@Override
	public AbstractMessage handleIncomingMessage(PeerId peerIdSender, byte[] incomingByteArray, int offset) {

		AbstractMessage incomingMessage = this.getMessage(peerIdSender, incomingByteArray, offset);
		
		return handleIncomingMessage(incomingMessage);
	}
	*/
	
	@Override
	public AbstractMessage handleIncomingMessage(AbstractMessage incomingMessage) {
		
		this.receivedMessagesAverage.inputValue(1);
		
		logger.info("in handleIncomingMessage("+incomingMessage.toString()+")");
		
		//adds them to a list to see later which got stuck here
		incomingMessage.setReceiveTimeMillis(Clock.getMainClock().getTimeInMillis(false));
		synchronized (receivedMessages) {
			this.receivedMessages.add(incomingMessage);
		}
		
		//handles the message
		AbstractMessage replyMessage = null;
		try {
			replyMessage = this.handleMessage(incomingMessage);
		} catch (InterruptedException e) {
			logger.debug("interrupted");
			e.printStackTrace();
		}
		
		synchronized (receivedMessages) {
			this.receivedMessages.remove(incomingMessage);
		}
		
		return replyMessage;
	}
	
	@Override
	public AbstractMessage getMessage(final PeerId peerIdSender, final byte[] incomingByteArray, final int offset) {

		try {
			return this.videoSignaling.getMessageFactory().getMessage(peerIdSender, incomingByteArray, offset);
		} catch (UnknownHostException e) {
			String byteArrayAsString="[";
			for (byte b : incomingByteArray)
				byteArrayAsString+=b+",";
			byteArrayAsString+="]";
			logger.error("Error processing incoming message, dropping: "+e.getMessage()+", message is: " + byteArrayAsString + " with offset " + offset);

			e.printStackTrace();
			return null;
		}

	}
	
	@Override
	public AbstractMessage getMessage(final P2PAddress senderP2pAddress, final byte[] incomingByteArray, final int offset) {
		
		AbstractMessage incomingMessage = null;
		int countDown = 5;
		while (incomingMessage==null && countDown-->0) {
			try {
				incomingMessage = this.videoSignaling.getMessageFactory().getMessage(senderP2pAddress, incomingByteArray, offset);
			} catch (UnknownHostException e1) {
				logger.error("Message received from unknown host -- dropping");
				e1.printStackTrace();
				return null;
			} catch (SenderNotFoundException e) {
				if (logger.isDebugEnabled()) logger.debug("sender of message not found. it could be that the message containing the sender is a bit delayed. will wait and retry.");

				try {
					Thread.sleep(50);
				} catch (InterruptedException e1) {
					if (logger.isDebugEnabled()) logger.debug("interrupted");
					break;
				}
			} catch (Exception e) {
				String byteArrayAsString="[";
				for (byte b : incomingByteArray)
					byteArrayAsString+=b+",";
				byteArrayAsString+="]";
				logger.error("Error processing incoming message, dropping: "+e.getMessage()+", message is: " + byteArrayAsString + " with offset " + offset);

				e.printStackTrace();
				return null;
			}
		}
		
		return incomingMessage;
		
	}
	
	protected AbstractMessage handleMessage(AbstractMessage incomingMessage) throws InterruptedException {
		
		//adds message to the right pipeline

		//BR
		if (incomingMessage instanceof BlockRequestMessage) {
			
			BlockRequestMessage blockRequestMessage = (BlockRequestMessage) incomingMessage;
			
			//stats
			this.videoSignaling.getStats().newIncomingRequest(blockRequestMessage);
			
			this.pipelineBlockRequest.offer(incomingMessage);
		}
		
		//USM
		else if (incomingMessage instanceof SubscribeMessage || incomingMessage instanceof InterestedMessage || incomingMessage instanceof DisconnectMessage && ((DisconnectMessage)incomingMessage).stopUploading())
			this.pipelineUploadSlotManager.offer(incomingMessage);
		
		//TUNER
		else if (incomingMessage instanceof PingMessage || incomingMessage instanceof HaveMessage || incomingMessage instanceof SubscribedMessage || incomingMessage instanceof QueuedMessage || incomingMessage instanceof GrantedMessage || incomingMessage instanceof BlockReplyMessage || incomingMessage instanceof PeerSuggestionMessage || incomingMessage instanceof DisconnectMessage && ((DisconnectMessage)incomingMessage).stopDownloading())
			this.pipelineTuner.offer(incomingMessage);
		
		else {
			logger.error("Message of type ("+incomingMessage.getClass().getCanonicalName()+", "+incomingMessage.toString()+") is not recognized");
			
			//flow control needs to take its MID not to have a gap			
			this.pipelineUploadSlotManager.offer(incomingMessage);
			return null;
		}
		
		AbstractMessage replyMessage = null;
		try {
			replyMessage = this.waitForProcessingGetReply(incomingMessage);
		}
		catch (Exception e) {
			e.printStackTrace();
			logger.error("Error waiting for message to be processed: "+e.getMessage());
		}
		
		return replyMessage;
	}
	
	protected AbstractMessage waitForProcessingGetReply(AbstractMessage incomingMessage) throws InterruptedException {
		//waits for message to be processed
		synchronized (incomingMessage) {
			while (!incomingMessage.isProcessed()) {
				logger.debug("waiting on: "+incomingMessage);
				incomingMessage.wait();
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("done waiting on: "+incomingMessage);
		}
		AbstractMessage replyMessage = incomingMessage.getReplyMessage();

		if (logger.isDebugEnabled()) {
			if (replyMessage==null) {
				logger.debug("will reply NULL to "+incomingMessage);
			}
			else {
				logger.debug("will reply: "+replyMessage+" to "+incomingMessage);
			}
		}
		return replyMessage;
	}

	protected void processMessage(AbstractMessage incomingMessage) {
		
		if (incomingMessage.isProcessed() && !(incomingMessage instanceof HaveMessage || incomingMessage instanceof BlockReplyMessage))
			return;
		
		AbstractMessage replyMessage = null;
		
		//USM
		if (incomingMessage instanceof SubscribeMessage) {
			replyMessage = this.handleSubscribe((SubscribeMessage)incomingMessage);
		}
		else if (incomingMessage instanceof InterestedMessage) {
			replyMessage = this.handleInterested((InterestedMessage)incomingMessage);
		}
		else if (incomingMessage instanceof DisconnectMessage) {
			DisconnectMessage disconnectMessage = (DisconnectMessage)incomingMessage;
			if (disconnectMessage.stopUploading())
				this.handleDisconnect(disconnectMessage);
		//TUNER

			if (disconnectMessage.stopDownloading())
				this.handleDisconnect(disconnectMessage);
		}
		else if (incomingMessage instanceof HaveMessage) {
			this.handleHave((HaveMessage)incomingMessage);
		}
		else if (incomingMessage instanceof SubscribedMessage) {
			this.handleSubscribed((SubscribedMessage)incomingMessage);
		}
		else if (incomingMessage instanceof QueuedMessage) {
			this.handleQueued((QueuedMessage)incomingMessage);
		}
		else if (incomingMessage instanceof GrantedMessage) {
			this.handleGranted((GrantedMessage)incomingMessage);
		}
		else if (incomingMessage instanceof BlockReplyMessage) {
			this.handleBlockReply((BlockReplyMessage)incomingMessage);
		}
		else if (incomingMessage instanceof PeerSuggestionMessage) {
			this.handlePeerSuggestion((PeerSuggestionMessage)incomingMessage);
		}
		else {
			logger.warn("Unknown message received, not processing ("+incomingMessage+")");
		}

		incomingMessage.setReplyMessage(replyMessage, incomingMessage.getSender());

	}
	
	protected AbstractMessage handlePing(PingMessage incomingMessage) {

		if (logger.isDebugEnabled()) logger.debug("processing message:" + incomingMessage.toString());
		if (incomingMessage.isRequest())
			return this.videoSignaling.getPingReply(incomingMessage);
		else
			return null;
	}


	protected AbstractMessage handleSubscribe(final SubscribeMessage incomingMessage) {
		
		if (logger.isDebugEnabled()) logger.debug("processing message:" + incomingMessage.toString());

		SegmentIdentifier segmentIdentifier = incomingMessage.getSegmentIdentifier();
		Segment segment = this.tuner.getSegmentStorage().getSegment(segmentIdentifier);

		if (segment==null) {
			logger.warn("USRM:"+incomingMessage.toString()+" asks for unknown segment.");
			return this.videoSignaling.getSubscribed(segmentIdentifier, null, 0, incomingMessage.getSender(), false);		
		}
		
		AtomicBoolean peerLock;
		synchronized (this.peerLocks) {
			peerLock = this.peerLocks.get(incomingMessage.getSender());
			if (peerLock==null) {
				this.peerLocks.put(incomingMessage.getSender(), peerLock = new AtomicBoolean(false));
			}
		}
		Subscriber subscriber = new Subscriber(incomingMessage.getSender(), segmentIdentifier, segment, this.videoSignaling.getUploadSlotManager(), peerLock);
		
		//lets the tuner know: it may be a new interesting neighbor
		this.tuner.getNeighborList().addPotentialCandidate(segmentIdentifier, incomingMessage.getSender());
		
		//checks if already granted
		int timeoutMillis = this.videoSignaling.getUploadSlotManager().getUploadSlotTimeoutMillis(subscriber);
		
		boolean isAlreadyGranted = timeoutMillis > 0;
		
		if (isAlreadyGranted)
			logger.warn("USR:"+subscriber.toString()+" has already an upload slot! the other peer could have a bug (requesting again when it's not supposed to). But it could also that a message got lost, or the peer failed and came back.");
		
		//if not, tries to add to queue and gets timeout
		if (!isAlreadyGranted)
			timeoutMillis = this.videoSignaling.getUploadSlotManager().addSubscriberGetTimeoutMillis(subscriber);
	
		if (logger.isDebugEnabled()) logger.debug("addUploadSlotRequest ["+subscriber+"] returned ["+timeoutMillis+","+isAlreadyGranted+"]");

		if (isAlreadyGranted)
			return this.videoSignaling.getGranted(segmentIdentifier, timeoutMillis, UploadSlot.INACTIVITY_TIMEOUT_S*1000, incomingMessage.getSender(), false);
		else if (timeoutMillis > 0) {
			//replies with the timeout, so the other peer knows when to send another request
			SegmentBlockMap segmentBlockMap = this.tuner.getSegmentBlockMap(segmentIdentifier);
			
			return this.videoSignaling.getSubscribed(segmentIdentifier, segmentBlockMap, timeoutMillis, incomingMessage.getSender(), false);
		}
		else {
			//queue is full or something like that
			return this.videoSignaling.getSubscribed(segmentIdentifier, null, 0, incomingMessage.getSender(), true);			
		}
	}

	protected void handleSubscribed(final SubscribedMessage subscribedMessage) {

		if (logger.isDebugEnabled()) logger.debug("processing message:" + subscribedMessage.toString());

		
		if (subscribedMessage.isNot()) {
			this.tuner.getNeighborList().removeNeighbor(subscribedMessage.getSender(), subscribedMessage.getSegmentIdentifier(), 5000);
			//this.tuner.getNeighborList().lookForCandidates(usReplyMessage.getSegmentIdentifier());
		}
		else {
			//registers timeout for not trying again too soon (if peer has slot, does not change anything, only if it were BACK_IN_Q)
			this.tuner.getNeighborList().addNeighbor(subscribedMessage.getSender(), subscribedMessage.getSegmentIdentifier(), subscribedMessage.getSegmentBlockMap(), subscribedMessage.getTimeoutMillis());
		}
	}

	protected void handleGranted(final GrantedMessage grantedMessage) {

		if (logger.isDebugEnabled()) logger.debug("processing message:" + grantedMessage.toString());

		if (grantedMessage.isNot()) {
			this.tuner.getNeighborList().removeUsGranter(grantedMessage.getSender(), grantedMessage.getSegmentIdentifier(), grantedMessage.getTimeoutMillis());
		}
		else
			this.tuner.getNeighborList().addUsGranter(grantedMessage.getSender(), grantedMessage.getSegmentIdentifier(), grantedMessage.getTimeoutMillis());
	}

	protected void handleQueued(final QueuedMessage queuedMessage) {

		if (logger.isDebugEnabled()) logger.debug("processing message:" + queuedMessage.toString());
		
		if (queuedMessage.isNot()) {
			this.tuner.getNeighborList().removeFromInterested(queuedMessage.getSender(), queuedMessage.getSegmentIdentifier(), queuedMessage.getTimeoutMillis());
		}
		else {
			this.tuner.getNeighborList().addNeighbor(queuedMessage.getSender(), queuedMessage.getSegmentIdentifier(), null, queuedMessage.getTimeoutMillis());
		}

	}
	
	protected AbstractMessage handleInterested(final InterestedMessage incomingMessage) {

		if (logger.isDebugEnabled()) logger.debug("processing message:" + incomingMessage.toString());

		Subscriber subscriber;
		if (null!=(subscriber=this.videoSignaling.getUploadSlotManager().getGrantedUploadSlotRequest(incomingMessage.getSender(), incomingMessage.getSegmentIdentifier()))) {
			logger.warn(subscriber+" is already granted!!");
			return this.videoSignaling.getGranted(incomingMessage.getSegmentIdentifier(), subscriber.getTimeToTimeoutMillis(), UploadSlot.INACTIVITY_TIMEOUT_S*1000, incomingMessage.getSender(), false);
		}

		int timeoutMillis = this.videoSignaling.getUploadSlotManager().setInterestedGetTimeout(incomingMessage.getSender(), incomingMessage.getSegmentIdentifier(), !incomingMessage.isNot());
		
		if (incomingMessage.isNot()) {
			//if not interested, should return subscribed
			return this.videoSignaling.getSubscribed(incomingMessage.getSegmentIdentifier(), null, timeoutMillis, incomingMessage.getSender(), false);
		}
		else {
			//must return Queued with new (extended) timeout.. and synchronize with the possible Granted	
			return this.videoSignaling.getQueued(incomingMessage.getSegmentIdentifier(), timeoutMillis, incomingMessage.getSender(), false);
		}
		
	}

	protected void handleBlockReply(BlockReplyMessage blockReplyMessage) {

		if (logger.isDebugEnabled()) logger.debug("processing message:" + blockReplyMessage.toString());

		//stats
		this.videoSignaling.getStats().updateOutgoingRequest(blockReplyMessage);			
		
		final SegmentBlock segmentBlock = blockReplyMessage.getSegmentBlock();
		
		PeerId sender = blockReplyMessage.getSender();
		
		// Make sure we never save anything empty
		if (blockReplyMessage.getReplyCode()==BlockReplyCode.GRANTED && segmentBlock != null) {
			
			//increases reputation, resets timeout
			this.tuner.getNeighborList().registerSuccess(sender, segmentBlock);
			
			//increments application-level hop count
			if (segmentBlock!=null) {
				segmentBlock.setDownloadTime();
				segmentBlock.incrementHopCount();
			}
			
			//writes segment (important!)
			final boolean successPut = this.tuner.putSegmentBlock(sender, segmentBlock);

			if (successPut) {
				Runnable runner = new Runnable() {
					@Override
					public void run() {
	
						try {
							//signals player that it has arrived (maybe it's waiting for it)
							if (tuner.getVideoPlayer()!=null)
								tuner.getVideoPlayer().tryAndPlay();
							
							//sends HAVE messages
							videoSignaling.sendHave(segmentBlock.getSegmentIdentifier(), segmentBlock.getBlockNumber());

						} catch (Exception e) {
							// just so it doesn't die silently if an unhandled exception happened
							logger.error("error calling player or sending Haves: "+e.getMessage());
							e.printStackTrace();
						}
					}
				};
				ExecutorPool.getGeneralExecutorService().execute(runner);
			}
		}
		else if (blockReplyMessage.getReplyCode()==BlockReplyCode.REJECTED || blockReplyMessage.getReplyCode()==BlockReplyCode.DONT_HAVE || blockReplyMessage.getReplyCode()==BlockReplyCode.NO_SLOT) {

			//no slot granted, so it probably missed the message ungranting the slot (or message could be on its way when it lost slot)
			if (blockReplyMessage.getReplyCode()==BlockReplyCode.NO_SLOT)
				this.tuner.getNeighborList().removeUsGranter(blockReplyMessage.getSender(), blockReplyMessage.getSegmentBlock().getSegmentIdentifier(), 10000);
			
			//DONT_HAVE is the same as HAVE (!doHave)
			//except that a peer keeps track if many requests are failing in order to send unsubscribe (hopefully eventually solving the sync issue)
			// ===> DISABLED BECAUSE IT COULD MAKE A PEER STALL FOR A LONG TIME
			//else if (blockReplyMessage.getReplyCode()==BlockReplyCode.DONT_HAVE)
			//	this.tuner.updateNeighborBlockMap(blockReplyMessage.getSender(), blockReplyMessage.getSegmentBlock().getSegmentIdentifier(), blockReplyMessage.getSegmentBlock().getBlockNumber(), false, -1);
			
			//anyway, a block request has failed, so let's react quickly and reschedule the block
			if (segmentBlock!=null && segmentBlock.getBlockNumber() != -1)
				this.tuner.getNeighborList().registerFailure(segmentBlock.getSegmentIdentifier(), segmentBlock.getBlockNumber(), blockReplyMessage.getSender());
		}
		else {
			logger.warn("unexpected format of message: "+blockReplyMessage);
		}
		//		TODO: see what to do when getReplyCode() is different or segmentBlock==null
	}

	protected void handleHave(final HaveMessage incomingMessage) {

		if (logger.isDebugEnabled()) logger.debug("processing message:" + incomingMessage.toString());

		//updates block map of peer
		this.tuner.updateNeighborBlockMap(incomingMessage.getSender(), incomingMessage.getSegmentIdentifier(), incomingMessage.getBlockNumber(), incomingMessage.doHave() , incomingMessage.getRate());
		
	}

	protected void handleDisconnect(final DisconnectMessage incomingMessage) {

		if (logger.isDebugEnabled()) logger.debug("processing message:" + incomingMessage.toString());

		if (incomingMessage.stopUploading()) {
			//this peer won't upload anymore to the sender of the message (peer may be added again)
			this.videoSignaling.disconnectPeer(incomingMessage.getSender(), incomingMessage.getSegmentIdentifier());
		}
		
		if (incomingMessage.stopDownloading()) {
			//this peer won't download anymore from the sender of the message (peer may be added again)
			this.tuner.disconnectPeer(incomingMessage.getSender(), incomingMessage.getSegmentIdentifier());
			//this.tuner.getNeighborList().lookForCandidates(incomingMessage.getSegmentIdentifier());
		}
	}

	private void handlePeerSuggestion(PeerSuggestionMessage incomingMessage) {

		if (logger.isDebugEnabled()) logger.debug("processing message:" + incomingMessage.toString());

		for (PeerId peerId : incomingMessage.getSuggestedPeerIds()) {
			this.tuner.getNeighborList().addPotentialCandidate(incomingMessage.getSegmentIdentifier(), peerId);
		}
	}

}
