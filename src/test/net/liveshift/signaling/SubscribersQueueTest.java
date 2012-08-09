package net.liveshift.signaling;


import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.LogManager;

import net.liveshift.core.Channel;
import net.liveshift.configuration.Configuration;
import net.liveshift.core.PeerId;
import net.liveshift.incentive.IncentiveMechanism;
import net.liveshift.incentive.IncentiveMechanism.IncentiveMechanismType;
import net.liveshift.signaling.VideoSignaling;
import net.liveshift.storage.Segment;
import net.liveshift.storage.SegmentIdentifier;
import net.liveshift.upload.Subscriber;
import net.liveshift.upload.SubscribersQueue;
import net.liveshift.upload.UploadSlotManager;

import org.junit.Test;

import net.liveshift.dummy.DummyPeerId;
import net.liveshift.dummy.DummyVideoSignaling;

public class SubscribersQueueTest {

	Random rnd = new Random();
	Channel channel = new Channel("c0", (byte)rnd.nextInt(), rnd.nextInt());
	IncentiveMechanism im;

	// Init LogManager
	static {
		try {
			LogManager.getLogManager().readConfiguration(SubscribersQueueTest.class.getResourceAsStream("/jdklogtest.properties"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testPreemption() throws SocketException, UnknownHostException {

		PeerId myPeerId = new DummyPeerId(0);
		
		Configuration conf = new Configuration();
		im = new IncentiveMechanism(null);
		im.setIncentiveMechanism(IncentiveMechanismType.SDUC);
		im.setSduc();
		VideoSignaling videoSignaling = new DummyVideoSignaling(myPeerId, im, conf);
		UploadSlotManager uploadSlotManager = new UploadSlotManager(videoSignaling, conf);
		
		SegmentIdentifier si = new SegmentIdentifier(channel, (byte)rnd.nextInt(), rnd.nextLong());
		Segment segment = new Segment(si, rnd.nextInt());
		AtomicBoolean peerLock = new AtomicBoolean(false);
		
		SubscribersQueue sq = new SubscribersQueue(3, true, videoSignaling);

		tryToAdd(si, segment, uploadSlotManager, peerLock, sq, 2);
		tryToAdd(si, segment, uploadSlotManager, peerLock, sq, 3);
		tryToAdd(si, segment, uploadSlotManager, peerLock, sq, 1);
		tryToAdd(si, segment, uploadSlotManager, peerLock, sq, 4);
		
	}
	private void tryToAdd(SegmentIdentifier si, Segment segment, UploadSlotManager uploadSlotManager, AtomicBoolean peerLock, SubscribersQueue sq, int peerNumber) throws UnknownHostException {
		PeerId peer = new DummyPeerId(peerNumber);
		
		im.setReputation(peer, peerNumber);
		Subscriber s = new Subscriber(peer, si, segment, uploadSlotManager, peerLock);
		System.out.println(sq.offer(s));
	}
}
