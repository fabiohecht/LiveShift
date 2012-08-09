package net.liveshift.p2p;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.LogManager;

import junit.framework.Assert;

import net.liveshift.core.Channel;
import net.liveshift.core.PeerId;
import net.liveshift.p2p.P2PAddress;
import net.liveshift.p2p.Peer;
import net.liveshift.signaling.IncomingMessageHandler;
import net.liveshift.signaling.MessageListener;
import net.liveshift.signaling.MessageSender;
import net.liveshift.signaling.messaging.AbstractMessage;
import net.liveshift.signaling.messaging.BlockReplyMessage;
import net.liveshift.signaling.messaging.MessageFactory;
import net.liveshift.signaling.messaging.SegmentBlock;
import net.liveshift.signaling.messaging.SenderNotFoundException;
import net.liveshift.signaling.messaging.SubscribeMessage;
import net.liveshift.signaling.messaging.BlockReplyMessage.BlockReplyCode;
import net.liveshift.storage.SegmentIdentifier;
import net.liveshift.video.PacketData;

import org.junit.Test;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;


public class MessageSenderTest {

	private Random random = new Random();
	final Channel channel = new Channel("c1", (byte) 1, 1);
	final Map<Integer, Channel> channelCatalog = new HashMap<Integer, Channel>();

	// Init LogManager
	static {
		try {
			LogManager.getLogManager().readConfiguration(Peer.class.getResourceAsStream("/jdklogtest.properties"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	class Loader implements Runnable {
		
		public boolean go = true;
		
		@Override
		public void run() {
			HashSet<Integer> dumb = new HashSet();
			int i = 0;
			System.out.println("start load");
			while (go) {
				dumb.add(i);
				dumb.remove(i-1);
				i++;
			}
			System.out.println("done load");
		}
	}
	
	@Test
	public void testMessageSender() throws UnknownHostException, Exception {
		

		final int NUM_PEERS = 50;
		
		
		final AtomicInteger messagesReceived = new AtomicInteger();
		
		final Map<P2PAddress, PeerId> peerIdCatalog = new HashMap<P2PAddress, PeerId>();
			
		IncomingMessageHandler incomingMessageHandler = new IncomingMessageHandler() {
			
			private MessageFactory messageFactory = new MessageFactory(channelCatalog, peerIdCatalog);
			ConcurrentHashMap<PeerId, AtomicInteger> lastReceivedMessageId = new ConcurrentHashMap<PeerId, AtomicInteger>();
			
			@Override
			public void shutdown() {
				
			}

			@Override
			public AbstractMessage getMessage(PeerId peerIdSender, byte[] obj, int offset) {
				try {
					return messageFactory.getMessage(peerIdSender, obj, offset);
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return null;
			}
			
			@Override
			public AbstractMessage getMessage(P2PAddress senderP2pAddress,	byte[] incomingByteArray, int offset) {
				try {
					return messageFactory.getMessage(senderP2pAddress, incomingByteArray, offset);
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SenderNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return null;
			}


			@Override
			public AbstractMessage handleIncomingMessage(AbstractMessage message) {

				this.accountForMessage(message);
				
				return null;

			}
			
			private void accountForMessage(AbstractMessage message) {
				
				System.out.println("#=====> received "+message);
				try {
					byte expectedMessageId = this.incrementAndGetMessageId(message.getSender());
					
					messagesReceived.incrementAndGet();

					//Assert.assertEquals(expectedMessageId, message.getMessageId());
					
				}
				finally {
					//lastReceivedMessageId=message.getMessageId();
				}
			}

			private byte incrementAndGetMessageId(PeerId sender) {

				synchronized (this.lastReceivedMessageId) {
						
					AtomicInteger l = this.lastReceivedMessageId.get(sender);
					if (l==null) {
						l=new AtomicInteger();
						this.lastReceivedMessageId.put(sender, l);
					}
					return (byte) l.incrementAndGet(); 

				}
			}

		};
		

		Peer[] peers = new Peer[NUM_PEERS];
		//DirectMessageSender[] senders = new DirectMessageSender[NUM_PEERS];
		Peer[] senders = new Peer[NUM_PEERS];
		
		for (int i=0; i<NUM_PEERS; i++) {
			peers[i] = new Peer();
			peers[i].connect(new InetSocketAddress(InetAddress.getLocalHost(), 10000+i), new InetSocketAddress("127.0.0.1", 10000), "lo", "p"+i, false);
			
			//senders[i] = new DirectMessageSender(peers[i].getMyId());
			senders[i] = peers[i];
			senders[i].registerIncomingMessageHandler(incomingMessageHandler);
			senders[i].startListening();
			peerIdCatalog.put(peers[i].getP2PAddress(), peers[i].getMyId());
		}
		
		
		Loader loader = new Loader();
		Thread tloader = new Thread(loader);
		
		channelCatalog.put(channel.getID(),channel);
		
		System.out.println("starting");
		AbstractMessage m;
		
		//tloader.start();
		
		//int msgsPerSecond=1;
		//int numMessagesSent=10;
		//for (int i=0; i<numMessagesSent; i++) {
		
		long duration = 10000;
		long t0=new Date().getTime();
		int numMessagesSent=0;
		while (t0+duration>new Date().getTime()) {
			int sender=this.random.nextInt(NUM_PEERS-1)+1;
			PeerId peerIdSender = peers[sender].getMyId();
			int receiver=0;
			while (receiver==-1 || receiver==sender) {
				receiver=this.random.nextInt(NUM_PEERS);
			}
			PeerId peerIdReceiver = peers[receiver].getMyId();
			
			m = this.getMessageToSend(peerIdSender, peerIdReceiver);
			System.out.println(peerIdSender.getName()+" -> "+peerIdReceiver.getName()+" : "+m);
			
			Assert.assertTrue(senders[sender].sendMessage(m, peerIdReceiver, false));

			//Thread.sleep(1000/msgsPerSecond);
			numMessagesSent++;
		}
		/*
		int timeLimit = 10000*10000;
		long t0 = new Date().getTime();
		while (numMessagesSent!=messagesReceived.get() && new Date().getTime()-t0<timeLimit) {
			System.out.println("sent "+numMessagesSent+", received "+messagesReceived.get());
			Thread.sleep(1000);
		}
		Assert.assertEquals(numMessagesSent, messagesReceived.get());
		*/
		
		System.out.println("sent "+numMessagesSent+" received "+messagesReceived.get()+" in "+duration+" ms");
		loader.go = false;
		
		//Thread.sleep(2000);
	}
	
	private AbstractMessage getMessageToSend(PeerId peerIdSender, PeerId peerIdReceiver) {
		
		float p=this.random.nextFloat();
		
		SegmentIdentifier si = new SegmentIdentifier(channel, (byte) 0, 1);
		
		if (p<.5)
			return new SubscribeMessage(si, peerIdSender, peerIdReceiver);
		else {
			int pdNum=50*5;
			int pdSize=1500;
			PacketData[] pd = new PacketData[pdNum];
			for (int i=0; i<pdNum; i++) {
				byte[] pdData = new byte[pdSize];
				this.random.nextBytes(pdData);
				pd[i] = new PacketData(pdData, new Date().getTime(), i, (byte) 0);
			}
			return new BlockReplyMessage(BlockReplyCode.GRANTED, new SegmentBlock(si, 666, pd), peerIdSender, peerIdReceiver);
		}
	}
}
