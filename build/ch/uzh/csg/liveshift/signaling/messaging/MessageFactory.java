package net.liveshift.signaling.messaging;

import java.net.UnknownHostException;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.liveshift.core.Channel;
import net.liveshift.core.PeerId;
import net.liveshift.p2p.P2PAddress;


public class MessageFactory {
	
	final private static Logger logger = LoggerFactory.getLogger(MessageFactory.class);

	final private Map<P2PAddress, PeerId> peerIdCatalog;
	private Map<Integer,Channel> channelCatalog;
	
	public MessageFactory() {
		this.peerIdCatalog = new HashMap<P2PAddress, PeerId>();
	}
	
	public MessageFactory(final Map<Integer,Channel> channelCatalog, Map<P2PAddress, PeerId> peerIdCatalog) {
		this.peerIdCatalog = peerIdCatalog;
		this.channelCatalog = channelCatalog;
	}
	
	/*
	 * returns an instance of the message by deserializing the given byteArray
	 */

	public AbstractMessage getMessage(final P2PAddress senderP2pAddress, final byte[] byteArray, final int offset) throws UnknownHostException, SenderNotFoundException, InterruptedException {
		
		PeerId sender = this.getSender(senderP2pAddress, byteArray, offset);

		if (sender==null) {
			logger.error("Sender not found in message of type "+(char)byteArray[offset+3]+", looking for peer:"+senderP2pAddress+ " in peercatalog "+this.peerIdCatalog);
			if (logger.isDebugEnabled()) {
				String debug = "";
				for (byte element : byteArray)
					debug += element + ":";
				logger.debug("message is ["+debug+"]");
			}
					
			throw new InvalidParameterException("Sender not found");
		}
		
		return getMessage(sender, byteArray, offset);
	}
	
	public void putPeerId(PeerId peerId) {
		synchronized (this.peerIdCatalog) {
			logger.debug("Writing new peerId "+peerId+", in peercatalog "+this.peerIdCatalog);
			P2PAddress dhtId = peerId.getDhtId();
			if (!this.peerIdCatalog.containsKey(dhtId))
				this.peerIdCatalog.put(dhtId, peerId);
		}
	}

	public void registerChannelList(Map<Integer, Channel> channels) {
		this.channelCatalog = channels;
	}

	public AbstractMessage getMessage(PeerId sender, byte[] byteArray, int offset) throws UnknownHostException {

		if (this.channelCatalog == null) {
			logger.error("need to set channelCatalog first!");
			throw new RuntimeException("need to set channelCatalog first!");
		}
		
		//decides what type of message it is, instantiates it, and returns it
		if (byteArray[offset] != 0x15) //not liveshift
			throw new InvalidParameterException("LiveShift messages must start with 0x15");
		if (byteArray[offset+1] > AbstractMessage.PROTOCOL_VERSION) //not this protocol
			throw new InvalidParameterException("I don't undestand this protocol version");
		
		byte messageId = byteArray[offset+2];
		Byte messageIdReply = null; 
		if (byteArray[offset+3]==(byte) 1) {
			messageIdReply = byteArray[offset+4];
		}

		switch (byteArray[offset+5]) {
			case 'P':
				return new PingMessage(messageId, messageIdReply, sender, byteArray, offset);
			case 'H':
				return new HaveMessage(messageId, sender, byteArray, channelCatalog, offset);
			case 'D':
				return new DisconnectMessage(messageId, sender, byteArray, channelCatalog, offset);
			case 'B':
				return new BlockRequestMessage(messageId, sender, byteArray, channelCatalog, offset);
			case 'C':
				return new BlockReplyMessage(messageId, messageIdReply, sender, byteArray, channelCatalog, offset);
			case 'U':
				return new SubscribeMessage(messageId, sender, byteArray, channelCatalog, offset);
			case 'V':
				return new SubscribedMessage(messageId, messageIdReply, sender, byteArray, channelCatalog, offset);
			case 'Q':
				return new QueuedMessage(messageId, messageIdReply, sender, byteArray, channelCatalog, offset);
			case 'G':
				return new GrantedMessage(messageId, messageIdReply, sender, byteArray, channelCatalog, offset);
			case 'I':
				return new InterestedMessage(messageId, sender, byteArray, channelCatalog, offset);
			case 'X':
				return new SducSubscribeMessage(messageId, sender, byteArray, channelCatalog, offset);
			case 'S':
				return new PeerSuggestionMessage(messageId, sender, byteArray, channelCatalog, offset);
/*
			case 'J':
				return new PshDownloadersRequestMessage(messageId, sender, byteArray, peerIdCatalog, offset);
			case 'K':
				return new PshDownloadersReplyMessage(messageId, sender, byteArray, peerIdCatalog, offset);
			case 'L':
				return new PshCheckRequestMessage(messageId, sender, byteArray, peerIdCatalog, offset);
			case 'M':
				return new PshCheckReplyMessage(messageId, sender, byteArray, peerIdCatalog, offset);
			case 'N':
				return new PshApplyRequestMessage(messageId, sender, byteArray, peerIdCatalog, offset);
			case 'O':
				return new PshApplyReplyMessage(messageId, sender, byteArray, offset);
		*/
		
		}
		
		return null;
	}
	
	private PeerId getSender(final P2PAddress senderP2pAddress, final byte[] byteArray, final int offset) throws InterruptedException, UnknownHostException {
		
		PeerId sender = null;
		int countDown = 5;
		while (sender == null && countDown-- > 0) {
			if (byteArray[offset+3]=='U' || byteArray[offset+3]=='X') {
	
				synchronized (this.peerIdCatalog) {
					
					sender = this.peerIdCatalog.get(senderP2pAddress);
					if (sender==null) {
						sender = new PeerId(byteArray, offset+17, senderP2pAddress);
						logger.debug("Writing new peerId "+sender+", in peercatalog "+this.peerIdCatalog);
						this.peerIdCatalog.put(senderP2pAddress, sender);
					}
				}
			}
			else {
				synchronized (this.peerIdCatalog) {
					sender = this.peerIdCatalog.get(senderP2pAddress);
				}
			}
			
			if (sender == null) {
				//it could be that some message sneaked in before the first one
				logger.debug("Sender not found in message of type "+(char)byteArray[offset+3]+", looking for peer:"+senderP2pAddress+ " in peercatalog "+this.peerIdCatalog+", will try again");
				Thread.sleep(100);
			}
		}
		
		return sender;
	
	}
}
