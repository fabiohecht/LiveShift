package net.liveshift.signaling;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.liveshift.configuration.Configuration;
import net.liveshift.core.PeerId;
import net.liveshift.incentive.IncentiveMechanism;
import net.liveshift.signaling.messaging.MessageFactory;
import net.liveshift.signaling.messaging.PshApplyRequestMessage;
import net.liveshift.signaling.messaging.PshCheck;
import net.liveshift.signaling.messaging.PshCheckRequestMessage;
import net.liveshift.signaling.messaging.PshDownloadersRequestMessage;
import net.liveshift.storage.SegmentStorage;


public class PshVideoSignaling extends VideoSignaling {

	final private static Logger logger = LoggerFactory.getLogger(PshVideoSignaling.class);
	
	public PshVideoSignaling(final MessageSender messageSender, final MessageListener messageListener, final SegmentStorage segmentStorage, final PeerId myPeerId, final IncentiveMechanism incentiveMechanism, final Configuration configuration, final MessageFactory messageFactory) {
		super(messageSender, messageListener, segmentStorage, myPeerId, incentiveMechanism, configuration, messageFactory);
		
		this.messageFactory.putPeerId(myPeerId);
	}
	
	public void sendPshDownloadersRequest(final PeerId peerId, final Set<PeerId> nonInterestingNeighbors) {
		
		if (logger.isDebugEnabled()) logger.debug("in sendPshDownloadersRequest("+peerId.toString()+")");

		PshDownloadersRequestMessage request = new PshDownloadersRequestMessage(myPeerId, peerId, nonInterestingNeighbors);

		super.messageSender.sendMessage(request, peerId, false);
	}

	public void sendPshCheckRequest(final PeerId target, final PeerId intermediate) {

		if (logger.isDebugEnabled()) logger.debug("in sendPshCheckRequest("+target+","+intermediate+")");

		PshCheckRequestMessage request = new PshCheckRequestMessage(intermediate, target, myPeerId);

		super.messageSender.sendMessage(request, intermediate, false);
	}

	public void sendPshApplyRequest(final PshCheck<PeerId> check, final PeerId peerId) {

		if (logger.isDebugEnabled()) logger.debug("in sendPshApplyRequest("+check+","+peerId+")");

		//at the moment, always applies the full amount on check
		PshApplyRequestMessage request = new PshApplyRequestMessage(check.getAmount(), check, myPeerId, peerId);

		super.messageSender.sendMessage(request, peerId, false);
	}

}
