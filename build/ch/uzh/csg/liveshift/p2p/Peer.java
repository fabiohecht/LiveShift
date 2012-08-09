package net.liveshift.p2p;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import net.tomp2p.connection.Bindings;
import net.tomp2p.connection.Bindings.Protocol;
import net.tomp2p.futures.BaseFutureAdapter;
import net.tomp2p.futures.FutureBootstrap;
import net.tomp2p.futures.FutureDHT;
import net.tomp2p.futures.FutureDiscover;
import net.tomp2p.futures.FutureResponse;
import net.tomp2p.futures.FutureTracker;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.peers.PeerStatusListener.Reason;
import net.tomp2p.rpc.RawDataReply;
import net.tomp2p.storage.Data;
import net.tomp2p.utils.Utils;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.liveshift.core.Channel;
import net.liveshift.core.DHTConnectionException;
import net.liveshift.core.PeerId;
import net.liveshift.download.Tuner;
import net.liveshift.signaling.IncomingMessageHandler;
import net.liveshift.signaling.MessageListener;
import net.liveshift.signaling.MessageSender;
import net.liveshift.signaling.VideoSignaling;
import net.liveshift.signaling.messaging.AbstractMessage;
import net.liveshift.storage.SegmentIdentifier;
import net.liveshift.time.Clock;
import net.liveshift.util.ExecutorPool;
import net.liveshift.util.MovingAverage;

/**
 * The implementation of the underlying p2p overlay
 * 
 * @author Thomas Bocek
 * @author Fabio Hecht
 * @author Kevin Leopold
 */
public class Peer implements DHTInterface, MessageSender, MessageListener {
	final private static Logger logger = LoggerFactory.getLogger(Peer.class);

	private net.tomp2p.p2p.Peer tomP2pPeer;
	final private net.tomp2p.p2p.PeerMaker tomP2pPeerMaker;

	// TODO: really bad distribution
	private final static Number160 CHANNEL_LIST_KEY = Number160.createHash("channel");
	private final static Number160 SEGMENT_DOMAIN = Number160.createHash("segment");
	private final static Number160 CHANNEL_LIST_DOMAIN = Number160.createHash("channellist");

	private boolean connected = false;
	private PeerId myPeerId;

	private IncomingMessageHandler incomingMessageHandler;
	private boolean receiveMessages = false;
	private PeerStatusNotifier peerStatusNotifier;

	private final MovingAverage sentMessagesAverage = new MovingAverage(Clock.getMainClock(), 5, 1000);
	private final MovingAverage dhtOperationsAverage = new MovingAverage(Clock.getMainClock(), 5, 1000);
	

	private final Runnable statsReporterRunner = new Runnable() {
		@Override
		public void run() {
			logger.info("sentMessagesAverage:" + sentMessagesAverage.getAverage());
			logger.info("dhtOperationsAverage:" + dhtOperationsAverage.getAverage());
		}
	};
	private ScheduledFuture<?> statsReporterRunnerScheduledFuture;

	private FutureDHT publishedChannelHandle;

	public Peer() {
		this.tomP2pPeerMaker = new net.tomp2p.p2p.PeerMaker(Utils.createRandomNodeID());

		this.statsReporterRunnerScheduledFuture = ExecutorPool.getScheduledExecutorService().scheduleAtFixedRate(statsReporterRunner, 5000, 5000,
				TimeUnit.MILLISECONDS);
	}

