package net.liveshift.upload;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import net.liveshift.configuration.Configuration;
import net.liveshift.signaling.messaging.SegmentBlock;
import net.liveshift.storage.Segment;
import net.liveshift.time.Clock;
import net.liveshift.util.ExecutorPool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * Manages and returns HaveVectors for local segments
 * Notifies subscribers if vector has changed, so peers can be notified
 * 
 * @author fabio
 *
 */

public class VectorHaveCollector {

	interface VectorHaveSender {
		void sendHaveMessage(VectorHave vectorHave);
		void sendHaveMessageToNewSubscribers(VectorHave vectorHave);  //sends only to the new ones, that did not get the last vector
	}

	final private static Logger logger = LoggerFactory.getLogger(VectorHaveCollector.class);
	
	private static final int MAX_WINDOW_SIZE = 10;
	private static final int MIN_WINDOW_SIZE = 3;
	private static final float MIN_R_SQUARE = .98F;  //sends vector if r² is large enough, otherwise individual Haves (containing rate=0)
	private static final float UPDATE_THRESHOLD = .1F;  //updates vector if estimation changes significantly
	public static final int VECTOR_LIMIT = 20;  //TODO maybe add this to vectorhave??? based on r^2??? <=====================
	private static final long MONITOR_RUNNER_FREQUENCY_MICROS = Configuration.SEGMENTBLOCK_SIZE_MS*1000/4;
	final private boolean vectorHavesEnabled;

	final private Segment segment;
	
	private long firstTime = 0; //not to work with such large numbers
	private long inactiveSince; //to shut it down when not used for a while
	private int[] lastReceivedBlocksTime = new int[MAX_WINDOW_SIZE];
	private int[] lastReceivedBlocksNumber = new int[MAX_WINDOW_SIZE];
	private int currentSlot = 0;
	
	private long lastGivenReferenceBlockTimeMillis = -1;
	private int lastGivenReferenceBlock = -1;
	private int lastGivenRate = 0;
	
	public double regressionSlope;
	//public double regressionYIntercept;   ====> maybe send this also in message... need to check if necessary, at the moment, it sends the slope only, to be applied from the block being announced
	public double regressionRsquared;
	
	private Set<VectorHaveSender> vectorHaveSenders = new HashSet<VectorHaveSender>();
	private ScheduledFuture<?> monitor;
	
	private Runnable monitorRunner = new Runnable() {
		
		@Override
		public void run() {
			checkprediction();
		}
	};

	synchronized private void checkprediction() {

		if (this.lastGivenRate>0 && this.lastGivenReferenceBlock>-1) {
			long currentTimeMillis = Clock.getMainClock().getTimeInMillis(false);
			long timeFrameMillis = (currentTimeMillis-this.lastGivenReferenceBlockTimeMillis) + (MONITOR_RUNNER_FREQUENCY_MICROS/1000) - BlockRequestPipeline.UNAVAILABLE_BLOCK_TIME_LIMIT_MILLIS; // with tolerance 
			int blockToCheck = this.lastGivenReferenceBlock + (int) (timeFrameMillis/this.lastGivenRate);  //TODO imprecise for peers that received different reference blocks
			
			if (blockToCheck < SegmentBlock.getBlocksPerSegment()) {
				if (logger.isDebugEnabled()) logger.debug("will check block "+blockToCheck+", I have: "+this.segment.getSegmentBlockMap());
				
				if (blockToCheck>this.lastGivenReferenceBlock && !segment.getSegmentBlockMap().get(blockToCheck)) {
					
					if (logger.isDebugEnabled()) logger.debug(blockToCheck+" will be badly predicted by the subscribers, I will cancel it now");
					
					//predicted block not arrived yet (prediction too fast)
					//cancels prediction
					this.lastGivenRate = 0;
					this.lastGivenReferenceBlock = -1;
					this.lastGivenReferenceBlockTimeMillis = -1;
					
					VectorHave vectorHave = new VectorHave(this.segment.getSegmentIdentifier(), blockToCheck, false, 0);
					sendToSubscribers(vectorHave);
				}
				else {
					//predicted block is too far behind (prediction too slow)
					//[detected only by blockReceived() below]
					//maybe put something here in the future
					
				}
			}
		}	
	}
	
	/**
	 * gets which is the last block that the subscribers must have predicted
	 * 
	 * @return the block number or -1 if there is no
	 */
	private int getLastPredictedBlock() {
		if (this.lastGivenRate>0 && this.lastGivenReferenceBlock>-1) {
			long currentTimeMillis = Clock.getMainClock().getTimeInMillis(false);
			long timeFrameMillis = (currentTimeMillis-this.lastGivenReferenceBlockTimeMillis); // without tolerance 
			int lastPredictedBlock = this.lastGivenReferenceBlock + (int) (timeFrameMillis/this.lastGivenRate);  //TODO imprecise for peers that received different reference blocks
			
			if (logger.isDebugEnabled()) logger.debug("lpb="+this.lastGivenReferenceBlock+"+"+timeFrameMillis+"/"+this.lastGivenRate);

			return lastPredictedBlock;
		}
		return -1;  //no predicted block
	}
	
