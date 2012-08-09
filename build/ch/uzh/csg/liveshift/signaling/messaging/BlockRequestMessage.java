package net.liveshift.signaling.messaging;

import java.util.Map;

import net.liveshift.core.Channel;
import net.liveshift.core.PeerId;
import net.liveshift.download.BlockRequest;
import net.liveshift.storage.SegmentIdentifier;
import net.liveshift.util.Utils;


public class BlockRequestMessage extends AbstractMessage {

	final private SegmentIdentifier segmentIdentifier;
	final private int blockNumber;
	transient private boolean granted;

	public BlockRequestMessage(SegmentIdentifier segmentIdentifier, int blockNumber, final PeerId myPeerId, final PeerId receiver) {
		super(myPeerId, receiver);
		
		this.segmentIdentifier = segmentIdentifier;
		this.blockNumber = blockNumber;
	}
	
	public BlockRequestMessage(BlockRequest blockRequest, final PeerId myPeerId, final PeerId receiver) {
		this(blockRequest.getSegmentIdentifier(), blockRequest.getBlockNumber(), myPeerId, receiver);
	}

	public BlockRequestMessage(byte messageId, PeerId sender, byte[] byteArray,	Map<Integer, Channel> channelCatalog, int offset) {
		super(messageId, sender);
		offset+=2;
		this.segmentIdentifier = new SegmentIdentifier(channelCatalog.get(Utils.byteArrayToInteger(byteArray, offset+4)), byteArray[offset+8], Utils.byteArrayToLong(byteArray, offset+9));
		this.blockNumber = Utils.byteArrayToInteger(byteArray, offset+17);
	}
	
	@Override
	public byte[] toByteArray() {
		byte[] out = new byte[23];
		
		int offset = super.toByteArray(out, 0);
		out[offset++] = 'B';
		offset = this.segmentIdentifier.toByteArray(out, offset);
		offset = Utils.integerToByteArray(this.blockNumber, out, offset);
			
		return out;
	}

	public SegmentIdentifier getSegmentIdentifier() {
		return this.segmentIdentifier;
	}
	public int getBlockNumber() {
		return this.blockNumber;
	}
	
	public BlockRequest getBlockRequest() {
		return new BlockRequest(this.segmentIdentifier, this.blockNumber);
	}
	
	@Override
	public String toString() {
		String out = super.toString() + " si:"+this.segmentIdentifier+ " b#"+this.blockNumber;
		
		return out;
	}

	public void setGranted(boolean granted) {
		this.granted = granted;
	}

	public boolean isGranted() {
		return granted;
	}
}
