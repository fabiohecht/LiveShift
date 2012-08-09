package net.liveshift.util;

import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.LogManager;

import net.liveshift.time.Clock;
import net.liveshift.util.TriggableScheduledExecutorService;

import org.junit.Test;



public class TriggableScheduledExecutorServiceTest {

	TriggableScheduledExecutorService tses;
	Random rnd = new Random();
	AtomicInteger ranTimes = new AtomicInteger();
	AtomicInteger calledTimes = new AtomicInteger();

	// Init LogManager
	static {
		try {
			LogManager.getLogManager().readConfiguration(TriggableScheduledExecutorServiceTest.class.getResourceAsStream("/jdklogtest.properties"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testTriggableScheduledExecutorService() throws InterruptedException {
		
		Callable<Boolean> task = new Callable<Boolean>() {
			
			@Override
			public Boolean call() throws Exception {
				System.out.println("ran "+ranTimes.incrementAndGet());
				Thread.sleep((long) (rnd.nextFloat()*100));
				return rnd.nextBoolean();
			}
		};
		
		tses = new TriggableScheduledExecutorService(task , 0, 0, 50, Clock.getMainClock(), "test1");
		/*
		for (int i = 0; i < 2000; i++) {
			System.out.println("called "+calledTimes.incrementAndGet());
			tses.scheduleNow();
			Thread.sleep(200);
		}
		*/
		Thread.sleep(1000*60*30);
	}
}
