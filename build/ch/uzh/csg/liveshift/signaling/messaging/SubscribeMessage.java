package net.liveshift.signaling.messaging;

import java.util.Map;

import net.liveshift.core.Channel;
import net.liveshift.core.PeerId;
import net.liveshift.storage.SegmentIdentifier;
import net.liveshift.util.Utils;

public class SubscribeMessage extends AbstractMessage {
	
	final private SegmentIdentifier segmentIdentifier;
	
	public SubscribeMessage(final SegmentIdentifier segmentIdentifier, final PeerId myPeerId, final PeerId receiver) {
		super(myPeerId, receiver);
		
		this.segmentIdentifier = segmentIdentifier;
	}
	
	public SubscribeMessage(final byte messageId, PeerId sender, final byte[] byteArray, final Map<Integer, Channel> channelCatalog, int offset) {
		super(messageId, sender);
		offset+=2;
		this.segmentIdentifier = new SegmentIdentifier(channelCatalog.get(Utils.byteArrayToInteger(byteArray, offset+4)), byteArray[offset+8], Utils.byteArrayToLong(byteArray, offset+9));
	}
	
	@Override
	public byte[] toByteArray() {
		
		byte[] socketAddress = this.getSender().getDhtId().getSocketAddressAsByteArray();
		byte[] nameAsByteArray = this.getSender().getName().getBytes();
		byte[] out = new byte[6+13+1+socketAddress.length+1+nameAsByteArray.length];
		
		int offset = super.toByteArray(out, 0);
		out[offset++] = 'U';
		offset = this.segmentIdentifier.toByteArray(out, offset);
		
		//rest of peerId (name)
		out[offset++] = (byte) nameAsByteArray.length;
		System.arraycopy(nameAsByteArray, 0, out, offset, nameAsByteArray.length);
		
		return out;
	}

	public SegmentIdentifier getSegmentIdentifier() {
		return this.segmentIdentifier;
	}
	
	@Override
	public String toString() {
		String out = super.toString() + " si:"+this.segmentIdentifier;
		
		return out;
	}
}