	public VectorHaveCollector(final Segment segment, final VectorHaveSender vectorHaveSender, final boolean vectorHavesEnabled) {
		this.segment=segment;
		this.addSegmentBlockMapUpdateVectorSender(vectorHaveSender);
		this.monitor = ExecutorPool.getScheduledExecutorService().scheduleAtFixedRate(this.monitorRunner, MONITOR_RUNNER_FREQUENCY_MICROS ,MONITOR_RUNNER_FREQUENCY_MICROS , TimeUnit.MICROSECONDS);

		long currentTime = Clock.getMainClock().getTimeInMillis(false);
		this.inactiveSince = currentTime;
		this.vectorHavesEnabled = vectorHavesEnabled;
	}
	
	synchronized public void shutdown() {
		logger.debug("shutdown");
		
		this.monitor.cancel(false);
		
		//cancels sent vectors
		if (this.lastGivenRate>0)
			this.sendToSubscribers(new VectorHave(this.segment.getSegmentIdentifier(), -1, false, 0));
	}
	
	/**
	 * triggered when a new block is downloaded (or created)
	 * may send a vector have, a scalar have, or nothing to subscribers
	 * 
	 * @param blockNumber
	 */	
	synchronized public void blockReceived(final int blockNumber) {
		
		if (logger.isDebugEnabled()) logger.debug("blockReceived b#"+blockNumber);

		//registers having received the new block
		long currentTime = Clock.getMainClock().getTimeInMillis(false);
		this.inactiveSince = currentTime;
		
		if (this.currentSlot==0)
			this.firstTime = currentTime;
		
		currentTime -= this.firstTime;
		
		this.lastReceivedBlocksNumber[this.currentSlot%MAX_WINDOW_SIZE] = blockNumber;
		this.lastReceivedBlocksTime[this.currentSlot%MAX_WINDOW_SIZE] = (int) currentTime;
		
		this.currentSlot++;
		
		//sends an individual Have, a Vector Have, or nothing (if the already sent vector would not have changed significantly) to all subscribers  
		
		VectorHave vectorHave = null;
		
		this.calculateLinearRegression();
		
		int newRate;
		if (this.vectorHavesEnabled && this.regressionRsquared > MIN_R_SQUARE && this.currentSlot > MIN_WINDOW_SIZE) {
			
			double updateFactor=Math.abs(1-(this.regressionSlope / this.lastGivenRate));
			
			if (this.lastGivenRate==0 || updateFactor >= UPDATE_THRESHOLD || blockNumber-this.lastGivenReferenceBlock>=VECTOR_LIMIT) {
				//must be updated/sent (prediction too slow)
				if (logger.isDebugEnabled()) logger.debug("will send vector since "+updateFactor+" >= "+UPDATE_THRESHOLD + " || "+(blockNumber-this.lastGivenReferenceBlock)+">="+VECTOR_LIMIT);
				
				newRate = (int) Math.round(this.regressionSlope);
				vectorHave = new VectorHave(this.segment.getSegmentIdentifier(), blockNumber, true, newRate);
			}
			else {
				//prediction should work fine
				//BUT may need to notify new peers that missed the last prediction
				newRate = this.lastGivenRate;
				this.sendToNewSubscribers(new VectorHave(this.segment.getSegmentIdentifier(), blockNumber, true, newRate));
			}
		}
		else {
			//sends individual Have (r² too low, or configured to send only scalars)
			if (logger.isDebugEnabled()) logger.debug("will send scalar since R²="+this.regressionRsquared + " <= " +MIN_R_SQUARE + " || min win " +this.currentSlot +"<="+ MIN_WINDOW_SIZE);

			newRate=0;
			vectorHave = new VectorHave(this.segment.getSegmentIdentifier(), blockNumber, true, newRate);
		}
		
		if (vectorHave!=null) {
			
			int lastPredictedBlock = this.getLastPredictedBlock();
			if (logger.isDebugEnabled()) logger.debug("lpb="+lastPredictedBlock+" b#"+vectorHave.getBlockNumber());

			if (lastPredictedBlock>-1) {
				for (int holeBlock=lastPredictedBlock+1; holeBlock < vectorHave.getBlockNumber(); holeBlock++) {
					if (this.segment.getSegmentBlockMap().get(holeBlock)) {
						//covers hole
						if (logger.isDebugEnabled()) logger.debug("covering hole, b#"+holeBlock);
						this.sendToSubscribers(new VectorHave(this.segment.getSegmentIdentifier(), holeBlock, true, -1));
					}
				}
				
				for (int overrunBlock=Math.max(blockNumber+1,lastGivenReferenceBlock); overrunBlock <= lastPredictedBlock; overrunBlock++) {
					if (!this.segment.getSegmentBlockMap().get(overrunBlock)) {
						//predicted block is not present
						//sends a Have (false) for the block
						if (logger.isDebugEnabled()) logger.debug("fixing overrun, b#"+overrunBlock);
						this.sendToSubscribers(new VectorHave(this.segment.getSegmentIdentifier(), overrunBlock, false, -1));
					}
				}
			}

			lastGivenReferenceBlock = blockNumber;
			this.lastGivenReferenceBlockTimeMillis = Clock.getMainClock().getTimeInMillis(false);
			this.sendToSubscribers(vectorHave);
			
			this.lastGivenRate = newRate;
		}
	}
	
	
	private void sendToSubscribers(final VectorHave vectorHave) {
		for (VectorHaveSender vectorHaveSender : this.vectorHaveSenders) {
			vectorHaveSender.sendHaveMessage(vectorHave);
		}
	}
	
