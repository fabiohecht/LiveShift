package net.liveshift.core;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import net.liveshift.util.Utils;


public class ProbabilisticPeerBehavior implements PeerBehavior {
	
	protected static final long	MIN_HOLDING_TIME = 1L;
	protected static final long	MAX_HOLDING_TIME = 86307L;  //TODO could read these from the file 
	private static int[] holdingTime;
	private static Map<Float, Integer> popularity;
	
	ProbabilisticPeerBehavior() {
		holdingTime = new int[1000];
		popularity = new TreeMap<Float, Integer>();
		
		readHoldingTimeCSV(ProbabilisticPeerBehavior.class.getResourceAsStream("/holding_time-1000samples.csv"), holdingTime);
		readHoldingTimeCSV(ProbabilisticPeerBehavior.class.getResourceAsStream("/numviewers-6channels-sum.csv"), popularity);
	}
	
	private void readHoldingTimeCSV(InputStream inputStream, Map<Float, Integer> map) {
		
		BufferedReader bufRdr = null;
		try {
			bufRdr = new BufferedReader(new InputStreamReader(inputStream));
			
			String line = null;
			String[] splitline;
			 
			//read each line of text file
			while((line = bufRdr.readLine()) != null)
			{
				splitline = line.split(";|\t");
				map.put(Float.valueOf(splitline[0]), Integer.valueOf(splitline[1]));
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			LiveShiftApplication.quit("file not found");
		} catch (IOException e) {
			e.printStackTrace();
			LiveShiftApplication.quit("ioexception");
		}
		finally {
			//close the file
			try {
				if (bufRdr!=null)
					bufRdr.close();
			} catch (IOException e) {
				e.printStackTrace();
				LiveShiftApplication.quit("ioexception2");
			}
		}
	}
	
	private void readHoldingTimeCSV(InputStream inputStream, int[] holdingTime) {

		BufferedReader bufRdr = null;
		try {
			bufRdr = new BufferedReader(new InputStreamReader(inputStream));
			
			String line = null;
			
			int linenumber = 0;
			//read each line of text file
			while((line = bufRdr.readLine()) != null)
			{
				holdingTime[linenumber++] = Integer.parseInt(line);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			LiveShiftApplication.quit("file not found");
		} catch (IOException e) {
			e.printStackTrace();
			LiveShiftApplication.quit("ioexception");
		}
		finally {
			//close the file
			try {
				if (bufRdr!=null)
					bufRdr.close();
			} catch (IOException e) {
				e.printStackTrace();
				LiveShiftApplication.quit("ioexception 2");
			}
		}
	}

	/* (non-Javadoc)
	 * @see net.liveshift.core.PeerBehavior#getHoldingTimeS()
	 */
	@Override
	public int getHoldingTimeS() {
		return getRandomElement(holdingTime);
	}

	/* (non-Javadoc)
	 * @see net.liveshift.core.PeerBehavior#getChannelNumber()
	 */
	@Override
	public int getChannelNumber() {
		return getRandomElement(popularity);
	}

	/* (non-Javadoc)
	 * @see net.liveshift.core.PeerBehavior#getTimeShiftRange(long, long)
	 */
	@Override
	public long getTimeShiftRange(long range0, long range1) {
		long x = getRandomElement(holdingTime);
		return Math.round(range1-(range1-range0)*(x-MIN_HOLDING_TIME)/(double)(MAX_HOLDING_TIME-MIN_HOLDING_TIME));
	}

	private Integer getRandomElement(Map<Float, Integer> map) {
		float chance = Utils.getRandomFloat();
		
		for (Entry<Float, Integer> entry : map.entrySet())
			if (entry.getKey() > chance)
				return entry.getValue();
		
		System.err.println("problem getting random element from "+map);
		LiveShiftApplication.quit("problem getting random element from "+map);
		return -1;
	}
	
	private int getRandomElement(int[] holdingTime) {
		float chance = Utils.getRandomFloat();
		
		return holdingTime[(int) Math.floor(chance*1000)];
	}

	/* (non-Javadoc)
	 * @see net.liveshift.core.PeerBehavior#shouldChurn(float)
	 */
	@Override
	public boolean shouldChurn(float churnProbability) {
		return Utils.getRandomFloat() < churnProbability;
	}

	@Override
	public boolean next() {
		// do nothing (everything is random, so nothing to advance)
		return true;
	}
	
}
