package net.liveshift.signaling.messaging;

import java.util.Map;

import net.liveshift.core.Channel;
import net.liveshift.core.PeerId;
import net.liveshift.storage.SegmentIdentifier;
import net.liveshift.util.Utils;


public class InterestedMessage extends AbstractMessage {

	final private SegmentIdentifier segmentIdentifier;
	final private boolean not;

	/**
	 * INTERESTED: peer wants an upload slot, since it is interested in downloading something
	 * NOT_INTERESTED: peer does not want an upload slot anymore
	 */
	
	public InterestedMessage(final SegmentIdentifier segmentIdentifier, final PeerId myPeerId, final PeerId receiver, final boolean not) {
		super(myPeerId, receiver);
		
		this.segmentIdentifier = segmentIdentifier;
		this.not = not;
	}
	
	public InterestedMessage(final byte messageId, PeerId sender, final byte[] byteArray, final Map<Integer, Channel> channelCatalog, int offset) {
		super(messageId, sender);
		offset+=2;
		this.segmentIdentifier = new SegmentIdentifier(channelCatalog.get(Utils.byteArrayToInteger(byteArray, offset+4)), byteArray[offset+8], Utils.byteArrayToLong(byteArray, offset+9));
		this.not = byteArray[offset+17]==1;
	}
	
	@Override
	public byte[] toByteArray() {
		byte[] out = new byte[20];
		
		int offset = super.toByteArray(out, 0);
		out[offset++] = 'I';
		offset = this.segmentIdentifier.toByteArray(out, offset);
		out[offset++] = (byte) (this.not?1:0);

		return out;
	}

	public SegmentIdentifier getSegmentIdentifier() {
		return this.segmentIdentifier;
	}
	
	public boolean isNot() {
		return this.not;
	}
	
	@Override
	public String toString() {
		String out = super.toString() + " si:"+this.segmentIdentifier+" not:"+this.not;
		
		return out;
	}
}
