package net.liveshift.core;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.liveshift.incentive.IncentiveMechanism;
import net.liveshift.signaling.messaging.BlockReplyMessage;
import net.liveshift.signaling.messaging.BlockRequestMessage;
import net.liveshift.storage.Segment;
import net.liveshift.storage.SegmentIdentifier;
import net.liveshift.upload.Subscriber;

public class StatsSet {
	Set<Stats> stats = new HashSet<Stats>();
	
	public void addStatListener(Stats listener) {
		this.stats.add(listener);
	}

	public void removeStatListener(Stats listener) {
		this.stats.remove(listener);
	}

	public void updateReputationTable(Map<PeerId, Float> reputationTable) {
		for (Stats stat : this.stats) {
			stat.updateReputationTable(reputationTable);
		}
	}

	public void newOutgoingRequest(BlockRequestMessage request, PeerId receiver) {
		for (Stats stat : this.stats) {
			stat.newOutgoingRequest(request, receiver);
		}
	}

	public void newIncomingRequest(BlockRequestMessage blockRequestMessage) {
		for (Stats stat : this.stats) {
			stat.newIncomingRequest(blockRequestMessage);
		}
	}

	public void updateOutgoingRequest(BlockReplyMessage blockReplyMessage) {
		for (Stats stat : this.stats) {
			stat.updateOutgoingRequest(blockReplyMessage);
		}
	}

	public void addIncentiveMessage(String string) {
		for (Stats stat : this.stats) {
			stat.addIncentiveMessage(string);
		}		
	}

	public void reset(byte numSubstreams) {
		for (Stats stat : this.stats) {
			stat.reset(numSubstreams);
		}
	}

	public void setPlayPosition(long playTimeMs) {
		for (Stats stat : this.stats) {
			stat.setPlayPosition(playTimeMs);
		}
	}

	public void blockStatChange(SegmentIdentifier segmentIdentifier, int blockNumber, int status) {
		for (Stats stat : this.stats) {
			stat.blockStatChange(segmentIdentifier, blockNumber, status);
		}
	}

	public void loadSegment(Segment localSegment) {
		for (Stats stat : this.stats) {
			stat.loadSegment(localSegment);
		}
	}

	public void updateQueueSnapshot(List<Subscriber> queueAndUploadSlots, IncentiveMechanism incentiveMechanism) {
		for (Stats stat : this.stats) {
			stat.updateQueueSnapshot(queueAndUploadSlots, incentiveMechanism);
		}
	}

	public void updateIncomingRequest(BlockReplyMessage replyMessage, PeerId sender) {
		for (Stats stat : this.stats) {
			stat.updateIncomingRequest(replyMessage, sender);
		}
	}
}
