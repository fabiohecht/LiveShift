package net.liveshift.configuration;

import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;

import net.liveshift.configuration.Configuration;

import org.junit.Test;

public class ConfigurationTest {
	
	@Test
	public void testVlcPathDiscovery() {
		

		Set<String> suspects = Configuration.findVlcLibs("");
		for (String lib : suspects) {
			System.out.println("find returned: "+lib);
		}
		
		
	}
}
