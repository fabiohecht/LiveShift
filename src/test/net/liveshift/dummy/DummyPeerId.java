package net.liveshift.dummy;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Random;

import net.liveshift.core.PeerId;
import net.liveshift.p2p.P2PAddress;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.PeerAddress;


/**
 * Represents a Neighbor for testing purposes
 *
 * @author Kevin Leopold
 *
 */
public class DummyPeerId extends PeerId {

	private static final long	serialVersionUID	= 8917592295378323703L;
	final private static Random rnd=new Random(0); 
	
	public DummyPeerId(int number) throws UnknownHostException  {
		super(new P2PAddress(new PeerAddress(new Number160(number), "127.0.0.1", 20000, 20000)));
		this.setName("p"+number);
		
	}
	public DummyPeerId(int number, int port)  {
		super(new P2PAddress(new PeerAddress(new Number160(number), new InetSocketAddress("127.0.0.1",port))));
		this.setName("p"+number);
	}
	public DummyPeerId(Number160 number, int port)  {
		super(new P2PAddress(new PeerAddress(number, new InetSocketAddress("127.0.0.1",port))));
		this.setName("p"+number);
	}
}
