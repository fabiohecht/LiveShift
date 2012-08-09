package net.liveshift.signaling;

import net.liveshift.core.PeerId;
import net.liveshift.signaling.messaging.AbstractMessage;
import net.liveshift.util.TimeOutHashMap;
import net.liveshift.util.TimeOutHashMap.WrappedValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MessageFlowControl {

	final private static Logger logger = LoggerFactory.getLogger(MessageFlowControl.class);

	static final int MAX_WAIT_TIME_MILLIS = 2500;
	static final int RESET_NUMBERING_IDLE_TIME_MILLIS = 20000;
	
	final TimeOutHashMap<PeerId, Byte> lastReceivedMessageId = new TimeOutHashMap<PeerId, Byte>(RESET_NUMBERING_IDLE_TIME_MILLIS);
	final TimeOutHashMap<PeerId, Object> gapLocks = new TimeOutHashMap<PeerId, Object>(RESET_NUMBERING_IDLE_TIME_MILLIS);
	
	public enum FlowControlResponse{ACCEPTED, REJECTED, WAIT}
	
	public FlowControlResponse acceptMessage(final AbstractMessage message, final long referenceTime) {

		//per peerid lock
		Object lock;
		synchronized (this.gapLocks) {
			lock = this.gapLocks.get(message.getSender());
			if (lock==null) {
				lock=new Object();
				this.gapLocks.put(message.getSender(), lock);
			}
		}

		synchronized (lock) {
			
			if (message.isYetAccepted())
				return FlowControlResponse.ACCEPTED;

			WrappedValue wrappedValue = lastReceivedMessageId.getWrappedValue(message.getSender());
			Byte lastReceivedMessageIdFromSender = null;
			long timeToTimeOut = -1;
			if (wrappedValue!=null) {
				lastReceivedMessageIdFromSender = (Byte) wrappedValue.get();
				timeToTimeOut = wrappedValue.getTimeToTimeout();
			}
			
			if (lastReceivedMessageIdFromSender!=null && (byte)(lastReceivedMessageIdFromSender+1) == message.getMessageId()) {
				//in order, no gap, perfect
				if (logger.isDebugEnabled()) logger.debug("message "+message+" is in perfect order, accepting");
	
				lastReceivedMessageId.put(message.getSender(), message.getMessageId());
				message.setYetAccepted();

				return FlowControlResponse.ACCEPTED;
			}
			else if (message.getMessageId()==1 && (lastReceivedMessageIdFromSender==null || timeToTimeOut < RESET_NUMBERING_IDLE_TIME_MILLIS* .1)) {
				//first message received (in a while) must be id=1
				if (logger.isDebugEnabled()) logger.debug("message "+message+" is a pioneer, accepting");

				lastReceivedMessageId.put(message.getSender(), message.getMessageId());
				message.setYetAccepted();

				return FlowControlResponse.ACCEPTED;
			}			
			else if (lastReceivedMessageIdFromSender!=null && (byte)(message.getMessageId() - lastReceivedMessageIdFromSender) <= 0) {
				//too late, or the same
				logger.warn("message "+message+" is too late, dropping");
				
				return FlowControlResponse.REJECTED;
			}
			else if (referenceTime - message.getReceiveTimeMillis() < MAX_WAIT_TIME_MILLIS) {				
				//would leave a gap, waits for the gap to be filled
				if (logger.isDebugEnabled()) logger.debug("message "+message+" may be too early, waiting");
				
				return FlowControlResponse.WAIT;
			}
			else {
				//gives up filling gap 
				if (logger.isDebugEnabled()) logger.debug("message "+message+" gave up filling gap, accepting");
	
				lastReceivedMessageId.put(message.getSender(), message.getMessageId());
				message.setYetAccepted();

				return FlowControlResponse.ACCEPTED;
			}
			
		}
	}

}
