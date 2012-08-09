package net.liveshift.signaling;

import java.math.BigInteger;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import net.liveshift.core.Channel;
import net.liveshift.core.PeerId;
import net.liveshift.p2p.P2PAddress;
import net.liveshift.signaling.messaging.BlockReplyMessage;
import net.liveshift.signaling.messaging.BlockRequestMessage;
import net.liveshift.signaling.messaging.DisconnectMessage;
import net.liveshift.signaling.messaging.GrantedMessage;
import net.liveshift.signaling.messaging.HaveMessage;
import net.liveshift.signaling.messaging.InterestedMessage;
import net.liveshift.signaling.messaging.MessageFactory;
import net.liveshift.signaling.messaging.PeerSuggestionMessage;
import net.liveshift.signaling.messaging.PingMessage;
import net.liveshift.signaling.messaging.QueuedMessage;
import net.liveshift.signaling.messaging.SegmentBlock;
import net.liveshift.signaling.messaging.SenderNotFoundException;
import net.liveshift.signaling.messaging.SubscribeMessage;
import net.liveshift.signaling.messaging.SubscribedMessage;
import net.liveshift.signaling.messaging.BlockReplyMessage.BlockReplyCode;
import net.liveshift.storage.SegmentBlockMap;
import net.liveshift.storage.SegmentIdentifier;
import net.liveshift.video.PacketData;
import net.tomp2p.peers.Number160;

import org.junit.Test;

import net.liveshift.dummy.DummyPeerId;
import junit.framework.Assert;

public class MessageTest {
	Map<Integer, Channel> channelCatalog = new HashMap <Integer, Channel>();
	Map<P2PAddress, PeerId> peerIdCatalog = new HashMap<P2PAddress, PeerId>();;
	MessageFactory mf = new MessageFactory(channelCatalog, peerIdCatalog);

	Random rnd = new Random();

	private PeerId getPeedId() {
		PeerId peerId = new DummyPeerId(Number160.MAX_VALUE.shiftLeft(1).shiftRight(2).shiftLeft(1), 6666);
		this.peerIdCatalog.put(peerId.getDhtId(), peerId);
		return peerId;
	}
	
	@Test
	public void testSubscribeMessages() throws UnknownHostException, SenderNotFoundException, InterruptedException {
		
		Channel channel = new Channel("c0", (byte)rnd.nextInt(), rnd.nextInt());
		channelCatalog.put(channel.getID(), channel);
		
		SegmentIdentifier si = new SegmentIdentifier(channel, (byte)rnd.nextInt(), rnd.nextLong());
		SegmentBlockMap bm = new SegmentBlockMap();
		
		for (int i = 0; i< 120; i++)
			bm.set(rnd.nextInt(120));
		
		this.testSubscribeMessage(new SubscribeMessage(si, getPeedId(), getPeedId()));
	}	
	private void testSubscribeMessage(SubscribeMessage m) throws UnknownHostException, SenderNotFoundException, InterruptedException {
		
		SubscribeMessage m2 = (SubscribeMessage) mf.getMessage(m.getSender().getDhtId(), m.toByteArray(), 0);
		
		Assert.assertEquals(m, m2);
		Assert.assertEquals(m.getSegmentIdentifier(), m2.getSegmentIdentifier());
		Assert.assertEquals(m.getSender(), m2.getSender());
		Assert.assertEquals(m.getSender().getName(), m2.getSender().getName());

	}
	
	
	@Test
	public void testSubscribedMessages() throws UnknownHostException, SenderNotFoundException, InterruptedException {

		Channel channel = new Channel("c0", (byte)rnd.nextInt(), rnd.nextInt());
		channelCatalog.put(channel.getID(), channel);

		SegmentIdentifier si = new SegmentIdentifier(channel, (byte)rnd.nextInt(), rnd.nextLong());
		SegmentBlockMap bm = new SegmentBlockMap();
		
		int bps = SegmentBlock.getBlocksPerSegment();
		for (int i = 0; i< bps; i++)
			bm.set(rnd.nextInt(bps));
		
		this.testSubscribedMessage(new SubscribedMessage(si, bm, rnd.nextInt(), getPeedId(), getPeedId(), false));
		this.testSubscribedMessage(new SubscribedMessage(si, null, rnd.nextInt(), getPeedId(), getPeedId(), true));
	}	
	private void testSubscribedMessage(SubscribedMessage m) throws UnknownHostException, SenderNotFoundException, InterruptedException {

		SubscribedMessage m2 = (SubscribedMessage) mf.getMessage(m.getSender().getDhtId(), m.toByteArray(), 0);

		Assert.assertEquals(m, m2);
		Assert.assertEquals(m.getSegmentBlockMap(), m2.getSegmentBlockMap());
		Assert.assertEquals(m.getSegmentIdentifier(), m2.getSegmentIdentifier());
		Assert.assertEquals(m.getTimeoutMillis(), m2.getTimeoutMillis());
		Assert.assertEquals(m.isNot(), m2.isNot());
		Assert.assertEquals(m.getSender(), m2.getSender());

	}

