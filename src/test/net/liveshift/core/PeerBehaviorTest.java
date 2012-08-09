package net.liveshift.core;

import java.io.IOException;
import java.util.Date;

import junit.framework.Assert;

import net.liveshift.core.DeterministicPeerBehavior;
import net.liveshift.core.ProbabilisticPeerBehavior;
import net.liveshift.core.UniformPeerBehavior;

import org.junit.Test;

public class PeerBehaviorTest {
	@Test
	public void testProbabilisticPeerBehaviorBehavior() {
		ProbabilisticPeerBehavior peerBehavior = new ProbabilisticPeerBehavior();
		long t0 = new Date().getTime();
		System.out.println("started at: "+t0);
		for (int i=0; i<10000; i++) {
			long t1 = new Date().getTime();

			long ts = peerBehavior.getTimeShiftRange(t0, t1);
			
			System.out.println("got channel "+peerBehavior.getChannelNumber()+ " t1("+t1+") ts(" + ts +") and holding for "+ peerBehavior.getHoldingTimeS());
			
			Assert.assertTrue(ts>=t0);
			Assert.assertTrue(ts<=t1);
			if (!peerBehavior.next()) break;
		}
		long t1 = new Date().getTime();
		System.out.println("finished at: "+t1);
	}
	
	@Test
	public void testDeterministicPeerBehaviorBehavior() throws IOException, ClassNotFoundException {
		
		DeterministicPeerBehavior peerBehavior = new DeterministicPeerBehavior(1);
		
		long t0 = new Date().getTime();
		System.out.println("started at: "+t0);
		
		long t1 = new Date().getTime();
		
		for (int i=0; i<10000; i++) {
			
			long ts = peerBehavior.getTimeShiftRange(t0, t1);
			int ht = peerBehavior.getHoldingTimeS();
			int channel=peerBehavior.getChannelNumber();
			System.out.println("got channel "+channel+ " t1("+t1+") ts(" + ts +") ["+(t1-ts)+"] and holding for "+ ht+" s");
			
			Assert.assertTrue(ts>=t0);
			Assert.assertTrue(ts<=t1);
			Assert.assertTrue(channel<=6);
			Assert.assertTrue(channel>=0);
			
			t1+=(ht*1000);
			if (!peerBehavior.next()) break;
		}
		t1 = new Date().getTime();
		System.out.println("finished at: "+t1);
	}
	
	@Test
	public void testUniformPeerBehaviorBehavior() throws IOException, ClassNotFoundException {
		
		UniformPeerBehavior peerBehavior = new UniformPeerBehavior();
		
		long t0 = new Date().getTime();
		System.out.println("started at: "+t0);
		
		long t1 = new Date().getTime();

		for (int i=0; i<10000; i++) {

			long ts = peerBehavior.getTimeShiftRange(t0, t1);
			int ht = peerBehavior.getHoldingTimeS();
			int channel=peerBehavior.getChannelNumber();
			System.out.println("got channel "+channel+ " t1("+t1+") ts(" + ts +") ["+(t1-ts)+"] and holding for "+ ht+" s");
			
			Assert.assertTrue(ts>=t0);
			Assert.assertTrue(ts<=t1);
			Assert.assertTrue(channel<=6);
			Assert.assertTrue(channel>=0);
			
			t1+=(ht*1000);
			if (!peerBehavior.next()) break;
		}
		t1 = new Date().getTime();
		System.out.println("finished at: "+t1);
	}
	
}
