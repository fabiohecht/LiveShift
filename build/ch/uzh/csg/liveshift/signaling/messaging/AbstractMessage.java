package net.liveshift.signaling.messaging;

import net.liveshift.core.PeerId;
import net.liveshift.util.TimeOutHashMap;

public abstract class AbstractMessage implements Comparable<AbstractMessage> {
	
	transient static final public byte PROTOCOL_VERSION = 1;  //denotes the version of the protocol used. will be useful in the future, to maintain backward compatibility or make a nicer transition.
	final private byte messageId;
	private Byte messageIdReply;
	final private PeerId sender;
	
	transient static final public int HEADER_SIZE = 5;
	transient private long receiveTimeMillis;  //only used for printing debug messages to get rid of timeouts
	transient private boolean yetAccepted = false;
	transient private boolean procesed = false;
	transient private AbstractMessage replyMessage;
	transient private PeerId replyReceiver;	  //only used for printing debug messages

	transient private static TimeOutHashMap<PeerId, Byte> lastGivenMessageId = new TimeOutHashMap<PeerId, Byte>(20000);  //message-ids created are sequential per destination peer ID

	public AbstractMessage(final PeerId sender, final PeerId receiver) {
		this.messageId = this.getNextMessageId(receiver);
		this.sender = sender;
	}
	
	protected AbstractMessage(final byte messageId, final PeerId sender) {
		this.messageId = messageId;
		this.sender = sender;
	}
	
	public AbstractMessage(final byte messageId, final Byte messageIdReply, final PeerId sender) {
		this.messageId = messageId;
		this.messageIdReply=messageIdReply;
		this.sender = sender;
	}

	public int toByteArray(final byte[] out, int offset) {
		out[offset++] = 0x15;  //15 = ls = LiveShift
		out[offset++] = PROTOCOL_VERSION;
		out[offset++] = this.messageId;
		out[offset++] = (byte) (this.messageIdReply==null?0:1);
		out[offset++] = (byte) (this.messageIdReply==null?0:this.messageIdReply);
		return offset;
	}
	abstract public byte[] toByteArray();
	
	public byte getMessageId() {
		return messageId;
	}
	
	public PeerId getSender() {
		return sender;
	}
	
	@Override
	public String toString() {
		String className = this.getClass().getCanonicalName();
		className = className.substring(className.lastIndexOf('.')+1);
		
		String out = className + " MID:" + this.messageId+" sender:" + (this.sender==null?"null":this.sender.getName());
		
		return out;
	}
	
	private synchronized byte getNextMessageId(final PeerId receiver) {
		
		Byte lastGivenMessageIdReceiver = lastGivenMessageId.get(receiver);
		
		if (lastGivenMessageIdReceiver==null)
			lastGivenMessageIdReceiver = 1;
		else
			lastGivenMessageIdReceiver++;
		
		lastGivenMessageId.put(receiver, lastGivenMessageIdReceiver);
		
		return lastGivenMessageIdReceiver;
	}
	
	@Override
	public int hashCode() {
		return this.messageId ^ this.sender.hashCode();
	}
	
	@Override
	public boolean equals(Object message0) {
		if (message0 == null || !(message0 instanceof AbstractMessage))
			return false;
		AbstractMessage m = (AbstractMessage) message0;
		return this.sender.equals(m.sender) && this.messageId==m.messageId;
	}
	
	@Override
	public int compareTo(AbstractMessage message0) {
		if (this.equals(message0))
			return 0;
		
		if (this.sender.equals(message0.sender)) {
			long middif = this.messageId-message0.messageId;
			if (middif!=0)
				return middif>0?1:-1;
			else
				return 0;	
		}
		
		long tdif = this.getReceiveTimeMillis() - message0.getReceiveTimeMillis();
		if (tdif!=0)
			return tdif>0?1:-1;
		
		return this.hashCode()-message0.hashCode();
	}

	public boolean isProcessed() {
		return this.procesed;
	}
	public void setProcessed() {
		this.procesed = true;
	}

	public void setReplyMessage(final AbstractMessage replyMessage, final PeerId replyReceiver) {
		this.replyMessage = replyMessage;
		if (replyMessage!=null) {
			replyMessage.messageIdReply = this.messageId;
			replyMessage.replyReceiver = replyReceiver;
		}		
	}
	public AbstractMessage getReplyMessage() {
		return this.replyMessage;
	}

	public long getReceiveTimeMillis() {
		return receiveTimeMillis;
	}
	public void setReceiveTimeMillis(long receiveTimeMillis) {
		this.receiveTimeMillis = receiveTimeMillis;
	}

	public boolean isYetAccepted() {
		return this.yetAccepted;
	}
	public void setYetAccepted() {
		this.yetAccepted=true;
	}

	public PeerId getReplyReceiver() {
		return this.replyReceiver;  //only used for printing debug messages
	}

	public Byte getMessageIdReply() {
		return messageIdReply;
	}
}

