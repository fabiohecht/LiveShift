package net.liveshift.upload;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.liveshift.storage.Segment;
import net.liveshift.upload.VectorHaveCollector.VectorHaveSender;


public class VectorHaveCollectors {
	
	private static final long TIMEOUT	= 10000L;
	final private boolean vectorHavesEnabled;
	final private VectorHaveSender	vectorHaveSender;
	final private Map<Segment, VectorHaveCollector> vectorCollectors = new HashMap<Segment, VectorHaveCollector>();
	
	public VectorHaveCollectors(final VectorHaveSender vectorHaveSender, final boolean vectorHavesEnabled) {
		this.vectorHaveSender=vectorHaveSender;
		this.vectorHavesEnabled=vectorHavesEnabled;
	}
	
	synchronized public void blockReceived(final Segment segment, final int blockNumber) {
		
		VectorHaveCollector vectorHaveCollector = this.vectorCollectors.get(segment);
		
		if (vectorHaveCollector==null) {
			vectorHaveCollector = new VectorHaveCollector(segment, vectorHaveSender, vectorHavesEnabled);
			this.vectorCollectors.put(segment, vectorHaveCollector);
		}
		
		this.shutdownInactiveCollectors();
		
		vectorHaveCollector.blockReceived(blockNumber);
	}	
	
	private void shutdownInactiveCollectors() {
		Set<Segment> collectorsToRemove = new HashSet<Segment>();
		for (VectorHaveCollector vectorHaveCollector : this.vectorCollectors.values()) {
			
			if (vectorHaveCollector.inactiveForMillis() > TIMEOUT) {
				vectorHaveCollector.shutdown();
				collectorsToRemove.add(vectorHaveCollector.getSegment());
			}
		}
		for (Segment vectorHaveCollector : collectorsToRemove) {
			this.vectorCollectors.remove(vectorHaveCollector);
		}
	}

	synchronized public void shutdown() {
		for (VectorHaveCollector vectorHaveCollector : this.vectorCollectors.values()) {
			vectorHaveCollector.shutdown();
		}
	}
}
