package net.liveshift.signaling.messaging;

import java.util.Map;

import net.liveshift.core.Channel;
import net.liveshift.core.PeerId;
import net.liveshift.storage.SegmentBlockMap;
import net.liveshift.storage.SegmentIdentifier;
import net.liveshift.util.Utils;


public class SubscribedMessage extends AbstractMessage {

	final private SegmentIdentifier segmentIdentifier;
	final private SegmentBlockMap segmentBlockMap;
	final private boolean not;
	final private int timeoutMillis;

	public SubscribedMessage(final SegmentIdentifier segmentIdentifier, final SegmentBlockMap segmentBlockMap, final int timeoutMillis, final PeerId myPeerId, final PeerId receiver, final boolean not) {
		super(myPeerId, receiver);
		
		this.not = not;
		this.segmentIdentifier = segmentIdentifier;
		
		this.segmentBlockMap = segmentBlockMap;
		this.timeoutMillis = timeoutMillis;
	}
	
	public SubscribedMessage(final byte messageId, Byte messageIdReply, final PeerId sender, final byte[] byteArray, final Map<Integer, Channel> channelCatalog, int offset) {
		super(messageId, messageIdReply, sender);
		offset+=2;
		this.segmentIdentifier = new SegmentIdentifier(channelCatalog.get(Utils.byteArrayToInteger(byteArray, offset+4)), byteArray[offset+8], Utils.byteArrayToLong(byteArray, offset+9));
		this.not = byteArray[offset+17]==1;
		this.timeoutMillis = Utils.byteArrayToInteger(byteArray, offset+18);
		
		if (byteArray.length-offset>22) {
			byte[] blockMapAsByteArray = new byte[(int)Math.ceil(SegmentBlock.getBlocksPerSegment()/8F)];
			System.arraycopy(byteArray, offset+22, blockMapAsByteArray, 0, blockMapAsByteArray.length);
			this.segmentBlockMap = new SegmentBlockMap(blockMapAsByteArray);
		}
		else
			this.segmentBlockMap = null;
	}
	
	@Override
	public byte[] toByteArray() {

		byte[] blockMap = this.segmentBlockMap!=null?this.segmentBlockMap.toByteArray():null;

		byte[] out = new byte[24+(blockMap==null?0:((int)Math.ceil(SegmentBlock.getBlocksPerSegment()/8F)))];
		int offset = super.toByteArray(out, 0);
		out[offset++] = 'V';
		offset = this.segmentIdentifier.toByteArray(out, offset);
		out[offset++] = (byte)(this.not?1:0);
		offset = Utils.integerToByteArray(this.timeoutMillis, out, offset);

		if (blockMap!=null)
			System.arraycopy(blockMap, 0, out, offset, blockMap.length);
		
		return out;
	}

	public boolean isNot() {
		return this.not;
	}

	public SegmentIdentifier getSegmentIdentifier() {
		return this.segmentIdentifier;
	}

	public long getTimeoutMillis() {
		return this.timeoutMillis;
	}

	public SegmentBlockMap getSegmentBlockMap() {
		return this.segmentBlockMap;
	}

	@Override
	public String toString() {
		String out = super.toString() + " not:"+this.not+" si:"+this.segmentIdentifier+ " to:"+this.timeoutMillis+ " bm:"+this.segmentBlockMap;
		
		return out;
	}
}
