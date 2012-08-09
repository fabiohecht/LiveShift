package net.liveshift.core;

import java.util.List;
import java.util.Map;

import net.liveshift.incentive.IncentiveMechanism;
import net.liveshift.signaling.messaging.BlockReplyMessage;
import net.liveshift.signaling.messaging.BlockRequestMessage;
import net.liveshift.storage.Segment;
import net.liveshift.storage.SegmentIdentifier;
import net.liveshift.upload.Subscriber;


public interface Stats {

/*
 * block maps
 */
	public void reset(byte substreams);
	public void setPlayPosition(long timeS);
	public void loadSegment(Segment segment);  //the blockmap has present (1) and absent (0) blocks
	public void blockStatChange(SegmentIdentifier segmentIdentifier, int block, int status);
 
/*
 * outgoing requests
 */
	
	public void newOutgoingRequest(BlockRequestMessage message, PeerId peerId);
	public void updateOutgoingRequest(BlockReplyMessage message);

/*
 * incoming requests
 */

	public void newIncomingRequest(BlockRequestMessage message);
	public void updateIncomingRequest(BlockReplyMessage message, PeerId peerId);

	public void updateQueueSnapshot(List<Subscriber> queueAndUploadSlots, IncentiveMechanism incentiveMechanism);

/*
 * incentive mechanisms
 */
	public void updateReputationTable(Map<PeerId, Float> map);
	public void addIncentiveMessage(String message);
}
