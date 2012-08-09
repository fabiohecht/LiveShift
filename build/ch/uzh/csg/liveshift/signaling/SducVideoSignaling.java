package net.liveshift.signaling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.liveshift.configuration.Configuration;
import net.liveshift.core.PeerId;
import net.liveshift.incentive.IncentiveMechanism;
import net.liveshift.signaling.messaging.MessageFactory;
import net.liveshift.signaling.messaging.SducSubscribeMessage;
import net.liveshift.storage.SegmentIdentifier;
import net.liveshift.storage.SegmentStorage;


public class SducVideoSignaling extends VideoSignaling {
	
	final private static Logger logger = LoggerFactory.getLogger(VideoSignaling.class);
	
	public SducVideoSignaling(final MessageSender messageSender, final MessageListener messageListener, final SegmentStorage segmentStorage, final PeerId myPeerId, final IncentiveMechanism incentiveMechanism, final Configuration configuration, final MessageFactory messageFactory) {
		super(messageSender, messageListener, segmentStorage, myPeerId, incentiveMechanism, configuration, messageFactory);
	}
	
	@Override
	public void sendSubscribe(final SegmentIdentifier segmentIdentifier, final PeerId peerId) {
		if (logger.isDebugEnabled()) logger.debug("in sendSubscribe("+segmentIdentifier.toString()+", "+peerId.toString()+")");

		SducSubscribeMessage request = new SducSubscribeMessage(segmentIdentifier, myPeerId, peerId, this.uploadSlotManager.getUploadRateBandwidth());

		this.messageFactory.putPeerId(peerId);
		
		this.messageSender.sendMessage(request, peerId, true);
	}
}
