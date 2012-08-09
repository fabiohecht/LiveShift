package net.liveshift.signaling.messaging;

import net.liveshift.core.PeerId;


public class PingMessage extends AbstractMessage {

	final private boolean request;
	/**
	 * used to request
	 */
	public PingMessage(final PeerId myPeerId, final PeerId receiver, final boolean request) {
		super(myPeerId, receiver);
		
		this.request = request;
	}
	
	/**
	 * used to reply
	 */
	public PingMessage(final byte messageId, Byte messageIdReply, final PeerId sender, final byte[] byteArray, final int offset) {
		super(messageId, messageIdReply, sender);
		
		this.request = byteArray[offset+6]==1;
	}
	
	@Override
	public byte[] toByteArray() {
		byte[] out = new byte[7];
		
		int offset = super.toByteArray(out, 0);
		out[offset++] = 'P';
		out[offset++] = (byte) (this.request?1:0);
			
		return out;
	}
	
	public boolean isRequest() {
		return request;
	}
	
	@Override
	public String toString() {
		String out = super.toString() + " req:"+this.request;
		
		return out;
	}
	
}
