package net.liveshift.signaling;

import java.util.Date;
import java.util.logging.LogManager;

import junit.framework.Assert;
import net.liveshift.core.PeerId;
import net.liveshift.p2p.Peer;
import net.liveshift.signaling.MessageFlowControl;
import net.liveshift.signaling.MessageFlowControl.FlowControlResponse;
import net.liveshift.signaling.messaging.*;
import net.liveshift.util.TimeOutHashMap;

import org.junit.Test;

import net.liveshift.dummy.DummyPeerId;


public class MessageFlowControlTest {
	
	// Init LogManager
	static {
		try {
			LogManager.getLogManager().readConfiguration(Peer.class.getResourceAsStream("/jdklogtest.properties"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	MessageFlowControl mfc = new MessageFlowControl();
	
	@Test
	public void testMessageFlowControl0() throws Exception {
		
		PeerId myPeerId = new DummyPeerId(1);
		
		System.out.println("in order should be OK");

		for (int i=1; i<1000; i++) {
			final AbstractMessage m = new PingMessage(myPeerId, myPeerId, true);
			System.out.println(m);
			Assert.assertEquals(FlowControlResponse.ACCEPTED, mfc.acceptMessage(m, new Date().getTime()));
			Thread.sleep((long) (Math.random()*1000));
		}
	}
	@Test
	public void testMessageFlowControl() throws Exception {
		PeerId myPeerId = new DummyPeerId(1);
		
		System.out.println("in order should be OK");
		final AbstractMessage[] m = new AbstractMessage[5];
		m[0] = new PingMessage(myPeerId, myPeerId, true);
		m[1] = new PingMessage(myPeerId, myPeerId, true);
		m[2] = new PingMessage(myPeerId, myPeerId, true);
		m[3] = new PingMessage(myPeerId, myPeerId, true);
		m[4] = new PingMessage(myPeerId, myPeerId, true);
		
		Assert.assertEquals(FlowControlResponse.ACCEPTED, mfc.acceptMessage(m[0], new Date().getTime()));
		Assert.assertEquals(FlowControlResponse.ACCEPTED, mfc.acceptMessage(m[1], new Date().getTime()));
		Assert.assertEquals(FlowControlResponse.ACCEPTED, mfc.acceptMessage(m[2], new Date().getTime()));
		Assert.assertEquals(FlowControlResponse.WAIT, mfc.acceptMessage(m[4], new Date().getTime()));
		Assert.assertEquals(FlowControlResponse.REJECTED, mfc.acceptMessage(m[2], new Date().getTime()));
		
		Thread.sleep(5100);  //resets

		System.out.println("with one in gap");
		
		Assert.assertTrue(mfc.acceptMessage(m[0], new Date().getTime()).equals(FlowControlResponse.ACCEPTED));
		
		Runnable r0 = new Runnable() {
			@Override
			public void run() {
				Assert.assertTrue(mfc.acceptMessage(m[1], new Date().getTime()).equals(FlowControlResponse.ACCEPTED));
			}
		};
		
		Runnable r1 = new Runnable() {
			@Override
			public void run() {
				Assert.assertTrue(mfc.acceptMessage(m[2], new Date().getTime()).equals(FlowControlResponse.ACCEPTED));
			}
		};
		
		Thread t0 = new Thread(r0);
		Thread t1 = new Thread(r1);
		
		t1.start();
		Thread.sleep(30);
		t0.start();
		
		Assert.assertFalse(mfc.acceptMessage(m[0], new Date().getTime()).equals(FlowControlResponse.ACCEPTED));
		
		Thread.sleep(5100);  //resets

		System.out.println("with two in gap");

		Assert.assertTrue(mfc.acceptMessage(m[0], new Date().getTime()).equals(FlowControlResponse.ACCEPTED));
		
		Runnable r2 = new Runnable() {
			@Override
			public void run() {
				Assert.assertTrue(mfc.acceptMessage(m[3], new Date().getTime()).equals(FlowControlResponse.ACCEPTED));
			}
		};
		
		Thread t2 = new Thread(r2);
		t0 = new Thread(r0);
		t1 = new Thread(r1);

		t2.start();
		Thread.sleep(15);
		t0.start();
		Thread.sleep(15);
		t1.start();
		
		Thread.sleep(5100);  //resets

		System.out.println("with two in gap, different order");

		Assert.assertTrue(mfc.acceptMessage(m[0], new Date().getTime()).equals(FlowControlResponse.ACCEPTED));
		
		t1 = new Thread(r1);
		t2 = new Thread(r2);
		t0 = new Thread(r0);

		t1.start();
		Thread.sleep(7);
		t2.start();
		Thread.sleep(74);
		t0.start();
		
		Thread.sleep(5100);  //resets

		System.out.println("with gap in border");
		
		while (m[0].getMessageId()!=126)
			m[0] = new PingMessage(myPeerId, myPeerId, true);
		m[1] = new PingMessage(myPeerId, myPeerId, true);
		m[2] = new PingMessage(myPeerId, myPeerId, true);
		
		Assert.assertTrue(mfc.acceptMessage(m[0], new Date().getTime()).equals(FlowControlResponse.ACCEPTED));
		Assert.assertFalse(mfc.acceptMessage(m[0], new Date().getTime()).equals(FlowControlResponse.ACCEPTED));

		t0 = new Thread(r0);
		t1 = new Thread(r1);
		
		t1.start();
		Assert.assertFalse(mfc.acceptMessage(m[0], new Date().getTime()).equals(FlowControlResponse.ACCEPTED));
		Thread.sleep(30);
		Assert.assertFalse(mfc.acceptMessage(m[0], new Date().getTime()).equals(FlowControlResponse.ACCEPTED));
		t0.start();
		Assert.assertFalse(mfc.acceptMessage(m[0], new Date().getTime()).equals(FlowControlResponse.ACCEPTED));
		
		Thread.sleep(1000);

		Assert.assertFalse(mfc.acceptMessage(m[0], new Date().getTime()).equals(FlowControlResponse.ACCEPTED));
		Assert.assertFalse(mfc.acceptMessage(m[2], new Date().getTime()).equals(FlowControlResponse.ACCEPTED));
		Assert.assertFalse(mfc.acceptMessage(m[1], new Date().getTime()).equals(FlowControlResponse.ACCEPTED));

	}

	@Test
	public void testHashTimeoutMap() throws Exception {
		TimeOutHashMap<PeerId, Integer> htm = new TimeOutHashMap<PeerId, Integer>(1000);
		
		PeerId pid = new DummyPeerId(1);
		htm.put(pid, 666);
		
		Assert.assertEquals((Integer)666, htm.get(pid));
		Assert.assertEquals((Integer)666, htm.get(pid));
		Thread.sleep(1010);
		Assert.assertEquals(null, htm.get(pid));
	}
}
