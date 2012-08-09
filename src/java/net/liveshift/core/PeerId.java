package net.liveshift.core;


import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;

import net.liveshift.p2p.P2PAddress;
import net.tomp2p.peers.PeerAddress;


/**
 * Describes a neighbor (peer)
 *
 * @author Fabio Victora Hecht
 * @author Kevin Leopold
 *
 */
public class PeerId implements Serializable {

	private static final long	serialVersionUID	= 4515227562962185180L;
	
	protected final P2PAddress dhtId; // in TomP2P, it will be Number160
	private String name;  //name is only for being displayed, it doesn't have to be unique
	
	private int signalingPort;
	
	public PeerId(P2PAddress dhtId)
	{
		this.dhtId = dhtId;
		
		//FIXME another temporary ugly hack
		if (dhtId!=null) {
			this.signalingPort = dhtId.getTcpPort()+1000;
		}
	}
	
	/**
	 * constructs a peer Id with a given dhtId (with socket address), with only the peer name from a byteArray
	 * 
	 * @param byteArray
	 * @param peerNameOffset
	 * @param dhtId
	 * @throws UnknownHostException
	 */
	public PeerId(byte[] byteArray, int peerNameOffset, P2PAddress dhtId) throws UnknownHostException {
		
		this.dhtId = dhtId;
		
		byte[] nameAsByteArray = new byte[byteArray[peerNameOffset]]; 
		System.arraycopy(byteArray, peerNameOffset+1, nameAsByteArray, 0, nameAsByteArray.length);  //name
		
		this.name = new String(nameAsByteArray);
		
		//FIXME another temporary ugly hack
		this.signalingPort = dhtId.getTcpPort()+1000;

	}
	
	/**
	 * constructs a peer Id based on data from a byteArray that contains the dhtId, the socket address, and the peer name 
	 * 
	 * @param byteArray
	 * @param dhtIdOffset
	 * @param socketAddressOffset
	 * @throws UnknownHostException
	 */
	public PeerId(byte[] byteArray, int dhtIdOffset, int socketAddressOffset) throws UnknownHostException {
		
		byte[] peerAddress = new byte[20];
		System.arraycopy(byteArray, dhtIdOffset, peerAddress, 0, 20);  //dhtid
		
		byte[] socketAddress = new byte[byteArray[socketAddressOffset]];
		System.arraycopy(byteArray, socketAddressOffset+1, socketAddress, 0, socketAddress.length);  //address
		
		this.dhtId = new P2PAddress(peerAddress, socketAddress);
		
		byte[] nameAsByteArray = new byte[byteArray[socketAddressOffset+1+byteArray[socketAddressOffset]]]; 
		System.arraycopy(byteArray, socketAddressOffset+2+byteArray[socketAddressOffset], nameAsByteArray, 0, nameAsByteArray.length);  //name
		
		this.name = new String(nameAsByteArray);
		
		//FIXME another temporary ugly hack
		this.signalingPort = dhtId.getTcpPort()+1000;

		
	}
	
	public PeerId(byte[] byteArray, int offset) throws UnknownHostException {
		this(byteArray, 0, 20);
	}

	public PeerId(PeerAddress peerAddress, String name) {
		this.dhtId=new P2PAddress(peerAddress);
		this.name=name;
		
		//FIXME another temporary ugly hack
		this.signalingPort = dhtId.getTcpPort()+1000;

	}

	public P2PAddress getDhtId()
	{
		return dhtId;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString()
	{
		return this.name;
	}
	
	@Override
	public int hashCode() {
		return this.getDhtId().hashCode();
	}
	
	@Override
	public boolean equals(Object pid1) {
		
		if(!(pid1 instanceof PeerId))
			return false;
		return dhtId.equals(((PeerId) pid1).dhtId);
	}

	public String getIpAddress() {
		return this.dhtId.getIpAddress();
	}

	public InetAddress getInetAddress() {
		return this.dhtId.getInetAddress();
	}
	
	public byte[] toByteArray() {
		
		int size = 20 + 1 + this.dhtId.getSocketAddressByteArraySize() + 1 + this.name.getBytes().length;
		
		byte[] out = new byte[size];
		int offset=0;
		offset = this.dhtId.getIdAsByteArray(out, offset);
		out[offset++] = (byte) this.dhtId.getSocketAddressByteArraySize();
		offset = this.dhtId.getSocketAddressAsByteArray(out, offset);
		
		byte[] nameAsByteArray = this.name.getBytes();
		out[offset++] = (byte) nameAsByteArray.length;
		System.arraycopy(nameAsByteArray, 0, out, offset, nameAsByteArray.length);
		offset += nameAsByteArray.length;
		
		return out;
	}
	/*
	//tests from here below
	private int	port = 11066;  //test
	public void setPort(int port) {
		this.port = port;
	}
	public int getPort() {
		return this.port;
	}
*/

	public int getSignalingPort() {
		return signalingPort;
	}

	public void setSignalingPort(int signalingPort) {
		this.signalingPort = signalingPort;
	}

	public Number getNumber() {
		return this.getDhtId().getId();
	}
}
