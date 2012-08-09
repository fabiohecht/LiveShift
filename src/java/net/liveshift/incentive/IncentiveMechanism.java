package net.liveshift.incentive;

import java.util.HashMap;
import java.util.Map;

import net.liveshift.core.PeerId;
import net.liveshift.core.Stats;
import net.liveshift.incentive.psh.FcfsIncentive;
import net.liveshift.incentive.psh.PSH;
import net.liveshift.incentive.psh.PshTrader;
import net.liveshift.incentive.psh.RandomIncentive;
import net.liveshift.incentive.psh.SducIncentive;
import net.liveshift.incentive.psh.TFT;
import net.liveshift.signaling.PshVideoSignaling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class IncentiveMechanism
{
	final private static Logger logger = LoggerFactory.getLogger(IncentiveMechanism.class);

	public enum IncentiveMechanismType { RANDOM, TFT, PSH, FCFS, SDUC }

	private IncentiveMechanismType incentiveMechanismType;
	private TFT<PeerId> tftHistory;
	
	private PshTrader pshTrader;
	final private Stats	stats;
	final private Map<PeerId, Float> reputationMultipliers = new HashMap<PeerId, Float>();
	
	private Map<String, Integer> initialHistoryMap;

	public IncentiveMechanism(final Stats stats) {
		this.stats = stats;
	}

	public void setIncentiveMechanism(IncentiveMechanismType incentiveMechanismType) {
		this.incentiveMechanismType = incentiveMechanismType;
	}
	
	public void setRandom() {
		this.tftHistory = new RandomIncentive<PeerId>(0);
	}
	public void setSduc() {
		this.tftHistory = new SducIncentive<PeerId>(0);
	}
	public void setFcfs() {
		this.tftHistory = new FcfsIncentive<PeerId>(0);
	}
	public void setTft() {
		
		this.tftHistory = new TFT<PeerId>(0);

	}
	public void setPsh(PshVideoSignaling pshVideoSignaling) {
		
		PSH<PeerId> pshHistory = new PSH<PeerId>(0);
		this.tftHistory = pshHistory;

		this.pshTrader = new PshTrader(pshVideoSignaling, pshHistory);
		
	}
	
	public void setReputation(final PeerId peerId, final float amount)
	{
		if (logger.isDebugEnabled()) logger.debug("setting reputation of "+peerId+" to "+amount);
		
		switch (this.incentiveMechanismType) {
			case PSH:
				this.pshTrader.addPeerForTrading(peerId);
			case TFT:
			case FCFS:
			case SDUC:

				tftHistory.setAmount(peerId, amount);

				//stats
				if (this.stats!=null)
					this.stats.updateReputationTable(this.tftHistory.getReputationTable());

				break;
		}
	}
	
	public void decreaseReputation(final PeerId peerId, final float amount, final long time)
	{
		if (logger.isDebugEnabled()) logger.debug("decreasing reputation of "+peerId+" by "+amount);
		
		switch (this.incentiveMechanismType) {
			case PSH:
				this.pshTrader.addPeerForTrading(peerId);
			case TFT:
			case FCFS:
			case SDUC:

				tftHistory.increaseDownload(peerId, -amount);

				//stats
				if (this.stats!=null)
					this.stats.updateReputationTable(this.tftHistory.getReputationTable());

				break;

		}
	}

	public void increaseReputation(final PeerId peerId, final float amount, final long time)
	{
		if (logger.isDebugEnabled()) logger.debug("increasing reputation of "+peerId+" by "+amount);
		
		switch (this.incentiveMechanismType) {
			case PSH:
				this.pshTrader.addPeerForTrading(peerId);
			case TFT:
			case FCFS:
			case SDUC:

				tftHistory.increaseDownload(peerId, amount);

				//stats
				if (this.stats!=null)
					this.stats.updateReputationTable(this.tftHistory.getReputationTable());

				break;
		}
	}
	
	public float compareReputations(PeerId peerId0, PeerId peerId1, long timeMillis) {
		float rp0 = this.getReputation(peerId0, timeMillis);
		float rp1 = this.getReputation(peerId1, timeMillis);
		return rp0 - rp1;
	}

	public float getReputation(final PeerId peerId, final long timeMillis) {
		float reputation = 0;
		
		if (this.incentiveMechanismType!=null)
			switch (this.incentiveMechanismType) {
				case RANDOM:
				case FCFS:
				case SDUC:
				case TFT:
				case PSH:
					reputation = tftHistory.getAmountForTrade(peerId);  //don't actually use timeMillis
					break;
			}
		
//		logger.debug("reputation of "+peerId+" at "+timeMillis+" is "+reputation);

		//applies penalty to peer if there's one to be applied
		reputation = this.modifyReputation(peerId, reputation);

//		logger.debug("reputation of "+peerId+" at "+timeMillis+" then became "+reputation);

		return reputation;
	}

	public void startBackgroundProcesses()
	{
		switch (this.incentiveMechanismType) {
			case PSH:
				this.pshTrader.startTrading();
				break;
		}
	}

	public IncentiveMechanismType getIncentiveMechanismType() {
		return this.incentiveMechanismType;
	}

	public PSH<PeerId> getPshHistory() {
		return (PSH<PeerId>) this.tftHistory;
	}

	public void shutdown() {
		if (this.pshTrader != null)
			this.pshTrader.shutdown();
	}

	/**
	 * penalizes peer by multiplying its reputation by the given multiplier if it is positive
	 * or by 1/multiplier if it is negative
	 * so, 0 > multiplier > 1 in order to penalize the peer
	 * 
	 * @param peerId
	 * @param inactivePeerReputationMultiplier between 0 and 1
	 */
	public void penalizePeer(final PeerId peerId, final float inactivePeerReputationMultiplier) {
		
		if (inactivePeerReputationMultiplier <= 0 || inactivePeerReputationMultiplier >= 1)
			throw new RuntimeException("Invalid argument, 0 > multiplier > 1 but it's "+inactivePeerReputationMultiplier);
		
		synchronized (this.reputationMultipliers) {
			Float multiplier = this.reputationMultipliers.get(peerId);
			if (multiplier==null)
				multiplier = 1F;
			this.reputationMultipliers.put(peerId, multiplier * inactivePeerReputationMultiplier);
		}
		
	}
	
	public void clearPeerPenalty(final PeerId peerId) {
		synchronized (this.reputationMultipliers) {
			this.reputationMultipliers.remove(peerId);
		}
	}
	
	private float modifyReputation(final PeerId peerId, final float reputation) {
		Float multiplier = null;
		synchronized (this.reputationMultipliers) {
			multiplier = this.reputationMultipliers.get(peerId);
		}
		if (multiplier==null)
			return reputation;
		else if (reputation > 0)
			return reputation * multiplier;
		else
			return reputation * 1/multiplier;
			

	}
	
	/**
	 * this exists so that we can pass an initial history and, when time comes that we encounter the mentioned peer,
	 * we set this initial reputation to it
	 * 
	 * @param Map<String, Integer> initialHistoryMap maps peer names (matched to PeerId.getName() and initial local history values
	 */
	public void setInitialHistory(Map<String, Integer> initialHistoryMap) {
		this.initialHistoryMap = initialHistoryMap;
	}
	
	public Integer getInitialHistory(String peerName) {
		
		if (peerName==null || this.initialHistoryMap==null)
			return 0;
		
		//returns the initial history only once! (see if we want it like this)
		Integer initialHistory = this.initialHistoryMap.remove(peerName);
		
		return (initialHistory==null?0:initialHistory);

	}

}
