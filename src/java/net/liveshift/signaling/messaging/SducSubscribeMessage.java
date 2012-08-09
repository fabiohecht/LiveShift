package net.liveshift.signaling.messaging;

import java.util.Map;

import net.liveshift.core.Channel;
import net.liveshift.core.PeerId;
import net.liveshift.storage.SegmentIdentifier;
import net.liveshift.util.Utils;


public class SducSubscribeMessage extends SubscribeMessage {

	final private float peerUploadCapacity;

	public SducSubscribeMessage(final SegmentIdentifier segmentIdentifier, final PeerId myPeerId, final PeerId receiver, final float peerUploadCapacity) {
		super(segmentIdentifier, myPeerId, receiver);

		this.peerUploadCapacity = peerUploadCapacity;
	}

	public SducSubscribeMessage(final byte messageId, PeerId sender, final byte[] byteArray, final Map<Integer, Channel> channelCatalog, final int offset) {
		super(messageId, sender, byteArray, channelCatalog, offset);
		
		this.peerUploadCapacity = Utils.byteArrayToFloat(byteArray, byteArray.length-4);
	}
	
	@Override
	public byte[] toByteArray() {
		byte[] superOut = super.toByteArray();
		byte[] out = new byte[superOut.length+4];
		
		System.arraycopy(superOut, 0, out, 0, superOut.length);
		int offset=5;
		out[offset++] = 'X';
		offset = Utils.floatToByteArray(peerUploadCapacity, out, superOut.length);
		
		return out;
	}

	public float getPeerUploadCapacity() {
		return this.peerUploadCapacity;
	}

	@Override
	public String toString() {
		String out = super.toString() + " upload:"+this.peerUploadCapacity;	
		return out;
	}
}
