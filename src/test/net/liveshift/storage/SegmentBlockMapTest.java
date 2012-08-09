package net.liveshift.storage;

import java.util.Random;

import net.liveshift.storage.SegmentBlockMap;

import org.junit.Test;


public class SegmentBlockMapTest {

	Random rnd = new Random();
	@Test
	public void testSegmentBlockMapTest() {
		SegmentBlockMap sbm = new SegmentBlockMap();
		/*
		
		*/
		
		System.out.println(sbm);

		sbm.set(0);
		System.out.println(sbm);
		
		sbm.setStartBlockNumber(15);
		sbm.set(0, 120);
		System.out.println(sbm);
		
		sbm.clear(55);
		System.out.println(sbm);
		
		sbm.clear(0);
		System.out.println(sbm);
		
		sbm.clear();
		for (int i=0; i<sbm.getLength(); i++) 
			if (rnd.nextBoolean()) {
				System.out.print(i+",");
				sbm.set(i);
			}
		
		System.out.println();
		System.out.println(sbm);

	}
}