	@Override
	public boolean connect(InetSocketAddress localSocket, InetSocketAddress bootstrapSocket, String iface, String peerName, boolean doUpnp) throws DHTConnectionException {
		Bindings bindInformation = new Bindings(Protocol.IPv4, iface);

		try {
			return this.connect(localSocket, bootstrapSocket, iface, peerName, bindInformation, doUpnp);
		}
		catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			throw new DHTConnectionException(e);
		}
	}

	@Override
	public boolean connect(InetSocketAddress localSocket, InetSocketAddress bootstrapSocket, String iface, String peerName, InetAddress publicAddress,
			int publicPorts, boolean doUpnp) throws DHTConnectionException{
		Bindings bindInformation = new Bindings(Protocol.IPv4, publicAddress, publicPorts, publicPorts);
		bindInformation.addInterface(iface);
		
		try {
			return this.connect(localSocket, bootstrapSocket, iface, peerName, bindInformation, doUpnp);
		}
		catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			throw new DHTConnectionException(e);
		}
	}

	private synchronized boolean connect(InetSocketAddress localSocket, InetSocketAddress bootstrapSocket, String iface, String peerName,
			Bindings bindInformation, boolean doUpnp) throws DHTConnectionException {
		if (tomP2pPeer!= null && (tomP2pPeer.isListening() || tomP2pPeer.isRunning()))
			tomP2pPeer.shutdown();
		try {
			tomP2pPeer = tomP2pPeerMaker.setPorts(localSocket.getPort()).setBindings(bindInformation).makeAndListen();
		}
		catch (IOException e) {
			logger.error("IOException while makeAndListen");
			throw new DHTConnectionException(e);
		}

		if (doUpnp) {
			FutureDiscover futureDiscover = tomP2pPeer.discover().setPeerAddress(new PeerAddress(Number160.ZERO, bootstrapSocket)).start();

			futureDiscover.awaitUninterruptibly();
			
			if (futureDiscover.isFailed()) {
				logger.error("futureDiscover isFailed");
				throw new DHTConnectionException();
			}
		}
		
		if (bootstrapSocket!=null) {
			// tomP2pPeer.getPeerBean().getTrackerStorage().setTrackerTimoutSeconds(30);
			FutureBootstrap bootstrap = tomP2pPeer.bootstrap().setInetAddress(bootstrapSocket.getAddress()).setPorts(bootstrapSocket.getPort()).start();
			bootstrap.awaitUninterruptibly();

			connected = bootstrap.isSuccess();
		}
		else  {
			connected=true;
		}
			
		if (connected) {
			this.myPeerId = new PeerId(new P2PAddress(tomP2pPeer.getPeerAddress()));

			if (peerName != "")
				this.myPeerId.setName(peerName);
			else
				this.myPeerId.setName(this.myPeerId.getDhtId().toString());
		}
		else {
			tomP2pPeer.shutdown();
			
			logger.error("problem bootstrapping");
			throw new DHTConnectionException();
		}

		// if not first peer, then connected= tomP2pPeer.getPeerBean().getPeerMap().size()>0;

		return connected;
	}

	@Override
	public void addPeerOfflineListener(Tuner tuner, VideoSignaling videoSignaling) {

		Set<PeerFailureNotifier> failureNotifiers = new HashSet<PeerFailureNotifier>();
		failureNotifiers.add(tuner);
		failureNotifiers.add(videoSignaling);

		this.peerStatusNotifier = new PeerStatusNotifier(failureNotifiers);
		tomP2pPeer.getPeerBean().getPeerMap().addPeerOfflineListener(this.peerStatusNotifier);
	}

	@Override
	public void disconnect() throws InterruptedException {
		this.statsReporterRunnerScheduledFuture.cancel(false);

		try {
			this.tomP2pPeer.shutdown();
		}
		catch (RuntimeException re) {
			logger.warn("RunTime exception when disconnecting: " + re.getMessage());
			re.printStackTrace();
		}
		this.connected = false;
	}

	@Override
	public boolean isConnected() {
		return connected;
	}

	@Override
	public PeerId getMyId() {
		return this.myPeerId;
	}

	private Number160 hashSegmentIdentifier(SegmentIdentifier segmentIdentifier) {
		return Utils.makeSHAHash(segmentIdentifier.toByteArray());
	}

	@Override
	public Set<Channel> getChannelSet() throws IOException, ClassNotFoundException {
		this.dhtOperationsAverage.inputValue(1);
		logger.info("dhtOperation getChannelList()");
		FutureDHT futureDHT = tomP2pPeer.get(CHANNEL_LIST_KEY).setAll().setDomainKey(CHANNEL_LIST_DOMAIN).start();
		futureDHT.awaitUninterruptibly();
		Set<Channel> result = new HashSet<Channel>();
		for (Data data : futureDHT.getDataMap().values())
			result.add((Channel) data.getObject());
		for(PeerAddress peerAddress:futureDHT.getRawData().keySet())
		{
			System.out.println(peerAddress);
		}
		return result;
	}

	@Override
	public boolean publishChannel(Channel channel) throws IOException {
		this.dhtOperationsAverage.inputValue(1);
		logger.info("dhtOperation publishChannel(" + channel + ")");
		Number160 key = new Number160(channel.getName().hashCode());
		System.out.println("publish "+channel.getName()+" "+channel.getName().hashCode());
		Data data = new Data(channel);
		// data.setTTLSeconds(15);
		publishedChannelHandle = tomP2pPeer.put(CHANNEL_LIST_KEY).setData(key, data).setRefreshSeconds(60).setDomainKey(CHANNEL_LIST_DOMAIN).start();
		publishedChannelHandle.awaitUninterruptibly();
		return !publishedChannelHandle.isFailed();
	}

	@Override
	public void unPublishChannel(Channel channel) {
		this.dhtOperationsAverage.inputValue(1);
		logger.info("dhtOperation unPublishChannel(" + channel + ")");
		Number160 key = new Number160(channel.getName().hashCode());
		System.out.println("unpublish "+channel.getName()+" "+channel.getName().hashCode());

		publishedChannelHandle.shutdown();
		FutureDHT futureDHT = this.tomP2pPeer.remove(CHANNEL_LIST_KEY).setContentKey(key).setDomainKey(CHANNEL_LIST_DOMAIN).start();
		futureDHT.awaitUninterruptibly();
	}

	@Override
	public FutureTracker getPeerList(SegmentIdentifier segmentIdentifier, int howMany) throws IOException, ClassNotFoundException {
		this.dhtOperationsAverage.inputValue(1);
		logger.info("dhtOperation getPeerList(" + segmentIdentifier + "," + howMany + ")");

		// TODO at the moment, it's filtering to howMany at the client-side and
		// not taking randomness into account
		Number160 locationKey = hashSegmentIdentifier(segmentIdentifier);
		if (logger.isDebugEnabled())
			logger.debug("looking up P2P: " + locationKey);

		FutureTracker futureTracker = tomP2pPeer.getTracker(locationKey).setExpectAttachement().setDomainKey(SEGMENT_DOMAIN).start();
		return futureTracker;
		// FutureDHT futureDHT = tomP2pPeer.get(locationKey, "segment", null, routingGetConfiguration,
		// p2pGet, new CumulativeScheme());
		// futureTracker.awaitUninterruptibly();
		// Map<PeerAddress,Data> peers = futureTracker.getTrackers();
		// List<PeerId> result = fillPeerID(segmentIdentifier, howMany, peers);
		// if (logger.isDebugEnabled()) logger.debug("after filtering, returning: " + result + "for si:"+segmentIdentifier);
		// return result;
	}

	@Override
	public FuturePublish publishSegment(final SegmentIdentifier segmentIdentifier) {
		this.dhtOperationsAverage.inputValue(1);
		logger.info("dhtOperation publishSegment(" + segmentIdentifier + ")");

		Number160 locationKey = hashSegmentIdentifier(segmentIdentifier);
		if (logger.isDebugEnabled())
			logger.debug(this.getMyId() + " publish: " + segmentIdentifier + " key:" + locationKey);

		if (logger.isDebugEnabled())
			logger.debug("publishing P2P: " + locationKey + "/" + tomP2pPeer.getPeerID());
		final FuturePublish futurePublish = new FuturePublish();
		FutureTracker futureTracker = null;

		// tomP2pPeer.getPeerBean().getTrackerStorage().setTrackerTimoutSeconds(1800);
	
		futureTracker = tomP2pPeer.addTracker(locationKey).setAttachement(this.getMyId().getName().getBytes()).setDomainKey(SEGMENT_DOMAIN).start();

		if (futureTracker == null)
			return null;

		// this is just a wrapper
		futureTracker.addListener(new BaseFutureAdapter<FutureTracker>() {
			@Override
			public void operationComplete(FutureTracker future) throws Exception {
				if (future.isSuccess())
					futurePublish.setTrackers(future.getPotentialTrackers(), future.getDirectTrackers(), future.getRawPeersOnTracker());
				else
					futurePublish.setFailed(future.getFailedReason());
			}
		});

		return futurePublish;
	}

	@Override
	public boolean sendMessage(final AbstractMessage message, final PeerId peerIdReceiver, final boolean blockUntilReply) {
		if (!blockUntilReply) {
			ExecutorPool.getGeneralExecutorService().submit(new Runnable() {
				@Override
				public void run() {
					try {
						sendMessage(message, peerIdReceiver, blockUntilReply, 3);
					}
					catch (Exception e) {
						// just so it doesn't die silently if an unhandled exception happened
						logger.error("error in sendMessage(" + message + "," + peerIdReceiver + "," + blockUntilReply + "): " + e.getMessage());
						e.printStackTrace();
					}
				}
			});
			return true;
		}
		else
			return sendMessage(message, peerIdReceiver, blockUntilReply, 3);

	}

	private boolean sendMessage(final AbstractMessage liveshiftMessage, final PeerId peerIdReceiver, final boolean blockUntilReply, final int countDown) {
		byte[] message = liveshiftMessage.toByteArray();

		logger.info("sending message (" + liveshiftMessage + ") to " + peerIdReceiver.getName() + " block=" + blockUntilReply + " countDown=" + countDown
				+ " size=" + message.length);

		this.sentMessagesAverage.inputValue(1);

		if (!this.tomP2pPeer.getDirectDataRPC().hasRawDataReply())
			throw new RuntimeException("Please add a PeerHandler first, otherwise we don't get any replies");

		if (countDown <= 0) {
			logger.warn("giving up resending message:" + liveshiftMessage + " to:" + peerIdReceiver);

			if (this.peerStatusNotifier != null)
				this.peerStatusNotifier.peerOffline(peerIdReceiver.getDhtId().getPeerAddress(), Reason.NOT_REACHABLE);

			return false;
		}

		try {
			final FutureResponse futureData = this.tomP2pPeer.sendDirect().setPeerAddress(peerIdReceiver.getDhtId().getPeerAddress()).setBuffer(ChannelBuffers.wrappedBuffer(message)).start();

			if (blockUntilReply)
				futureData.await();

			futureData.addListener(new BaseFutureAdapter<FutureResponse>() {

				@Override
				public void operationComplete(final FutureResponse future) throws Exception {

					if (future.isSuccess() && future.getBuffer() == null) {
						if (logger.isDebugEnabled())
							logger.debug("future.isSuccess() && future.getBuffer()==null");
						return;
					}
					else if (future.isSuccess() && future.getBuffer() != null) {
						// ByteBuffer bb = future.getBuffer().toByteBuffer();
						// final Object obj = Utils.decodeJavaObject(bb.array(), bb.arrayOffset() + bb.position(), bb.limit());
						// if (obj instanceof byte[]) {

						ExecutorPool.getGeneralExecutorService().submit(new Runnable() {

							@Override
							public void run() {
								try {
									// processes the reply

									AbstractMessage message = incomingMessageHandler.getMessage(peerIdReceiver.getDhtId(), future.getBuffer().array(), future
											.getBuffer().arrayOffset());

									incomingMessageHandler.handleIncomingMessage(message);
								}
								catch (Exception e) {
									// just so it doesn't die silently if an unhandled exception happened
									logger.error("error handling IncomingMessage:" + e.getMessage());
									e.printStackTrace();
								}

							}
						});
						// }
					}
					else {
						// resend up to 3 times
						Thread.sleep(15 * (4 - countDown));
						if (logger.isDebugEnabled())
							logger.debug("resending message " + liveshiftMessage + " to " + peerIdReceiver + " (countdown " + countDown + ") reason:"
									+ future.getFailedReason());
						sendMessage(liveshiftMessage, peerIdReceiver, blockUntilReply, countDown - 1);
					}
				}
			});

		}
		catch (InterruptedException e) {
			logger.warn("interrupted (InterruptedException)");
			return false;
		}
		catch (Exception e) {
			logger.error("exception caught: " + e.getMessage());
			return false;
		}
		return true;
	}

	@Override
	public void registerIncomingMessageHandler(IncomingMessageHandler incomingMessageHandler) {
		this.incomingMessageHandler = incomingMessageHandler;
	}

	@Override
	public void startListening() throws Exception {
		if (this.incomingMessageHandler == null)
			throw new Exception("registerIncomingMessageHandler() must be called before startListening()");

		this.receiveMessages = true;
		tomP2pPeer.setRawDataReply(new RawDataReply() {

			@Override
			public ChannelBuffer reply(final PeerAddress sender, final ChannelBuffer requestBuffer) throws Exception {
				if (receiveMessages) {
					AbstractMessage replyMessage = null;

					P2PAddress senderP2pAddress = new P2PAddress(sender);

					AbstractMessage message = incomingMessageHandler.getMessage(senderP2pAddress, requestBuffer.array(), requestBuffer.arrayOffset());
					replyMessage = incomingMessageHandler.handleIncomingMessage(message);

					if (replyMessage != null) {

						// logs reply message
						sentMessagesAverage.inputValue(1);

						byte[] replyByteArray = replyMessage.toByteArray();

						logger.info("sending reply (" + replyMessage + ") to:" + replyMessage.getReplyReceiver() + " size=" + replyByteArray.length);

						return ChannelBuffers.wrappedBuffer(replyByteArray);
					}

				}
				return requestBuffer;

			}
		});
	}

	@Override
	public void stopListening() {
		this.receiveMessages = false;
		this.incomingMessageHandler.shutdown();
	}

	public void shutdown() throws InterruptedException {
		tomP2pPeer.shutdown();
	}

	public P2PAddress getP2PAddress() {
		return new P2PAddress(tomP2pPeer.getPeerAddress());
	}

	/*
	 * private void startAnnouncingMyself() { final Number160 location = new Number160(new ShortString("announce")); this.tomP2pPeer.addMaintainance(new
	 * Runnable() {
	 * 
	 * @Override public void run() { try { AddressBean addressBean = new AddressBean(tomP2pPeer.getPeerAddress(), getMyId()); tomP2pPeer.addToTracker(location,
	 * "announce", new Data(addressBean)) .awaitUninterruptibly(); } catch (IOException e1) { // TODO Auto-generated catch block e1.printStackTrace(); }
	 * FutureTracker futureTracker = tomP2pPeer.getFromTracker(location, "announce", true); futureTracker.awaitUninterruptibly(); if (futureTracker.isSuccess())
	 * { for (Data data : futureTracker.getTrackers().values()) { try { AddressBean addressBean = (AddressBean) data.getObject(); System.err.println("found " +
	 * addressBean.getPeerId()); increaseReputation(addressBean.getPeerId(), Application .getApplication().getInitialHistory(
	 * addressBean.getPeerId().getName())); //tradeIncentives.addPeerForTrading(addressBean.getPeerAddress(), addressBean.getPeerId()); } catch (Exception e) {
	 * e.printStackTrace(); } } } } }); }
	 */
}