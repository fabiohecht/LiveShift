package net.liveshift.core;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeterministicPeerBehavior implements PeerBehavior {

	final private static Logger logger = LoggerFactory.getLogger(DeterministicPeerBehavior.class);
			
	final private int peerId;
	private Queue<Entry> entries;

	class Entry {
		public int channelNumber;
		public int timeShiftDiffMillis;
		public int holdingTimeSeconds;
	}

	DeterministicPeerBehavior(int peerId) throws IOException, ClassNotFoundException {
		this.peerId=peerId;
		readBehaviorCSV(ProbabilisticPeerBehavior.class.getResourceAsStream("/deterministic-peer-behavior.csv"));
	}
	
	private void readBehaviorCSV(InputStream inputStream) throws IOException, ClassNotFoundException {
		
		this.entries = new LinkedList<Entry>();
		
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
		
        String line;
        while ((line = in.readLine()) != null) {
			String[] items = line.split(";");

			if (logger.isDebugEnabled()) {
				logger.debug("read "+line);
			}
			
			if (items[0].equals(String.valueOf(this.peerId))) {
				
				Entry entry = new Entry();
				entry.channelNumber = Integer.valueOf(items[1]);
				entry.timeShiftDiffMillis = Integer.valueOf(items[2]);
				entry.holdingTimeSeconds = Integer.valueOf(items[3]);
				this.entries.add(entry);
			}
		}
		in.close();
	}

	@Override
	public boolean next() {
		this.entries.poll();
		return this.entries.size()>0;
	}

	@Override
	public boolean shouldChurn(float churnProbability) {
		return this.entries.peek().channelNumber==0;
	}
	
	@Override
	public int getChannelNumber() {
		return this.entries.peek().channelNumber;
	}

	@Override
	public long getTimeShiftRange(long range0, long range1) {
		return range1-this.entries.peek().timeShiftDiffMillis;
	}


	@Override
	public int getHoldingTimeS() {
		return this.entries.peek().holdingTimeSeconds;
	}
}
