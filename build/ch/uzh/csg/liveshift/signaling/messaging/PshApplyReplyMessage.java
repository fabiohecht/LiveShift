package net.liveshift.signaling.messaging;

import net.liveshift.core.PeerId;
import net.liveshift.util.Utils;


public class PshApplyReplyMessage extends AbstractMessage {

	final float amount;

	public PshApplyReplyMessage(float amount, final PeerId myPeerId, final PeerId receiver) {
		super(myPeerId, receiver);
		
		this.amount = amount;
	}
	
	public PshApplyReplyMessage(final byte messageId, PeerId sender, final byte[] byteArray, int offset) {
		super(messageId, sender);
		
		this.amount = Utils.byteArrayToFloat(byteArray, offset+24);
	}
	
	@Override
	public byte[] toByteArray() {
		byte[] out = new byte[28];
		
		int offset = super.toByteArray(out, 0);
		out[offset++] = 'O';
		
		offset = Utils.floatToByteArray(this.amount, out, offset);

		return out;
	}
	
	public float getAmount() {
		return this.amount;
	}
	
	@Override
	public String toString() {
		String out = super.toString() + " amount: "+this.amount;
		
		return out;
	}
}