	@Test
	public void testQueuedMessages() throws UnknownHostException, SenderNotFoundException, InterruptedException {

		Channel channel = new Channel("c0", (byte)rnd.nextInt(), rnd.nextInt());
		channelCatalog.put(channel.getID(), channel);

		SegmentIdentifier si = new SegmentIdentifier(channel, (byte)rnd.nextInt(), rnd.nextLong());
		SegmentBlockMap bm = new SegmentBlockMap();
		
		int bps = SegmentBlock.getBlocksPerSegment();
		for (int i = 0; i< bps; i++)
			bm.set(rnd.nextInt(bps));
		
		this.testQueuedMessage(new QueuedMessage(si, rnd.nextInt(), getPeedId(), getPeedId(), false));
		this.testQueuedMessage(new QueuedMessage(si, rnd.nextInt(), getPeedId(), getPeedId(), true));
	}	
	private void testQueuedMessage(QueuedMessage m) throws UnknownHostException, SenderNotFoundException, InterruptedException {

		QueuedMessage m2 = (QueuedMessage) mf.getMessage(m.getSender().getDhtId(), m.toByteArray(), 0);

		Assert.assertEquals(m, m2);
		Assert.assertEquals(m.getSegmentIdentifier(), m2.getSegmentIdentifier());
		Assert.assertEquals(m.getTimeoutMillis(), m2.getTimeoutMillis());
		Assert.assertEquals(m.getSender(), m2.getSender());

	}
	
	@Test
	public void testGrantedMessages() throws UnknownHostException, SenderNotFoundException, InterruptedException {

		Channel channel = new Channel("c0", (byte)rnd.nextInt(), rnd.nextInt());
		channelCatalog.put(channel.getID(), channel);

		SegmentIdentifier si = new SegmentIdentifier(channel, (byte)rnd.nextInt(), rnd.nextLong());
		SegmentBlockMap bm = new SegmentBlockMap();
		
		int bps = SegmentBlock.getBlocksPerSegment();
		for (int i = 0; i< bps; i++)
			bm.set(rnd.nextInt(bps));
		
		this.testGrantedMessage(new GrantedMessage(si, rnd.nextInt(), rnd.nextInt(), getPeedId(), getPeedId(), false));
		this.testGrantedMessage(new GrantedMessage(si, rnd.nextInt(), rnd.nextInt(), getPeedId(), getPeedId(), true));
	}	
	private void testGrantedMessage(GrantedMessage m) throws UnknownHostException, SenderNotFoundException, InterruptedException {

		GrantedMessage m2 = (GrantedMessage) mf.getMessage(m.getSender().getDhtId(), m.toByteArray(), 0);

		Assert.assertEquals(m, m2);
		Assert.assertEquals(m.getSegmentIdentifier(), m2.getSegmentIdentifier());
		Assert.assertEquals(m.getTimeoutMillis(), m2.getTimeoutMillis());
		Assert.assertEquals(m.getTimeoutInactiveMillis(), m2.getTimeoutInactiveMillis());
		Assert.assertEquals(m.isNot(), m2.isNot());
		Assert.assertEquals(m.getSender(), m2.getSender());
	}
	
