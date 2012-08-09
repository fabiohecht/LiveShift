package net.liveshift.util;

import net.liveshift.time.Clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MovingAverage {
	
	final private static Logger logger = LoggerFactory.getLogger(MovingAverage.class);

	final private int numBoxes;	
	private final Clock clock;
	private final int[] boxes;
	private final int boxSizeMillis;
	private long startTimeMillis;
	
	private long lastErasedBox;
	
	public MovingAverage(final Clock clock, final int numBoxes, final int boxSizeMillis) {
		this.clock = clock;
		this.boxes = new int[this.numBoxes = numBoxes];
		this.boxSizeMillis = boxSizeMillis;
		this.startTimeMillis = clock.getTimeInMillis(false);
	}
	
	public void inputValue(int amount) {
		this.clearOldBoxes();

		this.boxes[this.getBoxNumber(this.clock.getTimeInMillis(false))] += amount;
	}
	private int getBoxNumber(long timeMillis) {
		return (int) ((timeMillis/ this.boxSizeMillis) % this.numBoxes);
	}

	public long getBoxTimeFloor(int box) {
		long currentTime = this.clock.getTimeInMillis(false);
		return ((currentTime/this.boxSizeMillis)+1)*this.boxSizeMillis;
	}
	public float getAverage() {
		this.clearOldBoxes();
		
		long firstbox = this.startTimeMillis/ this.boxSizeMillis;
		long currentbox = this.clock.getTimeInMillis(false)/ this.boxSizeMillis ;
		long boxcount = currentbox-firstbox+1;

		int sum=0;
		for (int i = 0; i<Math.min(boxcount,this.numBoxes); i++) {
			sum += this.boxes[(int)((firstbox+i)%this.numBoxes)];
		}
		return (float)sum / Math.min(boxcount,this.numBoxes);
	}

	/**
	 * gets the average of only the last given boxes
	 * 
	 * @param boxes
	 * @return
	 */
	public float getAverage(int boxes) {
		return getAverage(0, boxes);
	}
	/**
	 * gets the average of only from given start for the given number of boxes
	 * @param start
	 * @param boxes
	 * @return
	 */
	public float getAverage(int start, int boxes) {
		
		if (boxes > this.numBoxes)
			throw new RuntimeException("boxes must be <= this.numBoxes, but "+boxes+ ">"+ this.numBoxes);
		
		this.clearOldBoxes();
		
		int startBox = this.getBoxNumber(this.clock.getTimeInMillis(false));
		int sum=0;
		for (int i=0;i<boxes;i++)
			sum += this.boxes[(this.numBoxes+startBox-start-i)%this.numBoxes];
		
		return (float)sum / boxes;
	}
	
	/**
	 * gets the average of only from given start for the given number of boxes.
	 * the first box will be multiplied by the factor boxes, the second by by boxes-1 and so on.
	 * @param start
	 * @param boxes
	 * @return
	 */
	public float getDecreasingWeightedLinearAverage(int start, int boxes) {

		if (boxes > this.numBoxes)
			throw new RuntimeException("boxes must be <= this.numBoxes, but "+boxes+ ">"+ this.numBoxes);
		
		this.clearOldBoxes();
		
		int startBox = this.getBoxNumber(this.clock.getTimeInMillis(false));
		int sum=0;
		int factor = boxes;
		for (int i=0;i<boxes;i++) {
			sum += (factor--) * this.boxes[(this.numBoxes+startBox-start-i)%this.numBoxes];
		}
		
		return (float)sum / boxes;
	}
	
	public int getSum() {
		this.clearOldBoxes();
		
		int sum=0;
		for (int i = 0; i<this.numBoxes; i++)
			sum += this.boxes[i];
		return sum;
	}
	
	public int[] getRawValidBoxes(int fromBox) {
		this.clearOldBoxes();
		
		int firstbox = (int) (this.startTimeMillis/ this.boxSizeMillis);
		int currentbox = (int) (this.clock.getTimeInMillis(false)/ this.boxSizeMillis);
		int boxcount = currentbox-firstbox-fromBox;
		
		int[] out = new int[Math.min(boxcount, this.numBoxes)];
		for (int i=0;i<out.length;i++) {
			int box = (currentbox-i-fromBox)%this.numBoxes;
			out[i] = this.boxes[box];
		}
		return out;
	}

	private void clearOldBoxes() {
		long currentTimeMillis = this.clock.getTimeInMillis(false);
		
		if (this.lastErasedBox==0)
			this.lastErasedBox = currentTimeMillis/ this.boxSizeMillis;
		
		long numBoxesToErase = currentTimeMillis/ this.boxSizeMillis - this.lastErasedBox;
		long firstBoxToErase = this.lastErasedBox+1;
		
		for (int i=0; i < Math.min(numBoxesToErase,this.numBoxes); i++) {
			this.boxes[(int)((firstBoxToErase + i) % this.numBoxes)] = 0;
			if (logger.isDebugEnabled()) {
				logger.debug("cleared box "+i);
			}
		}
		
		if (numBoxesToErase>0)
			this.lastErasedBox = currentTimeMillis/ this.boxSizeMillis;
	}
	
	@Override
	public String toString() {
		
		int startbox = this.getBoxNumber(this.clock.getTimeInMillis(false));

		String out = "";
		for (int i=0;i<this.numBoxes;i++) {
			int box = (this.numBoxes+startbox-i)%this.numBoxes;
			out += boxes[box]+":";
		}
		out += "~"+this.getAverage();
		return out;
	}

	public int getBoxSizeMillis() {
		return this.boxSizeMillis;
	}

	public long getValidBoxCount() {
		this.clearOldBoxes();
		
		long firstbox = this.startTimeMillis/ this.boxSizeMillis ;
		long currentbox = this.clock.getTimeInMillis(false)/ this.boxSizeMillis ;
		return Math.min(currentbox-firstbox+1, this.numBoxes);
	}

	public long getTimeToSlideMillis() {
		long currentTimeMillis = this.clock.getTimeInMillis(false);

		return 1000L-(currentTimeMillis-((currentTimeMillis/this.boxSizeMillis)*this.boxSizeMillis));	
	}
}
