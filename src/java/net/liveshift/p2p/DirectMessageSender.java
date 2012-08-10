package net.liveshift.p2p;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import net.liveshift.core.PeerId;
import net.liveshift.signaling.IncomingMessageHandler;
import net.liveshift.signaling.MessageListener;
import net.liveshift.signaling.MessageSender;
import net.liveshift.signaling.messaging.AbstractMessage;
import net.liveshift.time.Clock;
import net.liveshift.util.ExecutorPool;
import net.liveshift.util.MovingAverage;
import net.liveshift.util.Utils;
import net.tomp2p.peers.PeerStatusListener.Reason;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/*
 * 
 * still TODO: 
 *  - Disconnect message should probably really break connection. but the sender could do it as well.
 *  - since outgoing queues are assigned to connections, when a connection is broken, messages are not sent and cause delays due to flow control
 */

public class DirectMessageSender  implements MessageSender, MessageListener {

	private IncomingMessageHandler incomingMessageHandler;
	private PeerStatusNotifier peerStatusNotifier;
	private ServerSocket serverSocket;
	
	final private PeerId myPeerId;
	final private Map<PeerId, ConnectedPeer> connectedPeers = new HashMap<PeerId, ConnectedPeer>();
	private final MovingAverage sentMessagesAverage = new MovingAverage(Clock.getMainClock(),5,1000);
	final private Map<PeerMessageId, MessageToSend> replyLocks = new ConcurrentHashMap<PeerMessageId, MessageToSend>(); 
	private boolean listening;
	
	final private static Logger logger = LoggerFactory.getLogger(DirectMessageSender.class);
	
	private static final int RETRIES_SEND_MESSAGE = 4;
	private static final long CONNECTION_INACTIVITY_TIMEOUT_MILLIS = 10000;  //checked every 10 seconds, so potentially this + 10s
	private static final long REPLY_WAIT_TIMEOUT_MILLIS = 5000;

	private class MessageToSend {
		public MessageToSend(AbstractMessage message) {
			this.message=message;
		}
		final AbstractMessage message;
		int countDown = RETRIES_SEND_MESSAGE;
		final long submissionTime = Clock.getMainClock().getTimeInMillis(false);
		boolean replyReceived = false;
		
		@Override
		public String toString() {
			return message.toString()+" st:"+this.submissionTime+" cd:"+this.countDown;
		}
	}
	
	private class PeerMessageId {

		final private byte messageId;
		final private PeerId peerId;

		public PeerMessageId(byte messageId, PeerId peerIdReceiver) {
			this.messageId=messageId;
			this.peerId = peerIdReceiver;
		}
		
