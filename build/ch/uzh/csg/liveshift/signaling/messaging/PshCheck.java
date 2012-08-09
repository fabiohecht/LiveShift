/*
 * This file is part of FileSharing-TomP2P.
 * 
 * FileSharing-TomP2P is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 * 
 * FileSharing-TomP2P is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with FileSharing-TomP2P. If not, see <http://www.gnu.org/licenses/>.
 */
package net.liveshift.signaling.messaging;

import java.math.BigInteger;

public class PshCheck<K>
{
	final private K intermediate;
	final private K target;
	final private float amount;
	private byte[] signatureIntermediate = new byte[128];

	public PshCheck(K intermediate, K target, float amount)
	{
		this.intermediate = intermediate;
		this.target = target;
		this.amount = amount;
	}
	public K getIntermediate()
	{
		return intermediate;
	}

	public K getTarget()
	{
		return target;
	}

	public float getAmount()
	{
		return amount;
	}

	public void setSignature(byte[] signature) {
		this.signatureIntermediate = signature;
	}
	public byte[] getSignature()
	{
		return signatureIntermediate;
	}

	public boolean verify(BigInteger publicKeyOfIntermediate)
	{
		return true;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder("I (").append(intermediate);
		sb.append(") T (");
		sb.append(target).append(") A (").append(this.amount).append(")");
		return sb.toString();
	}
}
