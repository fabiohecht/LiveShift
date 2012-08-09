package net.liveshift.dummy;

import net.liveshift.configuration.Configuration;
import net.liveshift.core.DummyStats;
import net.liveshift.core.PeerId;
import net.liveshift.core.Stats;
import net.liveshift.incentive.IncentiveMechanism;
import net.liveshift.signaling.VideoSignaling;
import net.liveshift.signaling.messaging.MessageFactory;


public class DummyVideoSignaling extends VideoSignaling {

	public DummyVideoSignaling(PeerId myPeerId, IncentiveMechanism incentiveMechanism, Configuration conf) {
		super(new DummyMessageSender(), new DummyMessageListener(), new DummySegmentStorage(myPeerId), myPeerId, incentiveMechanism, conf, new MessageFactory());
		super.setStats(new DummyStats());
	}
	
}