		@Override
		public int hashCode() {
			return this.messageId ^ this.peerId.hashCode();
		}
		
		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof PeerMessageId))
				return false;
			PeerMessageId o = (PeerMessageId) obj;
			return this.messageId==o.messageId && this.peerId.equals(o.peerId);
		}
		
		@Override
		public String toString() {
			return this.peerId.getName()+"/"+this.messageId;
		}
	}

	private class ConnectedPeer {
		
		final private PeerId peerId;
		final private Socket socket;
		private DataOutputStream outputStream;
		private DataInputStream inputStream;
		private PeerMessageSender peerMessageSender;
		private PeerMessageProcessor peerMessageProcessor;
		final private BlockingQueue<MessageToSend> queue = new LinkedBlockingQueue<MessageToSend>();
		private ScheduledFuture<?> maintenanceTask;

		final AtomicBoolean running = new AtomicBoolean(true);
		long lastActivity;
		final private int id;
		

		private class PeerMessageSender extends Thread {
			
			@Override
			public void run() {
				try {
					if (logger.isDebugEnabled()) {
						logger.debug("started");
					}
					while (running.get()) {
						this.processQueue();
					}
				}
				catch (InterruptedException e) {
					if (logger.isDebugEnabled()) {
						logger.debug("interrupted");
					}
				}
				catch (Exception e) {
					e.printStackTrace();
					logger.warn("error processing queue: "+e.getMessage());
				}

				if (running.get()) {
					if (logger.isDebugEnabled()) {
						logger.debug("sender thread died, shutting down connection");
					}
					shutdown();
				}
			}

			private void processQueue() throws InterruptedException {
				
				MessageToSend messageToSend = queue.take();
				
				lastActivity = Clock.getMainClock().getTimeInMillis(false);
				
				AbstractMessage liveshiftMessage = messageToSend.message;
				byte[] message = liveshiftMessage.toByteArray();
				
				logger.info("sending message "+liveshiftMessage +" to "+peerId.getName()+" countDown="+messageToSend.countDown+" size="+message.length);

				boolean success = false;
				try {
					
					sendMessage(message);
					
					success = true;
					
				}
				catch (IOException e) {
					logger.warn("I/O Exception sending message to "+peerId);
					success=false;
				}			
				
				if (!success) {
					
					if (messageToSend.countDown <= 1) {
						logger.warn("giving up resending message:"+liveshiftMessage+" to:"+peerId);
						
						if (peerStatusNotifier!=null)
							peerStatusNotifier.peerOffline(peerId.getDhtId().getPeerAddress(), Reason.NOT_REACHABLE);
						

						//TODO have some threshold and disconnect if failed too often
						
						
					}
					else {
						logger.warn("scheduling message for resending later:"+liveshiftMessage+" to:"+peerId);

						//TODO not retry immediately, wait a little while
						
						messageToSend.countDown-=1;

						queue.offer(messageToSend);
					}
					
				}
				else {
				
					if (logger.isDebugEnabled()) {
						logger.debug("done sending message "+liveshiftMessage +" to "+peerId.getName()+" countDown="+messageToSend.countDown+" size="+message.length);
					}
					
				}
				
			}
			
			
		};
		
		private class PeerMessageProcessor extends Thread {
		
			@Override
			public void run() {
				try {
					if (logger.isDebugEnabled()) {
						logger.debug("started");
					}
					
					this.receiveMessages();
				}
				catch (IOException e) {
					logger.warn("IOException in PeerMessageProcessor for "+peerId);
					//e.printStackTrace();
				}

				if (running.get()) {
					if (logger.isDebugEnabled()) {
						logger.debug("receiver thread died, shutting down connection");
					}
					shutdown();
				}
			}

			private void receiveMessages() throws IOException {

				//receives, handles messages
				while (isAlive() && running.get()) {

				    lastActivity = Clock.getMainClock().getTimeInMillis(false);
	    
				    final byte[] buf2 = readMessage();
				    
				    if (logger.isDebugEnabled()) {
				    	logger.debug("received a message from "+peerId+" with size "+buf2.length);
				    }
				    
				    Runnable handleIncomingConnection = new Runnable() {
						
						@Override
						public void run() {
						    try {
								AbstractMessage incomingMessage = incomingMessageHandler.getMessage(peerId, buf2, 0);
								
								if (logger.isDebugEnabled()) {
									logger.debug("handling message "+incomingMessage);
								}
								
								if (incomingMessage.getMessageIdReply()!=null) {
									//message is a reply, maybe they are waiting for it									
									
									PeerMessageId peerMessageId = new PeerMessageId(incomingMessage.getMessageIdReply(), peerId);

									if (logger.isDebugEnabled()) {
										logger.debug("will look for lock "+peerMessageId.toString());
									}
									
									MessageToSend requestMessage = replyLocks.get(peerMessageId);
									if (requestMessage!=null) {
										replyLocks.remove(peerMessageId);
										
										synchronized (requestMessage) {
											if (logger.isDebugEnabled()) {
												logger.debug("in synchronized (requestMessage)");
											}

											requestMessage.replyReceived=true;
											requestMessage.notify();
										}
									}
									else {
										//no lock but it's a reply to something. happens when it's a reply to a message sent without blocking until the reply comes, it's normal.
										if (logger.isDebugEnabled()) {
											String debug="";
											for (PeerMessageId messageWaiting : replyLocks.keySet()) {
												debug+= " : "+messageWaiting.toString();
											}
											logger.debug("can't find reply lock -- maybe we are not really waiting for it. we are waiting for"+debug);
										}
										
									}
								
								}
								
								AbstractMessage reply = incomingMessageHandler.handleIncomingMessage(incomingMessage);
								
								if (reply!=null) {
									MessageToSend replyToSend = new MessageToSend(reply);
									
									while (replyToSend.countDown>0 && !queueMessage(replyToSend)) {
										replyToSend.countDown--;
										Thread.sleep(100);
									}
								}
								if (logger.isDebugEnabled()) {
									logger.debug("done handling message");
								}
							}
							catch (Exception e) {
								logger.warn("exception handling message: "+e.getLocalizedMessage());
								e.printStackTrace();
							}

						}
					};
					
					ExecutorPool.getGeneralExecutorService().execute(handleIncomingConnection);
				}		
			
			}

		}
		
		class MaintenanceRunner implements Runnable {

			@Override
			public void run() {
				//kills dead connections
				try {
					long timeNow = Clock.getMainClock().getTimeInMillis(false);
					
					if (!isAlive() || timeNow-lastActivity > CONNECTION_INACTIVITY_TIMEOUT_MILLIS) {
						
						if (logger.isDebugEnabled()) {
							logger.debug("shutting down due to death ("+!isAlive()+") or inactivity ("+(timeNow-lastActivity > CONNECTION_INACTIVITY_TIMEOUT_MILLIS)+" by "+(timeNow-lastActivity)+"ms)");
						}
						shutdown();
						removeConnectedPeer(peerId, socket);
					}
				}
				catch (Exception e) {
					logger.error("error running connection maintenance thread: "+e.getMessage());
					e.printStackTrace();
				}
			}
		}
		

		private byte[] readMessage() throws IOException {

			int len = inputStream.readInt();
		    
			byte[] buf = new byte[len];
		    int pos=0;
		    while (len>pos) {
		    	int read = inputStream.read(buf,pos,len-pos);
		    	if (read==-1) {
		    		logger.warn("reached EOF from "+peerId);
		    		break;
		    	}
		    	pos+=read;
		    }
		    
		    return buf;
		}
		
		/**
		 * to be used when connection is outgoing
		 * 
		 * @param peerId
		 * @throws IOException 
		 */
		ConnectedPeer(final PeerId peerId) throws IOException {
			this.peerId=peerId;
			lastActivity = Clock.getMainClock().getTimeInMillis(false);
			this.id = Utils.getRandomInt();
			
			if (logger.isDebugEnabled()) {
				logger.debug("will connect a new socket to "+peerId.getName()+" @"+peerId.getInetAddress()+" port "+peerId.getSignalingPort()+" connnection id="+Integer.toHexString(this.id));
			}
			
			this.socket = new Socket(peerId.getInetAddress(), peerId.getSignalingPort());
			
			createStreams();
			
			//sends peer id (identifies itself)
			byte[] peerIdSerial = myPeerId.toByteArray();
			outputStream.writeInt(peerIdSerial.length);
			outputStream.write(peerIdSerial);
			
			//sends a random connection identifier (for solving conflicts)
			outputStream.writeInt(this.id);
			
			outputStream.flush();

		    this.createThreads();
		}
		
		/**
		 * to be used when connection is incoming
		 * 
		 * @param socket
		 * @throws IOException 
		 */
		public ConnectedPeer(Socket socket) throws IOException {
			
			this.socket = socket;
			lastActivity = Clock.getMainClock().getTimeInMillis(false);
		    
			createStreams();

		    //reads peerId
		    byte[] buf = readMessage();
		    this.peerId = new PeerId(buf, 0, 20);

			//reads id
			this.id = inputStream.readInt();
			
			if (logger.isDebugEnabled()) {
				logger.debug("new connection to "+this.peerId+" connnection id="+Integer.toHexString(this.id));
			}
			
		    this.createThreads();
		    
		}
		
		private void createStreams() throws IOException {
		    InputStream rawIn = socket.getInputStream();
		    BufferedInputStream buffIn = new BufferedInputStream(rawIn);
		    inputStream = new DataInputStream(buffIn);

			OutputStream rawOut = socket.getOutputStream();
			BufferedOutputStream buffOut = new BufferedOutputStream (rawOut);
			outputStream = new DataOutputStream (buffOut);
		}
		
		/**
		 * create threads to send (from queue) and receive messages
		 * 
		 */
		private void createThreads() {
			
			this.peerMessageSender = new PeerMessageSender();
			this.peerMessageSender.setName("MsgSender-"+myPeerId+">"+peerId+"-"+Integer.toHexString(this.id));
			this.peerMessageSender.start();
			
			this.peerMessageProcessor = new PeerMessageProcessor();
			this.peerMessageProcessor.setName("MsgProc-"+myPeerId+"<"+peerId+"-"+Integer.toHexString(this.id));
			this.peerMessageProcessor.start();
			
			this.maintenanceTask = ExecutorPool.getScheduledExecutorService().scheduleAtFixedRate(new MaintenanceRunner(), 10, 10, TimeUnit.SECONDS);
			
		}
		
		public void sendMessage(byte[] message) throws IOException {
			if (logger.isDebugEnabled()) {
				logger.debug("sending message thru wire, size "+message.length+", socket="+socket);
			}
			
			sentMessagesAverage.inputValue(1);

			outputStream.writeInt(message.length);
			outputStream.write(message);
			
			outputStream.flush();
		}

		boolean isAlive() {
			return this.running.get() && socket!=null && socket.isConnected() && this.outputStream!=null && this.inputStream!=null;
		}
		
		void shutdown() {
		
			if (this.running.compareAndSet(true,false)) {
				
				try {
					if (logger.isDebugEnabled()) {
						StringBuilder sb = new StringBuilder("shutdown");
						if (!this.queue.isEmpty()) {
							sb.append(", queue still contained");
							for (MessageToSend message : this.queue) {
								sb.append(" : ").append(message.toString());
							}
						}
						logger.debug(sb.toString());
					}
					
					this.maintenanceTask.cancel(true);
					this.peerMessageSender.interrupt();
					this.peerMessageProcessor.interrupt();
	
					try {
						inputStream.close();
						outputStream.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					finally {
						try {
							socket.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				finally {
					//gets queue content, reschedules it
					Collection<MessageToSend> queueElements = new LinkedList<MessageToSend>();
					this.queue.drainTo(queueElements);
					
					try {
						rescheduleQueue(queueElements, this.peerId);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			else {
				if (logger.isDebugEnabled()) {
					logger.debug("was already shutting down");
				}
			}
		}

		public boolean queueMessage(MessageToSend message) {
			
			if (logger.isDebugEnabled()) {
				logger.debug("queuing message "+message);
			}
			
			return this.queue.offer(message);
		}
		
	}
	
	public DirectMessageSender(PeerId myPeerId) {
		this.myPeerId=myPeerId;
	}
	
	public void rescheduleQueue(Collection<MessageToSend> queueElements, PeerId peerIdReceiver) throws InterruptedException {
		for (MessageToSend message : queueElements) {
			
			if (logger.isDebugEnabled()) {
				logger.debug("rescheduling message that was left in queue: "+message);
			}
			
			message.countDown--;
			this.queueMessage(message, peerIdReceiver);
		}
	}

	@Override
	public boolean sendMessage(final AbstractMessage message, final PeerId peerIdReceiver, final boolean blockUntilReply) {
		
		MessageToSend messageToSend = new MessageToSend(message);
		
		boolean success = false;
		
		synchronized (messageToSend) {
			
			if (logger.isDebugEnabled()) {
				logger.debug("in synchronized (messageToSend="+messageToSend.toString()+")");
			}
			
			try {
				success = this.queueMessage(messageToSend, peerIdReceiver);
			}
			catch (InterruptedException e) {
				logger.warn("interrupted");
				success=false;
			}
			
			if (success && blockUntilReply) {
				
				PeerMessageId peerMessageId = new PeerMessageId(message.getMessageId(), peerIdReceiver);

				this.replyLocks.put(peerMessageId, messageToSend);
				
				if (logger.isDebugEnabled()) {
					logger.debug("will put lock "+peerMessageId.toString()+" for "+message);
				}
				
				try {
					while (!messageToSend.replyReceived && Clock.getMainClock().getTimeInMillis(false)-messageToSend.submissionTime < REPLY_WAIT_TIMEOUT_MILLIS) {
						messageToSend.wait(REPLY_WAIT_TIMEOUT_MILLIS);
					}
					
					if (!messageToSend.replyReceived) {
						logger.warn("reply not received as expected! TIMEOUT on "+message);
						return false;
					}
					
				} catch (InterruptedException e) {
					if (logger.isDebugEnabled()) {
						logger.debug("interrupted when waiting for reply on "+message);
					}
					
					return false;
				}
			}
		
		}
		
		return success;
	}
	
	private boolean queueMessage(MessageToSend message, PeerId peerIdReceiver) throws InterruptedException {
		boolean success = false;
		while (!success && message.countDown>=0) {
			try {
				
				ConnectedPeer connectedPeer=this.getConnectedPeer(peerIdReceiver);
				
				success = connectedPeer.queueMessage(message);
				
			}
			catch (ConnectException e) {
				logger.warn("ConnectException while queueing message: "+e.getMessage());
				success=false;
				e.printStackTrace();
			}
			catch (IOException e) {
				logger.warn("IOException while queueing message: "+e.getMessage());
				success=false;
				e.printStackTrace();
			}
			
			if (!success) {
				if (message.countDown==0) {
					logger.warn("IOException when queuing message, gave up retrying");

					return false;
				}
				else {
					logger.warn("problem queuing message, will retry");
					
					Thread.sleep(50);

					message.countDown--;
					
					return this.queueMessage(message, peerIdReceiver);
				}

			}
		}
		return success;
	}

	private ConnectedPeer getConnectedPeer(PeerId peerIdReceiver) throws IOException {
		
		synchronized (this.connectedPeers) {
		
			ConnectedPeer connectedPeer = this.connectedPeers.get(peerIdReceiver);
			
			if (connectedPeer!=null && !connectedPeer.isAlive()) {
				
				if (logger.isDebugEnabled()) {
					logger.debug("existing connection is dead, shutting it down");
				}
				
				connectedPeer.shutdown();
				this.connectedPeers.remove(peerIdReceiver);
				connectedPeer=null;
			}
			if (connectedPeer==null) {
				
				//creates new connection
				connectedPeer = new ConnectedPeer(peerIdReceiver);
				
				this.connectedPeers.put(peerIdReceiver, connectedPeer);
			}
			return connectedPeer;
		}
	}

	@Override
	public void registerIncomingMessageHandler(IncomingMessageHandler incomingMessageHandler) {
		this.incomingMessageHandler=incomingMessageHandler;
	}
	
	@Override
	public void startListening() throws Exception {

		try {
			if (logger.isDebugEnabled()) {
				logger.debug(this.myPeerId+" will listen on port "+this.myPeerId.getSignalingPort());
			}
			
		    serverSocket = new ServerSocket(this.myPeerId.getSignalingPort());
		    
		    this.listening = true;
		    
		    Runnable listenForIncomingConnections = new Runnable() {
				
				@Override
				public void run() {
					try {
						while (listening) {
							
							ConnectedPeer connectedPeer = null;
							
							try {
								final Socket socket = serverSocket.accept();
		
								if (logger.isDebugEnabled()) {
									logger.debug("received a new connection from "+socket.getRemoteSocketAddress());
								}
								
								connectedPeer = new ConnectedPeer(socket);								
								
							}
							catch (IOException e) {
								if (logger.isDebugEnabled()) {
									logger.debug("socket disconnected/IOException");
								}
								//e.printStackTrace();
								connectedPeer = null;
							}
							
							if (connectedPeer!=null) {
								addConnectedPeer(connectedPeer);
							}
						}						
					}
					catch (Exception e) {
						logger.error("exception occurred: "+e.getMessage());

						e.printStackTrace();
					}
				}
			};
			
			ExecutorPool.getGeneralExecutorService().execute(listenForIncomingConnections);

		} 
		catch (SocketException e) {
			if (logger.isDebugEnabled()) {
				logger.debug("socket closed, it may happen when peer disconnects");
			}
		}
		catch (Exception e) {
			logger.error("exception occurred: "+e.getMessage());

			e.printStackTrace();
		}
	}
	

	private void addConnectedPeer(ConnectedPeer connectedPeer) {
		synchronized (this.connectedPeers) {
			ConnectedPeer connectedFirst = this.connectedPeers.get(connectedPeer);
			if (connectedFirst!=null) {
				//received a connection, already has one
				if (connectedFirst.id < connectedPeer.id) {
					
					if (logger.isDebugEnabled()) {
						logger.debug("shutting down to resolve conflict: "+connectedFirst.id);
					}
					
					connectedFirst.shutdown();
					this.connectedPeers.put(connectedPeer.peerId, connectedPeer);
				}
				else {

					if (logger.isDebugEnabled()) {
						logger.debug("shutting down to resolve conflict: "+connectedPeer.id);
					}
					
					connectedPeer.shutdown();
				}
			}
			else {
				this.connectedPeers.put(connectedPeer.peerId, connectedPeer);
			}
		}
	}

	private void removeConnectedPeer(PeerId peerId, Socket socket) {
		synchronized (this.connectedPeers) {
			ConnectedPeer connectedPeer = this.connectedPeers.get(peerId);
			if (connectedPeer!=null && socket.equals(connectedPeer.socket)) {
				this.connectedPeers.remove(peerId);
			}
		}
	}
	
	@Override
	public void stopListening() {
		this.listening  = false;
		if (this.serverSocket!=null) {
			try {
				this.serverSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}

