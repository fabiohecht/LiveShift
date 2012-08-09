package net.liveshift.signaling.messaging;

import java.util.Map;

import net.liveshift.core.Channel;
import net.liveshift.core.PeerId;
import net.liveshift.storage.SegmentIdentifier;
import net.liveshift.util.Utils;


public class GrantedMessage extends AbstractMessage {

	final private SegmentIdentifier segmentIdentifier;
	final private int timeoutMillis;
	final private int timeoutInactiveMillis;
	final private boolean not;

	public GrantedMessage(final SegmentIdentifier segmentIdentifier, final int timeoutMillis, final int timeoutInactiveMillis, final PeerId myPeerId, final PeerId receiver, final boolean not) {
		super(myPeerId, receiver);
		
		this.not = not;
		this.segmentIdentifier = segmentIdentifier;
		this.timeoutMillis = timeoutMillis;
		this.timeoutInactiveMillis = timeoutInactiveMillis;
	}
	
	public GrantedMessage(byte messageId, Byte messageIdReply, PeerId sender, byte[] byteArray, Map<Integer, Channel> channelCatalog, int offset) {
		super(messageId, messageIdReply, sender);
		offset+=2;
		this.segmentIdentifier = new SegmentIdentifier(channelCatalog.get(Utils.byteArrayToInteger(byteArray, offset+4)), byteArray[offset+8], Utils.byteArrayToLong(byteArray, offset+9));
		this.not = byteArray[offset + 17]==1;
		this.timeoutMillis = Utils.byteArrayToInteger(byteArray, offset+18);
		this.timeoutInactiveMillis = Utils.byteArrayToInteger(byteArray, offset+22);
	}

	@Override
	public byte[] toByteArray() {
	
		byte[] out = new byte[28];
		
		int offset = super.toByteArray(out, 0);
		out[offset++] = 'G';
		offset = this.segmentIdentifier.toByteArray(out, offset);
		out[offset++] = (byte) (this.not?1:0);
		offset = Utils.integerToByteArray(this.timeoutMillis, out, offset);
		offset = Utils.integerToByteArray(this.timeoutInactiveMillis, out, offset);
		
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
	public long getTimeoutInactiveMillis() {
		return this.timeoutInactiveMillis;
	}

	@Override
	public String toString() {
		String out = super.toString() + " not:"+this.not+" si:"+this.segmentIdentifier+ " to:"+this.timeoutMillis;
		
		return out;
	}

}
