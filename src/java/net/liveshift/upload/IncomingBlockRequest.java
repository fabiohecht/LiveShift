package net.liveshift.upload;

import net.liveshift.core.PeerId;
import net.liveshift.download.BlockRequest;
import net.liveshift.signaling.messaging.SegmentBlock;
import net.liveshift.storage.SegmentIdentifier;
import net.liveshift.time.Clock;

/*
 * NOT USED
 */

public class IncomingBlockRequest implements Comparable<IncomingBlockRequest>
{
	public enum Reply {GRANTED, DONT_HAVE, REJECTED, NO_SLOT}
	
	final private PeerId	peerId;
	final private BlockRequest blockRequest;
	final private long	creationTime;
	final private int	requestMessageId;

	private volatile Reply reply;
	private SegmentBlock	segmentBlock;

	public IncomingBlockRequest(final SegmentIdentifier segmentIdentifier, final int blockNumber, final PeerId peerId, final int requestMessageId) {
		this.blockRequest = new BlockRequest(segmentIdentifier, blockNumber);
		this.peerId = peerId;
		this.requestMessageId = requestMessageId;
		this.creationTime = Clock.getMainClock().getTimeInMillis(false);
	}
	
	public PeerId getPeerId() {
		return this.peerId;
	}

	public Reply getReply() {
		if (this.reply==null)
			return Reply.REJECTED;
		else
			return this.reply;
	}
	public void setReply(Reply reply) {
		this.reply = reply;
	}
	
	public void setSegmentBlock(SegmentBlock segmentBlock) {
		this.segmentBlock = segmentBlock;
	}
	public SegmentBlock getSegmentBlock() {
		return this.segmentBlock;
	}
	
	public SegmentIdentifier getSegmentIdentifier() {
		return this.blockRequest.getSegmentIdentifier();
	}
	public int getBlockNumber() {
		return this.blockRequest.getBlockNumber();
	}
	
	@Override
	public String toString() {
		return "IBR(si:"+getSegmentIdentifier()+" b#"+ getBlockNumber()+" pid:"+peerId+" reply:"+reply+" sb:"+segmentBlock+")";
	}	

	@Override
	public int hashCode() {
		return this.getSegmentIdentifier().hashCode() ^ this.getBlockNumber();
	}
	
	@Override
	public boolean equals(Object ibr1) {
		if (ibr1 == null || !(ibr1 instanceof IncomingBlockRequest))
			return false;
		IncomingBlockRequest ibr = (IncomingBlockRequest) ibr1;
		return ibr.getSegmentIdentifier().equals(this.getSegmentIdentifier())
			&& ibr.getPeerId().equals(this.getPeerId())
			&& ibr.getBlockNumber() == this.getBlockNumber()
			&& ibr.requestMessageId == this.requestMessageId;
	}

	@Override
	public int compareTo(IncomingBlockRequest o) {
		if (this.equals(o))
			return 0;
		else {
			long timeDifference = this.creationTime - o.creationTime;
			if (timeDifference!=0)
				return timeDifference>0?1:-1;
			long midDifference = this.requestMessageId - o.requestMessageId;
			if (midDifference!=0)
				return midDifference>0?1:-1;
				
			return 0;
		}
	}

	public boolean isProcessed() {
		return this.reply!=null;
	}
}
