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
package net.liveshift.util;
import java.io.Serializable;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

/**
 * A simple Bloom Filter (see http://en.wikipedia.org/wiki/Bloom_filter) that
 * uses java.util.Random as a primitive hash function, and which implements
 * Java's Set interface for convenience.
 * 
 * Only the add(), addAll(), contains(), and containsAll() methods are
 * implemented. Calling any other method will yield an
 * UnsupportedOperationException.
 * 
 * This code may be used, modified, and redistributed provided that the author
 * tag below remains intact.
 * 
 * @author Ian Clarke <ian@uprizer.com>
 * @author Thomas Bocek <bocek@ifi.uzh.ch> 15.1.2009: Added methods to get and
 *         create a SimpleBloomFilter from existing data. The data can be either
 *         a BitSet or a bye[].
 * 
 * @param <E> The type of object the BloomFilter should contain
 */
public class SimpleBloomFilter<E> implements Set<E>, Serializable
{
	private static final long serialVersionUID = 3527833617516722215L;
	private final int k;
	private final BitSet bitSet;
	private final int bitArraySize, expectedElements;

	/**
	 * Construct an empty SimpleBloomFilter. You must specify the number of bits
	 * in the Bloom Filter, and also you should specify the number of items you
	 * expect to add. The latter is used to choose some optimal internal values
	 * to minimize the false-positive rate (which can be estimated with
	 * expectedFalsePositiveRate()).
	 * 
	 * @param bitArraySize The number of bits in the bit array (often called 'm'
	 *        in the context of bloom filters).
	 * @param expectedElements The typical number of items you expect to be
	 *        added to the SimpleBloomFilter (often called 'n').
	 */
	public SimpleBloomFilter(int bitArraySize, int expectedElements)
	{
		this.bitArraySize = bitArraySize;
		this.expectedElements = expectedElements;
		this.k = (int) Math.ceil((bitArraySize / expectedElements) * Math.log(2.0));
		bitSet = new BitSet(bitArraySize);
	}

	/**
	 * Constructs a SimpleBloomFilter out of existing data. You must specify the
	 * number of bits in the Bloom Filter, and also you should specify the
	 * number of items you expect to add. The latter is used to choose some
	 * optimal internal values to minimize the false-positive rate (which can be
	 * estimated with expectedFalsePositiveRate()).
	 * 
	 * @param bitArraySize The number of bits in the bit array (often called 'm'
	 *        in the context of bloom filters).
	 * @param expectedElements he typical number of items you expect to be added
	 *        to the SimpleBloomFilter (often called 'n').
	 * @param rawBitArray The data that will be used in the backing BitSet
	 */
	public SimpleBloomFilter(int bitArraySize, int expectedElements, byte[] rawBitArray)
	{
		this.bitArraySize = bitArraySize;
		this.expectedElements = expectedElements;
		this.k = (int) Math.ceil((bitArraySize / expectedElements) * Math.log(2.0));
		bitSet = fromByteArray(new BitSet(bitArraySize), rawBitArray);
	}

	/**
	 * Constructs a SimpleBloomFilter out of existing data. You must specify the
	 * number of bits in the Bloom Filter, and also you should specify the
	 * number of items you expect to add. The latter is used to choose some
	 * optimal internal values to minimize the false-positive rate (which can be
	 * estimated with expectedFalsePositiveRate()).
	 * 
	 * @param bitArraySize The number of bits in the bit array (often called 'm'
	 *        in the context of bloom filters).
	 * @param expectedElements he typical number of items you expect to be added
	 *        to the SimpleBloomFilter (often called 'n').
	 * @param bitSet The data that will be used in the backing BitSet
	 */
	public SimpleBloomFilter(int bitArraySize, int expectedElements, BitSet bitSet)
	{
		this.bitArraySize = bitArraySize;
		this.expectedElements = expectedElements;
		this.k = (int) Math.ceil((bitArraySize / expectedElements) * Math.log(2.0));
		this.bitSet = bitSet;
	}

