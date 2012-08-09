package net.liveshift.signaling;

import net.liveshift.core.PeerId;
import net.liveshift.p2p.P2PAddress;
import net.liveshift.signaling.messaging.AbstractMessage;

/**
 * The message reply handler interface, which handles received messages from other peers
 * Messages may have been initiated by this peer or other peers
 *
 * @author Fabio Victora Hecht
 * @author Kevin Leopold
 *
 */
public interface IncomingMessageHandler {

	AbstractMessage getMessage(P2PAddress senderP2pAddress,	byte[] incomingByteArray, int offset);
	AbstractMessage getMessage(PeerId peerIdSender, byte[] incomingByteArray, int offset);
	AbstractMessage handleIncomingMessage(AbstractMessage message);
	
	public void shutdown();

}
