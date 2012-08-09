package net.liveshift.dummy;

import net.liveshift.core.PeerId;
import net.liveshift.signaling.MessageSender;
import net.liveshift.signaling.messaging.AbstractMessage;


public class DummyMessageSender implements MessageSender {

	@Override
	public boolean sendMessage(AbstractMessage message, PeerId peerIdReceiver, boolean blockUntilReply) {
		System.out.println("sending message to "+peerIdReceiver+" : "+message.toString());
		return true;
	}

}
