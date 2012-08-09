package net.liveshift.util;

import net.liveshift.time.Clock;
import net.liveshift.util.MovingAverage;

import org.junit.Test;


public class MovingAverageTest {

	@Test
	public void testRecentAverage() throws InterruptedException {
		
		MovingAverage ra = new MovingAverage(new Clock(),10,1000);
		
		long step = 1000;
		
		for (int i=0; i<20; i++) {
			
			int input = 4;
			
			ra.inputValue(input);
			
			System.out.println((i*step)+"\tinput "+input+"\tavg "+ra.getAverage()+"\t(2)"+ra.getAverage(2)+"\t(1,1)"+ra.getAverage(1,1)/4F+"\t(exp)"+ra.getDecreasingWeightedLinearAverage(1, 2)+"\t"+ra);
			
			Thread.sleep(step);
		}
		
	}
}
