package net.liveshift.signaling;

import net.liveshift.core.PeerId;
import net.liveshift.signaling.messaging.AbstractMessage;

public interface MessageSender {
	public boolean sendMessage(AbstractMessage message, PeerId peerId, boolean blockUntilReply);
}
