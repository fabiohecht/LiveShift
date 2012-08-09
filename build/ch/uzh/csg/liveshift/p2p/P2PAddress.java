package net.liveshift.p2p;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;

import net.liveshift.util.Utils;

import net.tomp2p.peers.Number160;
import net.tomp2p.peers.PeerAddress;

/**
 * The implementation of the underlying p2p overlay
 * 
 * @author Thomas Bocek
 * @author Fabio Hecht
 * @author Kevin Leopold
 */
public class P2PAddress implements Serializable, Comparable<P2PAddress>
{
	private static final long	serialVersionUID	= -5643130997818101398L;
	
	final private PeerAddress peerAddress;

	public P2PAddress(PeerAddress peerAddress)
	{
		this.peerAddress = peerAddress;
	}

	public P2PAddress(byte[] peerAddress) throws UnknownHostException {
		this.peerAddress = new PeerAddress(peerAddress);
	}

	public P2PAddress(byte[] peerAddress, byte[] socketAddress) throws UnknownHostException {
		this.peerAddress = new PeerAddress(peerAddress, socketAddress);
	}

	public P2PAddress(byte[] byteArray, int offset) throws UnknownHostException {
		this.peerAddress = new PeerAddress(byteArray, offset);
	}

	PeerAddress getPeerAddress()
	{
		return peerAddress;
	}
	
	@Override
	public int hashCode() {
		return this.peerAddress.hashCode();
	}
	
	@Override
	public boolean equals(Object pid1) {
		
		if(!(pid1 instanceof P2PAddress))
			return false;
		return this.peerAddress.equals(((P2PAddress) pid1).peerAddress);
	}
	
	@Override
	public String toString() {
		return this.peerAddress.toString();
	}

	@Override
	public int compareTo(P2PAddress o) {
		return this.peerAddress.compareTo(o.peerAddress);
	}

	public String getIpAddress() {
		return this.peerAddress.getInetAddress().getHostAddress();
	}
	
	public byte[] getIdAsByteArray() {
		return this.peerAddress.getID().toByteArray();
	}
	
	public Number160 getId() {
		return this.peerAddress.getID();
	}
	
	public InetAddress getInetAddress() {
		return this.peerAddress.getInetAddress();
	}

	public byte[] getAddressAsByteArray() {
		return this.peerAddress.toByteArray();
	}

	public byte[] getSocketAddressAsByteArray() {
		return this.peerAddress.toByteArraySocketAddress();
	}

	public int getIdAsByteArray(byte[] out, int offset) {
		return this.peerAddress.getID().toByteArray(out, offset);
		//this.peerAddress.getID().toByteArray(out, offset);
		//return offset+20;
	}

	public int getSocketAddressAsByteArray(byte[] out, int offset) {
		return this.peerAddress.toByteArraySocketAddress(out, offset);
	}
	public int getSocketAddressByteArraySize() {
		return this.peerAddress.getSocketAddressSize();
		//return 9;
	}

	public int getTcpPort() {
		byte[] socketAddress = this.peerAddress.toByteArraySocketAddress();
		int tcpPort = Utils.byteArrayToMediumInteger(socketAddress, 1);
		return tcpPort;
	}
}