	/**
	 * Calculates the approximate probability of the contains() method returning
	 * true for an object that had not previously been inserted into the bloom
	 * filter. This is known as the "false positive probability".
	 * 
	 * @return The estimated false positive rate
	 */
	public double expectedFalsePositiveProbability()
	{
		return Math.pow((1 - Math.exp(-k * (double) expectedElements / bitArraySize)), k);
	}

	/*
	 * @return This method will always return false
	 * 
	 * @see java.util.Set#add(java.lang.Object)
	 */
	@Override
	public boolean add(E o)
	{
		Random r = new Random(o.hashCode());
		for (int x = 0; x < k; x++)
		{
			bitSet.set(r.nextInt(bitArraySize), true);
		}
		return false;
	}

	/**
	 * @return This method will always return false
	 */
	@Override
	public boolean addAll(Collection<? extends E> c)
	{
		for (E o : c)
		{
			add(o);
		}
		return false;
	}

	/**
	 * Clear the Bloom Filter
	 */
	@Override
	public void clear()
	{
		for (int x = 0; x < bitSet.length(); x++)
		{
			bitSet.set(x, false);
		}
	}

	/**
	 * @return False indicates that o was definitely not added to this Bloom
	 *         Filter, true indicates that it probably was. The probability can
	 *         be estimated using the expectedFalsePositiveProbability() method.
	 */
	@Override
	public boolean contains(Object o)
	{
		Random r = new Random(o.hashCode());
		for (int x = 0; x < k; x++)
		{
			if (!bitSet.get(r.nextInt(bitArraySize)))
				return false;
		}
		return true;
	}

	@Override
	public boolean containsAll(Collection<?> c)
	{
		for (Object o : c)
		{
			if (!contains(o))
				return false;
		}
		return true;
	}

	/**
	 * Not implemented
	 */
	@Override
	public boolean isEmpty()
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * Not implemented
	 */
	@Override
	public Iterator<E> iterator()
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * Not implemented
	 */
	@Override
	public boolean remove(Object o)
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * Not implemented
	 */
	@Override
	public boolean removeAll(Collection<?> c)
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * Not implemented
	 */
	@Override
	public boolean retainAll(Collection<?> c)
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * Not implemented
	 */
	@Override
	public int size()
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * Not implemented
	 */
	@Override
	public Object[] toArray()
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * Not implemented
	 */
	@Override
	public <T> T[] toArray(T[] a)
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * Returns the bitset that backs the bloom filter
	 * 
	 * @return bloom filter as a bitset
	 */
	public BitSet getBitSet()
	{
		return bitSet;
	}

	/**
	 * Sets bits in the bitset from a byte[]
	 * 
	 * @param bits The bits that will be filled
	 * @param bytes The data
	 * @return The filled bitset. It is the same object as passed as argument.
	 */
	private BitSet fromByteArray(BitSet bits, byte[] bytes)
	{
		for (int i = 0; i < bytes.length * 8; i++)
		{
			if ((bytes[bytes.length - i / 8 - 1] & (1 << (i % 8))) > 0)
			{
				bits.set(i);
			}
		}
		return bits;
	}

	/**
	 * Returns a byte array of at least length 1. The most significant bit in
	 * the result is guaranteed not to be a 1 (since BitSet does not support
	 * sign extension). The byte-ordering of the result is big-endian which
	 * means the most significant bit is in element 0. The bit at index 0 of the
	 * bit set is assumed to be the least significant bit.
	 * 
	 * @return a byte array representation of the bitset
	 */
	public byte[] toByteArray()
	{
		if (bitSet.length() == 0)
			return new byte[] {};
		byte[] bytes = new byte[bitSet.length() / 8 + 1];
		for (int i = 0; i < bitSet.length(); i++)
		{
			if (bitSet.get(i))
			{
				bytes[bytes.length - i / 8 - 1] |= 1 << (i % 8);
			}
		}
		return bytes;
	}
}