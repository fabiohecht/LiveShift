package net.liveshift.download;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import net.liveshift.core.PeerId;
import net.liveshift.signaling.messaging.SegmentBlock;
import net.liveshift.storage.SegmentBlockMap;
import net.liveshift.storage.SegmentIdentifier;
import net.liveshift.time.Clock;
import net.liveshift.upload.VectorHave;
import net.liveshift.upload.VectorHaveCollector;
import net.liveshift.util.ExecutorPool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A probabilistic block map that is not only updated by regular Haves but also VectorHaves
 * Probabilistic bits are returned as Set
 * 
 * @author fabio
 *
 */

public class ProbabilisticSegmentBlockMap {
	
	final private static Logger logger = LoggerFactory.getLogger(ProbabilisticSegmentBlockMap.class);

	final private SegmentIdentifier	segmentIdentifier;  //actually used only for printing when debugging
	final private PeerId peerId;  //actually used only for printing when debugging
	final private SegmentBlockMap segmentBlockMap;
	
	final Tuner tuner;
	
	private long rateUpdateTimeMillis;
	private int referenceBlock;
	private double rate;
	private int probBlockCount;
	
	private ScheduledFuture<?> probabilisticHave;
	private Runnable probabilisticBlockRunner = new Runnable() {
		
		@Override
		public void run() {
			if (++referenceBlock < SegmentBlock.getBlocksPerSegment()) {
				segmentBlockMap.set(referenceBlock);
				if (logger.isDebugEnabled()) logger.debug("["+peerId+":"+segmentIdentifier.toString()+"] probabilistically setting b#"+(referenceBlock));
				
				//triggers block scheduler to speed things up
				tuner.maybeScheduleBlock(peerId, segmentIdentifier, referenceBlock, true);
				
			}
			if (++probBlockCount >= VectorHaveCollector.VECTOR_LIMIT || referenceBlock==SegmentBlock.getBlocksPerSegment())
				probabilisticHave.cancel(false);
		}
	};

	public ProbabilisticSegmentBlockMap(final SegmentBlockMap segmentBlockMap, final SegmentIdentifier segmentIdentifier, final PeerId peerId, final Tuner tuner) {
		this.segmentBlockMap = segmentBlockMap;
		this.segmentIdentifier = segmentIdentifier;  //actually used only for printing when debugging
		this.peerId = peerId;  //actually used only for printing when debugging
		
		this.tuner = tuner;
	}

	public void addSegmentBlockMapUpdateVector(final VectorHave vectorHave) {
		if (logger.isDebugEnabled()) logger.debug("["+peerId+":"+segmentIdentifier.toString()+"] adding vector: "+vectorHave);

		this.addSegmentBlockMapUpdateVector(vectorHave.getBlockNumber(), vectorHave.doHave(), vectorHave.getRate());
	}
	
	/**
	 * 
	 * @param blockNumber
	 * @param rate rate expected rate at which new blocks are expected to arrive from blockNumber. -1=don't change the last prediction, 0=clear last prediction, >0=update the last prediction
	 */
	public void addSegmentBlockMapUpdateVector(final int blockNumber, final boolean doHave, final float rate) {
		//kills runner
		if (this.probabilisticHave!=null)
			this.probabilisticHave.cancel(false);
		
		//updates block (for sure there)
		if (blockNumber>-1)
			this.segmentBlockMap.set(blockNumber, doHave);
		
		//updates vector
		this.rateUpdateTimeMillis = Clock.getMainClock().getTimeInMillis(false);
		this.rate = rate;
		this.probBlockCount=0;

		if (this.rate>0) {
			this.referenceBlock=blockNumber;
		
			//runs runner to detect slowing rate
			this.probabilisticHave = ExecutorPool.getScheduledExecutorService().scheduleAtFixedRate(this.probabilisticBlockRunner, (long) this.rate*1000, (long) this.rate*1000, TimeUnit.MICROSECONDS);
		}
	}

	public boolean get(final int bitIndex) {
		return this.segmentBlockMap.get(bitIndex);
	}

	@Override
	public String toString() {
		return "(prob:"+this.rate+"@"+this.rateUpdateTimeMillis+")"+this.segmentBlockMap.toString();
	}

	public int getLastSetBit(final int blockNumber) {
		return this.segmentBlockMap.getLastSetBit(blockNumber);
	}
	
	@Override
	public boolean equals(Object obj) {
		
		if (obj instanceof ProbabilisticSegmentBlockMap)
			obj = ((ProbabilisticSegmentBlockMap)obj).segmentBlockMap;
		
		return this.segmentBlockMap.equals(obj);
		/*
		//TODO make this more efficient, not necessary to read bit by bit, could work with intervals or words
		if (obj instanceof SegmentBlockMap) {
			SegmentBlockMap sbm = (SegmentBlockMap) obj;
			for (int i=0; i<sbm.getLength(); i++)
				if (sbm.get(i)!=this.get(i))
					return false;
			return true;
		}
		else if (obj instanceof ProbabilisticSegmentBlockMap) {
			ProbabilisticSegmentBlockMap psbm = (ProbabilisticSegmentBlockMap) obj;
			for (int i=0; i<psbm.getLength(); i++)
				if (psbm.get(i)!=this.get(i))
					return false;
			return true;
		}
		else return false;*/
	}

	private int getLength() {
		return this.segmentBlockMap.getLength();
	}
	
	public void shutdown() {
		logger.debug("in shutdown pid:"+peerId+" si:"+this.segmentIdentifier+" sb:"+this.segmentBlockMap);
		if (this.probabilisticHave!=null && !this.probabilisticHave.isCancelled())
			this.probabilisticHave.cancel(false);
	}
	
}
