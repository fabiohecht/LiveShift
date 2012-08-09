package net.liveshift.p2p;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import junit.framework.Assert;

import net.liveshift.core.Channel;
import net.liveshift.core.PeerId;
import net.liveshift.p2p.Peer;
import net.liveshift.storage.SegmentIdentifier;
import net.tomp2p.futures.BaseFutureAdapter;
import net.tomp2p.futures.FutureTracker;
import net.tomp2p.storage.TrackerData;

import org.junit.Test;


/**
 * Test class for the P2P overlay used by LiveShift
 * 
 * @author Thomas Bocek
 * @author Kevin Leopold
 * 
 */
public class TestP2P
{
	private final static Random rnd = new Random(1L);
	/**
	 * Tests bootstrapping
	 * 
	 * @throws Exception
	 */
	@Test
	public void testBootstrap() throws Exception
	{
		final int numPeers=20;

		System.out.println("testBootstrap");
		Peer[] peers = bootstrapAndPublishChannels(0,numPeers, null);
		for (int i = 0; i < peers.length; i++)
			peers[i].shutdown();
	}



	/**
	 * Tests publishing channels and receiving the channel list
	 * 
	 * @throws Exception
	 */
	@Test
	public void testChannelList() throws Exception
	{
		final int numPeers=20;

		System.out.println("testChannelList");
		Peer[] peers = bootstrapAndPublishChannels(0,numPeers, this.fillChannels());
		for (int i = 0; i < peers.length; i++)
			peers[i].shutdown();
	}
	
	/**
	 * tests writing to and reading from distributed tracker
	 * 
	 * @throws Exception
	 */
	@Test
	public void testTracker() throws Exception
	{
		final int numPeers=20;
		final int numProviders=5;
		System.out.println("testTracker");
		Channel[] channels = this.fillChannels();
		Peer[] peers = bootstrapAndPublishChannels(0, numPeers, channels);
		
		final SegmentIdentifier si = new SegmentIdentifier(channels[0], (byte) 0, TestP2P.rnd.nextInt());
		
		//random peers write to tracker
		Set<Integer> providers = new HashSet<Integer>(numPeers);
		for (int i=0; i<numProviders; i++) {
			int provider = -1;
			while (provider == -1 || providers.contains(provider))
				provider = TestP2P.rnd.nextInt(numPeers);
			providers.add(provider);
			
			System.out.println("provider "+provider+" announces");
			peers[provider].publishSegment(si);
		}
		
		//all peers read from tracker: how much should they get? I'll see how much they get
		for (int i=0; i<numPeers; i++) {
			final int i2=i;
			FutureTracker ft = peers[i].getPeerList(si, numProviders);
			ft.addListener(new BaseFutureAdapter<FutureTracker>() {

				@Override
				public void operationComplete(FutureTracker future) throws Exception {
					
					if (future.isSuccess()) {
						Set<PeerId> result = fillPeerID(si, numProviders, future.getTrackers());
						
						System.out.println("got "+result.size()+" out of "+ numProviders);
						try {
							Assert.assertEquals(numProviders, result.size());
						}
						catch (AssertionError ae) {
							System.out.println("p"+i2+" got only "+result.size()+" out of "+numProviders +" :(");	
						}
						
					} else {
						System.out.println("error in searchCand: " + future.getFailedReason());
					}
				}
			});
		}
		
		for (int i = 0; i < peers.length; i++)
			peers[i].shutdown();
	}


	/**
	 * Creates some channels
	 */
	private Channel[] fillChannels()
	{
		Channel[] channels = new Channel[5];
		channels[0] = new Channel("C1", (byte) 1, 1);
		channels[0].setDescription("ereeqwrqwerqwer");
		channels[1] = new Channel("C2", (byte) 1, 2);
		channels[1].setDescription("dfsdasfsafsdafasfd");
		channels[2] = new Channel("C3", (byte) 1, 3);
		channels[2].setDescription("sdfasdfsafdasdfa");
		channels[3] = new Channel("C4", (byte) 1, 4);
		channels[3].setDescription("fdsafadsfsadfasfasf");
		channels[4] = new Channel("C5", (byte) 1, 5);
		channels[4].setDescription("asdfasdfasdfasdf");
		channels[4] = new Channel("C6", (byte) 1, 6);
		channels[4].setDescription("qwerqwerqwerqwer");
		return channels;
	}

