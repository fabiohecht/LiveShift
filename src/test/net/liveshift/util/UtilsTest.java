package net.liveshift.util;

import junit.framework.Assert;

import net.liveshift.util.Utils;

import org.junit.Test;

public class UtilsTest {
	
	@Test
	public void testBaseConversion() {
		
		for (int i=0; i<20; i++) {
			int base = (Math.abs(Utils.getRandomInt()%19))+2;
			System.out.println("1 "+base);
			Assert.assertEquals("1",Utils.convertBase(1, base));
		}
		for (int i=0; i<20; i++) {
			int number = Utils.getRandomInt();
			System.out.println(number+ " 16");
			Assert.assertEquals(Integer.toHexString(number).toLowerCase(),Utils.convertBase(number, 16).toLowerCase());
		}
	}
}