	@Test
	public void testPingMessages() throws UnknownHostException, SenderNotFoundException, InterruptedException {
		
		Channel channel = new Channel("c0", (byte)rnd.nextInt(), rnd.nextInt());
		channelCatalog.put(channel.getID(), channel);
		
		for (int i = 0; i< 120; i++) {
			this.testPing(new PingMessage(getPeedId(), getPeedId(), rnd.nextBoolean()));
		}
	}	
	private void testPing(PingMessage m) throws UnknownHostException, SenderNotFoundException, InterruptedException {
		
		PingMessage m2 = (PingMessage) mf.getMessage(m.getSender().getDhtId(), m.toByteArray(), 0);
		
		Assert.assertEquals(m, m2);
		Assert.assertEquals(m.isRequest(), m2.isRequest());
		Assert.assertEquals(m.getSender(), m2.getSender());
	}
	
	
	@Test
	public void testHaveMessages() throws UnknownHostException, SenderNotFoundException, InterruptedException {
		
		Channel channel = new Channel("c0", (byte)rnd.nextInt(), rnd.nextInt());
		channelCatalog.put(channel.getID(), channel);
		
		for (int i = 0; i< 120; i++) {
			SegmentIdentifier si = new SegmentIdentifier(channel, (byte)rnd.nextInt(), rnd.nextLong());
			this.testHave(new HaveMessage(si, rnd.nextInt(), rnd.nextBoolean(), rnd.nextInt(), getPeedId(), getPeedId()));
		}
	}	
	private void testHave(HaveMessage m) throws UnknownHostException, SenderNotFoundException, InterruptedException {
		
		HaveMessage m2 = (HaveMessage) mf.getMessage(m.getSender().getDhtId(), m.toByteArray(), 0);
		
		Assert.assertEquals(m, m2);
		Assert.assertEquals(m.getSegmentIdentifier(), m2.getSegmentIdentifier());
		Assert.assertEquals(m.getBlockNumber(), m2.getBlockNumber());
		Assert.assertEquals(m.getRate(), m2.getRate());
		Assert.assertEquals(m.getSender(), m2.getSender());

	}
	
	@Test
	public void testDisconnectMessages() throws UnknownHostException, SenderNotFoundException, InterruptedException {
		
		Channel channel = new Channel("c0", (byte)rnd.nextInt(), rnd.nextInt());
		channelCatalog.put(channel.getID(), channel);
	
		for (int i = 0; i< 120; i++) {
			SegmentIdentifier si = new SegmentIdentifier(channel, (byte)rnd.nextInt(), rnd.nextLong());
			this.testDisconnect(new DisconnectMessage(si, rnd.nextBoolean(), rnd.nextBoolean(), getPeedId(), getPeedId()));
		}
	}	
	private void testDisconnect(DisconnectMessage m) throws UnknownHostException, SenderNotFoundException, InterruptedException {
		
		DisconnectMessage m2 = (DisconnectMessage) mf.getMessage(m.getSender().getDhtId(), m.toByteArray(), 0);
		
		Assert.assertEquals(m, m2);
		Assert.assertEquals(m.getSegmentIdentifier(), m2.getSegmentIdentifier());
		Assert.assertEquals(m.stopDownloading(), m2.stopDownloading());
		Assert.assertEquals(m.stopUploading(), m2.stopUploading());
		Assert.assertEquals(m.getSender(), m2.getSender());

	}
	
	@Test
	public void testBlockRequestMessages() throws UnknownHostException, SenderNotFoundException, InterruptedException {
		
		Channel channel = new Channel("c0", (byte)rnd.nextInt(), rnd.nextInt());
		channelCatalog.put(channel.getID(), channel);
		
		for (int i = 0; i< 120; i++) {
			SegmentIdentifier si = new SegmentIdentifier(channel, (byte)rnd.nextInt(), rnd.nextLong());
			this.testBlockRequest(new BlockRequestMessage(si, (byte)rnd.nextInt(), getPeedId(), getPeedId()));
		}
	}	
	private void testBlockRequest(BlockRequestMessage m) throws UnknownHostException, SenderNotFoundException, InterruptedException {
		
		BlockRequestMessage m2 = (BlockRequestMessage) mf.getMessage(m.getSender().getDhtId(), m.toByteArray(), 0);
		
		Assert.assertEquals(m, m2);
		Assert.assertEquals(m.getSegmentIdentifier(), m2.getSegmentIdentifier());
		Assert.assertEquals(m.getBlockNumber(), m2.getBlockNumber());
		Assert.assertEquals(m.getSender(), m2.getSender());
		
	}
	
	
	