	private void sendToNewSubscribers(final VectorHave vectorHave) {
		for (VectorHaveSender subscriber : this.vectorHaveSenders) {
			subscriber.sendHaveMessageToNewSubscribers(vectorHave);
		}
	}
	
	/**
	 * adds a handler that gets notified when the vector is updated
	 * (the handler must send the new vector to all subscribed peers)
	 * 
	 * @param vectorHaveSender
	 */
	synchronized public void addSegmentBlockMapUpdateVectorSender(final VectorHaveSender vectorHaveSender) {
		this.vectorHaveSenders.add(vectorHaveSender);
	}
	synchronized public void removeSegmentBlockMapUpdateVectorSender(final VectorHaveSender vectorHaveSender) {
		this.vectorHaveSenders.remove(vectorHaveSender);
	}		
	
	private void calculateLinearRegression() {
		
		int n = Math.min(this.currentSlot, MAX_WINDOW_SIZE), m=0;
		
		if (n<MIN_WINDOW_SIZE) {
			this.regressionSlope = 0;
//			this.regressionYIntercept = 0;
			this.regressionRsquared = 0;
			return;
		}
			
		int sumX=0, sumY=0, sumXY=0, lastX=-1;
		long sumXsquared=0, sumYsquared=0, lastY=-1;
		
		int firstSlot=this.currentSlot <= MAX_WINDOW_SIZE?0:(this.currentSlot)%MAX_WINDOW_SIZE;
		
		String debug = "";
		for (int i=firstSlot+n-1;  i>=firstSlot; i--) {
			
			int x = this.lastReceivedBlocksNumber[i%MAX_WINDOW_SIZE];
			long y = this.lastReceivedBlocksTime[i%MAX_WINDOW_SIZE];

			if (lastX==-1 || lastX-1==x) {
				sumX+=x;
				sumXsquared+=Math.pow(x, 2);
				sumXY+=x*y;
				sumY+=y;
				sumYsquared+=Math.pow(y, 2);
				m++;
			}
			else
				break;
			
			if (logger.isDebugEnabled()) {
				if (lastX!=-1)
					debug+=(";["+lastX+","+(lastY!=-1?(lastY-y):"-")+"]");
				lastY=y;
			}
			lastX=x;
			
		}
		
		if (m<MIN_WINDOW_SIZE) {
			this.regressionSlope = 0;
//			this.regressionYIntercept = 0;
			this.regressionRsquared = 0;
		}
		else {
			double sumX_squared = Math.pow(sumX, 2);
			double sumY_squared = Math.pow(sumY, 2);
			
			this.regressionSlope = (m*sumXY-sumX*sumY)/(m*sumXsquared-sumX_squared);
//			this.regressionYIntercept = this.firstTime + (double)(sumY-this.regressionSlope*sumX)/m;
			this.regressionRsquared = (m*sumXY-sumX*sumY)/Math.sqrt((m*sumXsquared-sumX_squared)*(m*sumYsquared-sumY_squared));

			if (logger.isDebugEnabled()) logger.debug(/*"b="+regressionYIntercept+*/" m="+regressionSlope+ " r²="+regressionRsquared+" "+debug/*+"="+(m*sumXY-sumX*sumY)+"/Math.sqrt("+(n*sumXsquared-sumX_squared)+"*"+ (m*sumYsquared-sumY_squared)+")"*/);
		}
		
	}
	
	public long inactiveForMillis() {
		long currentTime = Clock.getMainClock().getTimeInMillis(false);
		return currentTime - this.inactiveSince;
	}

	public Segment getSegment() {
		return this.segment;
	}
}

