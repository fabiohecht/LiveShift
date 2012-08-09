package net.liveshift.upload;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.LogManager;

import junit.framework.Assert;

import net.liveshift.configuration.Configuration;
import net.liveshift.core.Channel;
import net.liveshift.core.DummyStats;
import net.liveshift.core.LiveShiftApplication;
import net.liveshift.core.PeerId;
import net.liveshift.download.BlockRequest;
import net.liveshift.download.Neighbor;
import net.liveshift.incentive.IncentiveMechanism;
import net.liveshift.incentive.IncentiveMechanism.IncentiveMechanismType;
import net.liveshift.p2p.Peer;
import net.liveshift.signaling.MessageFlowControl;
import net.liveshift.signaling.VideoSignaling;
import net.liveshift.signaling.messaging.BlockRequestMessage;
import net.liveshift.storage.Segment;
import net.liveshift.storage.SegmentIdentifier;
import net.liveshift.storage.SegmentStorage;
import net.liveshift.upload.BlockRequestPipeline;
import net.liveshift.upload.Subscriber;
import net.liveshift.upload.SubscribersQueue;
import net.liveshift.upload.UploadSlotManager;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.liveshift.dummy.DummySegmentStorage;
import net.liveshift.dummy.DummyVideoSignaling;
import net.liveshift.dummy.DummyPeerId;

/**
 * Tests the Resource Limiters
 *
 * @author Kevin Leopold
 *
 */
public class UploadSlotManagerTest {
	
	private static Logger logger = LoggerFactory.getLogger(UploadSlotManagerTest.class);
	
	// Init LogManager
	static {
		try {
			LogManager.getLogManager().readConfiguration(Peer.class.getResourceAsStream("/jdklogtest.properties"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	/**
	 * Test method for RequestLimiter
	 * @throws SocketException 
	 * @throws UnknownHostException 
	 */
	@Test
	public void testUploadSlotManager() throws SocketException, UnknownHostException {
		
		int peerCount = 20;
		int requestPerPeer= 30;
		
		//creates an application
		//Application app = Application.getApplication();
		//PeerId peerId = app.getDht().getMyId();
		Configuration conf = new Configuration();

		IncentiveMechanism dic = new IncentiveMechanism(null);
		dic.setRandom();
		
		PeerId myPeerId = new DummyPeerId(0);
		
		UploadSlotManager uploadSlotManager = new UploadSlotManager(new DummyVideoSignaling(myPeerId, dic, conf), conf);
		
		//creates some peers , makes peers request upload slots
		PeerId[] peerId = new PeerId[peerCount];
		SegmentStorage dss = new DummySegmentStorage(myPeerId);
		
		int countSuccess = 0, countError = 0;
		for (int i = 0; i < peerCount; i++) {
			peerId[i] = new DummyPeerId(i+1);

			for (int j = 0; j < requestPerPeer; j++) {
				
				SegmentIdentifier segmentIdentifier = new SegmentIdentifier(new Channel("testChannel",(byte)1, 0),(byte)0,j);
				Segment segment = new Segment(segmentIdentifier, 0);
				
				AtomicBoolean peerLock = new AtomicBoolean(false);
				Subscriber subscriber = new Subscriber(peerId[i], segmentIdentifier, segment, uploadSlotManager, peerLock);
				
				if (uploadSlotManager.addSubscriberGetTimeoutMillis(subscriber)>0)
					countSuccess++;
				else
					countError++;
				
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {}
			}
		}

		System.out.println("success:"+countSuccess+" error:"+countError);
		
	}
	
	@Test
	public void bandwidthAllocationTest() throws UnknownHostException, InterruptedException {
		PeerId myPeerId = new DummyPeerId(0);
		Configuration configuration = new Configuration();
		configuration.setUploadRate(250);
		IncentiveMechanism dic = new IncentiveMechanism(new DummyStats());
		dic.setIncentiveMechanism(IncentiveMechanismType.SDUC);
		dic.setSduc();

		VideoSignaling videoSignaling = new DummyVideoSignaling(myPeerId, dic, configuration);
		
		UploadSlotManager usm = new UploadSlotManager(videoSignaling, configuration);
		
		SegmentIdentifier segmentIdentifier = new SegmentIdentifier(new Channel("testChannel",(byte)1, 0),(byte)0, 1);
		Segment segment = new Segment(segmentIdentifier, 0);
		segment.getSegmentBlockMap().set(0, 599);
		PeerId requester = new DummyPeerId(1);
		AtomicBoolean peerLock = new AtomicBoolean(false);
		Subscriber subscriber = new Subscriber(requester, segmentIdentifier, segment, usm, peerLock);
		
		BlockRequestPipeline pipelineBlockRequest = new BlockRequestPipeline(new MessageFlowControl());
		usm.registerPipelineBlockRequest(pipelineBlockRequest);
		
		usm.startProcessing();
		
		usm.addSubscriberGetTimeoutMillis(subscriber);
		usm.setInterestedGetTimeout(requester, segmentIdentifier, true);
		Thread.sleep(20);
		
		for (int block=0; block<100; block++) {

			BlockRequest blockRequest = new BlockRequest(segmentIdentifier, block);

			System.out.println(">> requesting block #"+block);
			pipelineBlockRequest.offer(new BlockRequestMessage(blockRequest, requester, myPeerId));
			
			Thread.sleep(1000);
		}
		Thread.sleep(3000000);
	}
	
}