	@Test
	public void testBlockReplyMessages() throws UnknownHostException, SenderNotFoundException, InterruptedException {

		Channel channel = new Channel("c0", (byte)rnd.nextInt(), rnd.nextInt());
		channelCatalog.put(channel.getID(), channel);
		
		for (int i = 0; i< 120; i++) {
			SegmentIdentifier si = new SegmentIdentifier(channel, (byte)rnd.nextInt(), rnd.nextLong());
			PacketData[] packets = new PacketData[1];
			byte[] bytes = new byte[10];
			rnd.nextBytes(bytes);
			packets[0] = new PacketData(bytes, rnd.nextLong(), rnd.nextLong(), (byte) rnd.nextInt());
			SegmentBlock sb = new SegmentBlock(si, rnd.nextInt(), packets, (byte)rnd.nextInt());
			this.testBlockReply(new BlockReplyMessage(BlockReplyCode.DONT_HAVE, sb, getPeedId(), getPeedId()));
			this.testBlockReply(new BlockReplyMessage(BlockReplyCode.GRANTED, sb, getPeedId(), getPeedId()));
			this.testBlockReply(new BlockReplyMessage(BlockReplyCode.NO_SLOT, sb, getPeedId(), getPeedId()));
			this.testBlockReply(new BlockReplyMessage(BlockReplyCode.REJECTED, sb, getPeedId(), getPeedId()));
		}
	}	
	private void testBlockReply(BlockReplyMessage m) throws UnknownHostException, SenderNotFoundException, InterruptedException {

		BlockReplyMessage m2 = (BlockReplyMessage) mf.getMessage(m.getSender().getDhtId(), m.toByteArray(), 0);

		Assert.assertEquals(m, m2);
		Assert.assertEquals(m.getReplyCode(), m2.getReplyCode());
		Assert.assertEquals(m.getSegmentBlock().getBlockNumber(), m2.getSegmentBlock().getBlockNumber());
		Assert.assertEquals(m.getSegmentBlock().getHopCount(), m2.getSegmentBlock().getHopCount());
		Assert.assertEquals(m.getSegmentBlock().getPackets().length, m2.getSegmentBlock().getPackets().length);
		Assert.assertEquals(m.getSegmentBlock().getSegmentIdentifier(), m2.getSegmentBlock().getSegmentIdentifier());
		Assert.assertEquals(m.getSender(), m2.getSender());

	}
	
	
	@Test
	public void testInterestedMessages() throws UnknownHostException, SenderNotFoundException, InterruptedException {
		
		Channel channel = new Channel("c0", (byte)rnd.nextInt(), rnd.nextInt());
		channelCatalog.put(channel.getID(), channel);
		
		for (int i = 0; i< 120; i++) {
			SegmentIdentifier si = new SegmentIdentifier(channel, (byte)rnd.nextInt(), rnd.nextLong());
			this.testUsUpdate(new InterestedMessage(si, getPeedId(), getPeedId(), false));
			this.testUsUpdate(new InterestedMessage(si, getPeedId(), getPeedId(), true));
		}
	}	
	private void testUsUpdate(InterestedMessage m) throws UnknownHostException, SenderNotFoundException, InterruptedException {
		
		InterestedMessage m2 = (InterestedMessage) mf.getMessage(m.getSender().getDhtId(), m.toByteArray(), 0);
		
		Assert.assertEquals(m, m2);
		Assert.assertEquals(m.getSegmentIdentifier(), m2.getSegmentIdentifier());
		Assert.assertEquals(m.isNot(), m2.isNot());
		Assert.assertEquals(m.getSender(), m2.getSender());
		
	}

	@Test
	public void testPeerSuggestionMessages() throws UnknownHostException, SenderNotFoundException, InterruptedException {
		
		Channel channel = new Channel("c0", (byte)rnd.nextInt(), rnd.nextInt());
		channelCatalog.put(channel.getID(), channel);
		
		for (int i = 0; i< 120; i++) {
			SegmentIdentifier si = new SegmentIdentifier(channel, (byte)rnd.nextInt(), rnd.nextLong());
			
			Set<PeerId> suggestedPeers = new HashSet<PeerId>();
			for (int j=0; j<this.rnd.nextInt(10); j++) {
				suggestedPeers.add(new DummyPeerId(new Number160(this.rnd.nextLong()), this.rnd.nextInt(65535)));
			}
			
			this.testPeerSuggestion(new PeerSuggestionMessage(si, suggestedPeers, getPeedId(), getPeedId()));
		}
	}	
	private void testPeerSuggestion(PeerSuggestionMessage m) throws UnknownHostException, SenderNotFoundException, InterruptedException {
		
		PeerSuggestionMessage m2 = (PeerSuggestionMessage) mf.getMessage(m.getSender().getDhtId(), m.toByteArray(), 0);
		
		Assert.assertEquals(m, m2);
		Assert.assertEquals(m.getSegmentIdentifier(), m2.getSegmentIdentifier());
		Assert.assertEquals(m.getSender(), m2.getSender());
		for (PeerId suggestedPeerId :m.getSuggestedPeerIds())
			Assert.assertTrue(m2.getSuggestedPeerIds().contains(suggestedPeerId));
	}

}