	/**
	 * Does the actual bootstrapping
	 * 
	 * @param shift the port to shift from the standard port (to avoid using the
	 *        same port twice)
	 * @throws Exception
	 */
	private Peer[] bootstrapAndPublishChannels(int shift, int nr, final Channel[] channels) throws Exception
	{
		boolean success;

		Peer[] peers = new Peer[nr];
		InetSocketAddress bootstrapSocket = new InetSocketAddress(InetAddress.getLocalHost(), 2424 + shift);
		peers[0] = new Peer();
		success = peers[0].connect(bootstrapSocket, bootstrapSocket, "lo", "peer0", false);
		
		try {
			Assert.assertEquals(true, success);
		}
		catch (AssertionError ae) {
			System.out.println("how to check if this was successful?");
		}
		
		for (int i = 1; i < peers.length; i++)
		{
			System.out.println("bootstrap peer "+i);
			
			peers[i] = new Peer();
			InetSocketAddress localSocket = new InetSocketAddress(InetAddress.getLocalHost(), 2424 + shift + i);
			success = peers[i].connect(localSocket, bootstrapSocket, "lo", "peer"+i, false);
			Assert.assertEquals(true, success);
			
			//publishes a channel, should work
			if (channels!=null && channels.length >= i && channels[i-1]!=null) {
				System.out.println("publish channel "+(i-1));

				success = peers[i].publishChannel(channels[i-1]);
				Assert.assertEquals(true, success);
			}
			
			//fetches all published channels, should get all
			if (channels!=null) {
				
				final Peer peerToCheck = peers[i];
				final int iFinal = i;
				
				Runnable channelFetcher = new Runnable() {
					@Override
					public void run() {
						boolean success=false;
						int tries=1;
						while (tries<=5 && !success) {
							
							Set<Channel> retrievedChannels=null;
							try {
								retrievedChannels = peerToCheck.getChannelSet();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (ClassNotFoundException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							System.out.println("peer "+peerToCheck.getMyId().getName()+" got "+retrievedChannels.size()+" channels");
			
							success=true;
							for (int j=0; j<iFinal && j<channels.length; j++) {
								System.out.println("peer "+peerToCheck.getMyId().getName()+" check channel "+j);
								
								try {
									Assert.assertEquals(true, retrievedChannels.contains(channels[j]));
								}
								catch (AssertionError ae) {
									System.out.println("Failed "+tries+" times, peer "+peerToCheck.getMyId().getName()+" didn't get channel "+channels[j].getName());
									
									success=false;
								}
							}
							
							tries++;

							if (!success) {
								try {
									Thread.sleep(1000);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						}

						if (!success) {
							System.out.println("Failed "+tries+" times, peer "+peerToCheck.getMyId().getName()+" didn't get all channels!!!!!!");
							Assert.assertTrue("Failed "+tries+" times, peer "+peerToCheck.getMyId().getName()+" didn't get all channels!!!!!!", false);
						}
						else {
							System.out.println("peer "+peerToCheck.getMyId().getName()+" found all channels.");
						}
/*
					try {
						Assert.assertEquals(true, publishedChannels.contains(channels[j]));
					}
					catch (AssertionError ae) {
						System.out.println("publish channel "+j+" not found!");
					}
*/
					
					}
					
				};
				
				Thread t= new Thread(channelFetcher);
				t.run();
				
			}
			//Thread.sleep(1000+i*100);
			Thread.sleep(100);
		}
		return peers;
	}
	

	//TODO move this to Peer.java (since it deals with TrackerData)
	private Set<PeerId> fillPeerID(final SegmentIdentifier segmentIdentifier, final int howMany, final Collection<TrackerData> collection) throws ClassNotFoundException, IOException {
		
		if (collection.size()==0)
			return null;
		
		Set<PeerId> incomingPeers = new HashSet<PeerId>(collection.size() - 1);

		for (Iterator<TrackerData> iter = collection.iterator(); iter.hasNext();) {
			TrackerData trackerData = iter.next();
			byte[] attachment = trackerData.getAttachement();
			if (attachment!=null) {
				
				String candidateName = new String(attachment); 
				
				PeerId pid = new PeerId(trackerData.getPeerAddress(),candidateName);
				
				incomingPeers.add(pid);
				
			}
		}
		
		int usefulPeersCount = incomingPeers.size();

		if (howMany >= usefulPeersCount)
			return incomingPeers;

		int resultSize = 0;
		Set<PeerId> result = new HashSet<PeerId>();
		
		for (Iterator<PeerId> iter = incomingPeers.iterator(); iter.hasNext() && resultSize < howMany;) {
			PeerId peerId = iter.next();

			if (TestP2P.rnd.nextFloat() < (float)howMany/usefulPeersCount) {
				result.add(peerId);
				resultSize++;
			}
			usefulPeersCount--;
			
		}
		return result;
	}

}
