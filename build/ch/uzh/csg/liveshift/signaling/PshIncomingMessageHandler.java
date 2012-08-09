package net.liveshift.signaling;

import java.util.Collection;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.liveshift.core.Channel;
import net.liveshift.core.PeerId;
import net.liveshift.core.Stats;
import net.liveshift.download.NeighborList;
import net.liveshift.download.Tuner;
import net.liveshift.incentive.psh.PSH;
import net.liveshift.signaling.messaging.*;

/**
 * Handles received messages when using PSH
 * 
 * @author fabio
 *
 */
public class PshIncomingMessageHandler extends DefaultIncomingMessageHandler {
	
	final private static Logger logger = LoggerFactory.getLogger(PshIncomingMessageHandler.class);
	
	final private PSH<PeerId> pshHistory;
	protected final PeerId myPeerId;  //the sender of the messages!
	final private PshVideoSignaling videoSignaling;
	final private NeighborList neighborList;
	
	private final MessagePipeline pipelinePsh = new MessagePipeline(super.flowControl);
	private final Thread workerPsh;


	private Stats	stats;

	public PshIncomingMessageHandler(final Tuner tuner, final PshVideoSignaling videoSignaling, final PSH<PeerId> pshHistory, final PeerId myPeerId, final NeighborList neighborList, boolean freeride, final Map<Integer, Channel> channelCatalog) {
		super(tuner, videoSignaling, freeride);
		
		this.pshHistory = pshHistory;
		this.videoSignaling = videoSignaling;
		this.myPeerId = myPeerId;
		this.neighborList = neighborList;
		
		this.workerPsh = new Thread(new MessageProcessor(this.pipelinePsh));
		this.workerPsh.setName("MessageProcessor-PSH");
		this.workerPsh.start();
	}

	@Override
	public AbstractMessage handleMessage(AbstractMessage incomingMessage) throws InterruptedException {

		if (logger.isDebugEnabled()) logger.debug("in handleMessage("+incomingMessage.toString()+")");

		//adds message to the right pipeline	
		if (incomingMessage instanceof PshDownloadersRequestMessage||incomingMessage instanceof PshDownloadersReplyMessage||incomingMessage instanceof PshCheckRequestMessage||incomingMessage instanceof PshCheckReplyMessage||incomingMessage instanceof PshApplyRequestMessage||incomingMessage instanceof PshApplyReplyMessage) {
			this.pipelinePsh.offer(incomingMessage);
		}
		else
			return super.handleMessage(incomingMessage);
		

		AbstractMessage replyMessage = super.waitForProcessingGetReply(incomingMessage);
		
		return replyMessage;
		
	}
	
	@Override
	protected void processMessage(AbstractMessage incomingMessage) {

		AbstractMessage replyMessage = null;

		if (incomingMessage instanceof PshDownloadersRequestMessage) {
			replyMessage = this.handlePshDownloadersRequest((PshDownloadersRequestMessage)incomingMessage);
		}
		else if (incomingMessage instanceof PshDownloadersReplyMessage) {
			this.handlePshDownloadersReply((PshDownloadersReplyMessage)incomingMessage);
		}
		else if (incomingMessage instanceof PshCheckRequestMessage) {
			replyMessage = this.handlePshCheckRequest((PshCheckRequestMessage)incomingMessage);
		}		
		else if (incomingMessage instanceof PshCheckReplyMessage) {
			this.handlePshCheckReply((PshCheckReplyMessage)incomingMessage);
		}
		else if (incomingMessage instanceof PshApplyRequestMessage) {
			replyMessage = this.handlePshApplyRequest((PshApplyRequestMessage)incomingMessage);
		}
		else if (incomingMessage instanceof PshApplyReplyMessage) {
			this.handlePshApplyReply((PshApplyReplyMessage)incomingMessage);
		}
		else {
			super.processMessage(incomingMessage);
			return;
		}

		incomingMessage.setReplyMessage(replyMessage, incomingMessage.getSender());
	}
	
	private AbstractMessage handlePshDownloadersRequest(PshDownloadersRequestMessage message) {

		if (logger.isDebugEnabled()) logger.debug("processing message:" + message.toString());

		Map<PeerId, Float> nonInterestingNeighbors = this.pshHistory.getNonInterestingNeighbors(1, message.getPeerIds());

		return new PshDownloadersReplyMessage(nonInterestingNeighbors, myPeerId, message.getSender());
	}
	private void handlePshDownloadersReply(PshDownloadersReplyMessage message) {
		
		if (logger.isDebugEnabled()) logger.debug("processing message:" + message.toString());
		
		// now search for intermediate peers
		Collection<PeerId> intermediatePeers = this.pshHistory.getInterestingPeersToSendChecks(message.getPeerIds(), 1, message.getSender());
		
		// request check from every intermediate peer
		// (leave interesting peers out)
		for (PeerId intermediatePeer : intermediatePeers)
			if (!this.neighborList.isInteresting(intermediatePeer))
				this.videoSignaling.sendPshCheckRequest(message.getSender(), intermediatePeer);
	}
	
	private AbstractMessage handlePshCheckRequest(PshCheckRequestMessage message) {

		if (logger.isDebugEnabled()) logger.debug("processing message:" + message.toString());

		float amount = this.pshHistory.balanceAccount(message.getSender(), message.getTarget(), 1, true);
		if (amount > 0)
		{
			logger.debug("amount is OK ("+amount+"), replying with check");

			//stats
			if (this.videoSignaling.getStats()!=null)
				this.videoSignaling.getStats().addIncentiveMessage("Create check value " + amount + " on I for S: " + message.getSender().getName() + " and T: "	+ message.getTarget().getName());

			PshCheck<PeerId> check = new PshCheck<PeerId>(this.myPeerId, message.getTarget(), amount);
			
			return new PshCheckReplyMessage(check, myPeerId, message.getSender());
		}
		else {
			logger.debug("amount is too low ("+amount+"), not replying with check");
			return null;
		}
	}
	private void handlePshCheckReply(PshCheckReplyMessage message) {

		if (logger.isDebugEnabled()) logger.debug("processing message:" + message.toString());

		//stats
		if (this.videoSignaling.getStats()!=null)
			this.videoSignaling.getStats().addIncentiveMessage("Apply check on S for T (" + message.getSender().getName() + ") and I (" + message.getCheck().getIntermediate().getName() + ")");

		this.pshHistory.applyCheckSender(message.getSender(), message.getCheck());
		
		// apply check on target!!
		this.videoSignaling.sendPshApplyRequest(message.getCheck(), message.getCheck().getTarget());
	
	}


	private AbstractMessage handlePshApplyRequest(PshApplyRequestMessage message) {

		if (logger.isDebugEnabled()) logger.debug("processing message:" + message.toString());

		if (message.getCheck() == null)
			return null;
		
		//stats
		if (this.videoSignaling.getStats()!=null)
			this.videoSignaling.getStats().addIncentiveMessage("Apply check on T for S (" + message.getSender().getName() + ") and I (" + message.getCheck().getIntermediate().getName()	+ "), CompactPSH #: " + pshHistory.getPSHAccount());

		this.pshHistory.applyCheckTarget(message.getSender(), message.getCheck());
		
		return new PshApplyReplyMessage(message.getCheck().getAmount(), myPeerId, message.getSender());
	}
	
	private void handlePshApplyReply(PshApplyReplyMessage message) {

		if (logger.isDebugEnabled()) logger.debug("processing message:" + message.toString());

		//TODO what?
	}

}
