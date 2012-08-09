package net.liveshift.core;

import java.util.List;
import java.util.Map;

import net.liveshift.incentive.IncentiveMechanism;
import net.liveshift.signaling.messaging.BlockReplyMessage;
import net.liveshift.signaling.messaging.BlockRequestMessage;
import net.liveshift.storage.Segment;
import net.liveshift.storage.SegmentIdentifier;
import net.liveshift.upload.Subscriber;



public class DummyStats implements Stats {

	@Override
	public void addIncentiveMessage(String message) {
		// TODO Auto-generated method stub

	}

	@Override
	public void blockStatChange(SegmentIdentifier segmentIdentifier, int block, int status) {
		// TODO Auto-generated method stub

	}

	@Override
	public void loadSegment(Segment segment) {
		// TODO Auto-generated method stub

	}

	@Override
	public void newIncomingRequest(BlockRequestMessage message) {
		// TODO Auto-generated method stub
		System.out.println("newIncomingRequest("+ message+")");

	}

	@Override
	public void newOutgoingRequest(BlockRequestMessage message, PeerId peerId) {
		// TODO Auto-generated method stub
		System.out.println("newOutgoingRequest("+ message+","+peerId+")");

	}

	@Override
	public void reset(byte substreams) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setPlayPosition(long timeS) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateOutgoingRequest(BlockReplyMessage message) {
		// TODO Auto-generated method stub
		System.out.println("updateIncomingRequest("+ message+")");

	}

	@Override
	public void updateIncomingRequest(BlockReplyMessage message, PeerId peerId) {
		// TODO Auto-generated method stub
		System.out.println("updateIncomingRequest("+ message+","+ peerId+")");
	}

	@Override
	public void updateQueueSnapshot(List<Subscriber> queueAndUploadSlots, IncentiveMechanism incentiveMechanism) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateReputationTable(Map<PeerId, Float> map) {
		// TODO Auto-generated method stub
		
	}
	
}
