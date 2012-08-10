package net.liveshift.core;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//generates a probabilistic one to be used as a deterministic one

public class DeterministicPeerBehaviorGenerator {
	
	final private static Logger logger = LoggerFactory.getLogger(DeterministicPeerBehaviorGenerator.class);

	private static final float	churnProbability	= 0;

	public static void main(String[] args) throws IOException {
		
		File output = new File("src/conf/deterministic-peer-behavior.csv");
		output.delete();
		output.createNewFile();
		
		PeerBehavior peerBehavior = new ProbabilisticPeerBehavior();
		List<String> pbs = createPeerBehaviorScript(peerBehavior);
		
		BufferedWriter out = new BufferedWriter(new FileWriter(output));
		for (String line: pbs) {
			out.write(line);
			out.newLine();
		}
		out.close();
	}
	
	public static List<String> createPeerBehaviorScript(PeerBehavior peerBehavior) {
		
		int simulationDurationSeconds = 60*60;  //one hour
		int peers=6+15+150;  //worst case: s4
		List<String> out = new ArrayList<String>(peers*simulationDurationSeconds/10);
		for (int p=1; p<=peers; p++) {
			int totalHoldingTime=0;
			while (totalHoldingTime<simulationDurationSeconds) {
				int holdingTime = peerBehavior.getHoldingTimeS();
				int channel = peerBehavior.shouldChurn(churnProbability)?0:peerBehavior.getChannelNumber();
				out.add(p+";"+channel+";"+(totalHoldingTime*1000-peerBehavior.getTimeShiftRange(0, totalHoldingTime*1000))+";"+holdingTime);
				if (logger.isDebugEnabled()) {
					logger.debug(p+";"+channel+";"+(totalHoldingTime*1000-peerBehavior.getTimeShiftRange(0, totalHoldingTime*1000))+";"+holdingTime);
				}
				totalHoldingTime+=holdingTime;
			}
		}
		return out;
	}
	
	/*
	//outputs shuffled entries (to avoid having many short sessions then a long one) -- doesn't make sense, still achieves same thing but with less overall switches
	public static void createShuffledPeerBehaviorScript(PeerBehavior peerBehavior) {
		
		class Entry implements Comparable<Entry>{
			public int	channelNumber;
			public int	holdingTimeSeconds;
			private final int order = random.nextInt();
			@Override
			public int compareTo(Entry o) {
				return this.order-o.order;
			}
		}
		
		int simulationDurationSeconds = 60*60;  //one hour
		int peers=15+150;
		for (int p=1; p<=peers; p++) {
			int totalHoldingTime=0;
			SortedSet<Entry> entries = new TreeSet<Entry>();
			while (totalHoldingTime<simulationDurationSeconds) {
				int holdingTime = peerBehavior.getHoldingTimeS();
				Entry entry = new Entry();
				entry.channelNumber = peerBehavior.getChannelNumber();
				entry.holdingTimeSeconds = holdingTime;
				entries.add(entry);
				totalHoldingTime+=holdingTime;
			}
			totalHoldingTime=0;
			for (Entry entry : entries) {
				if (logger.isDebugEnabled()) {
					logger.debug(p+";"+entry.channelNumber+";"+peerBehavior.getTimeShiftRange(0, totalHoldingTime*1000)+";"+entry.holdingTimeSeconds);
				}
				totalHoldingTime+=entry.holdingTimeSeconds;
			}
		}
	}
*/
}
