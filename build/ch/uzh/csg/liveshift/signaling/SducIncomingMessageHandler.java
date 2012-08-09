package net.liveshift.signaling;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.liveshift.core.Channel;
import net.liveshift.download.Tuner;
import net.liveshift.incentive.IncentiveMechanism;
import net.liveshift.signaling.messaging.*;


public class SducIncomingMessageHandler extends DefaultIncomingMessageHandler {

	final private static Logger logger = LoggerFactory.getLogger(SducIncomingMessageHandler.class);
	
	final private IncentiveMechanism	incentiveMechanism;
	
	public SducIncomingMessageHandler(final Tuner tuner, final VideoSignaling videoSignaling, final boolean freeride, final IncentiveMechanism incentiveMechanism, final Map<Integer, Channel> channelCatalog) {
		super(tuner, videoSignaling, freeride);
		this.incentiveMechanism=incentiveMechanism;
	}

	@Override
	public AbstractMessage handleMessage(AbstractMessage incomingMessage) throws InterruptedException {

		if (logger.isDebugEnabled()) logger.debug("in handleMessage("+incomingMessage.toString()+")");
		
		return super.handleMessage(incomingMessage);
		
	}
	@Override
	public void processMessage(AbstractMessage incomingMessage) {

		//adds message to queue
		AbstractMessage replyMessage = null;
		
		//USM
		if (incomingMessage instanceof SducSubscribeMessage) {
			replyMessage = this.handleSducSubscribeMessage((SducSubscribeMessage)incomingMessage);
			
			incomingMessage.setReplyMessage(replyMessage, incomingMessage.getSender());
		}
		else
			super.processMessage(incomingMessage);
	}
	
	protected AbstractMessage handleSducSubscribeMessage(final SducSubscribeMessage incomingMessage) {
		
		//sets the upload capacity of the peer
		this.incentiveMechanism.setReputation(incomingMessage.getSender(), incomingMessage.getPeerUploadCapacity());
		
		return super.handleSubscribe(incomingMessage);		
	}
}
