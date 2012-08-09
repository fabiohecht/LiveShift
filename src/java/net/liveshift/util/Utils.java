package net.liveshift.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.Random;
import java.util.Set;

import net.liveshift.core.Channel;
import net.liveshift.core.PeerId;
import net.liveshift.incentive.IncentiveMechanism;

/**
*
* @author Fabio Victora Hecht
* 
*/
public class Utils
{
	static private final Random random = new Random();
	
	static public int getRandomInt() {
		return random.nextInt();
	}
	static public float getRandomFloat() {
		return random.nextFloat();
	}
	public static long getRandomLong() {
		return random.nextLong();
	}
	
	static public String formatDateTime(long datetime) {
		DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date(datetime*1000L);
        return dateFormat.format(date);
	}
	static public int byteArrayToInteger(byte[] byteArray) {
		return byteArrayToInteger(byteArray, 0);
	}
	static public int byteArrayToInteger(byte[] b, int offset) {
		return b[offset]<<24 | (b[offset+1]&0xff)<<16 | (b[offset+2]&0xff)<<8 | (b[offset+3]&0xff);
	}

	public static int integerToByteArray(int i, byte[] out, int offset) {
		out[offset++] = (byte)(i>>24);
		out[offset++] = (byte)(i>>16);
		out[offset++] = (byte)(i>>8);
		out[offset++] = (byte)i;

		return offset;
	}
	static public byte[] integerToByteArray(int i) {
		return new byte[] { (byte)(i>>24), (byte)(i>>16), (byte)(i>>8), (byte)i };
	}
	
	static public long byteArrayToLong(byte[] byteArray) {
		return byteArrayToLong(byteArray,0);
	}
	static public long byteArrayToLong(byte[] b, int offset) {
		return (long)b[offset]<<56 | (b[offset+1]&0xffL)<<48 | (b[offset+2]&0xffL)<<40 | (b[offset+3]&0xffL)<<32 | (b[offset+4]&0xffL)<<24 | (b[offset+5]&0xffL)<<16 | (b[offset+6]&0xffL)<<8 | (b[offset+7]&0xffL);
	}

	public static int longToByteArray(long i, byte[] out, int offset) {
		out[offset++] = (byte)(i>>56);
		out[offset++] = (byte)(i>>48);
		out[offset++] = (byte)(i>>40);
		out[offset++] = (byte)(i>>32);
		out[offset++] = (byte)(i>>24);
		out[offset++] = (byte)(i>>16);
		out[offset++] = (byte)(i>>8);
		out[offset++] = (byte)i;
		
		return offset;
	}
	static public byte[] longToByteArray(long i) {
		return new byte[] { (byte)(i>>56), (byte)(i>>48), (byte)(i>>40), (byte)(i>>32), (byte)(i>>24), (byte)(i>>16), (byte)(i>>8), (byte)i };
	}
	
	static public int md5ToInt(byte[] md5) {
		final int bytesOut = 4;
		final int bytesIn = md5.length;
		
		int output = 0;
		byte curByte = 0;
		for (int i = 0; i < bytesIn; i++) {
			
			curByte ^= md5[i];
			
			if ((i+1)%(bytesIn/bytesOut)==0) {
				output = output << 8;
				output += (curByte & 0x000000FF);
				curByte = 0;
			}
				
		}

		return output;
	}
	
	static public Comparator<PeerId> getReputationComparator(final long timeMillis, final IncentiveMechanism incentiveMechanism) {
		
		return new Comparator<PeerId> () {
			@Override
			public int compare(PeerId peerId1, PeerId peerId2)
			{
				double prio1 = incentiveMechanism.getReputation(peerId1, timeMillis);
				double prio2 = incentiveMechanism.getReputation(peerId2, timeMillis);
				
				return prio2 - prio1 > 0 ? 1 : -1;
			}
		};
	}
	
	
    public static float byteArrayToFloat(byte test[]) {
    	return byteArrayToFloat(test, 0);
    }
	/**
     * convert byte array (of size 4) to float
     * @param test
     * @return
     */
    public static float byteArrayToFloat(byte test[], int offset) {
        int bits = byteArrayToInteger(test, offset);
 
        return Float.intBitsToFloat(bits);
    }
    public static byte[] floatToByteArray(float f) {
        int i = Float.floatToRawIntBits(f);
        return integerToByteArray(i);
    }
	public static int floatToByteArray(float f, byte[] out, int offset) {
        int i = Float.floatToRawIntBits(f);
        return integerToByteArray(i, out, offset);
	}

	public static int byteArrayToMediumInteger(byte[] b, int offset) {  //2 bytes
		return (b[offset+0]&0xff)<<8 | (b[offset+1]&0xff);
	}

	public static String shortenString(String string, int maxLength) {

		if (string.length() > maxLength) {
			return string.substring(0, maxLength-1) + '\u2026';
		}
		else {
			return string;
		}
	}
	
	public static boolean fileExists(String file) {
		if (file==null) {
			return false;
		}
		return new File(file).exists();
	}
	
	static final String baseChars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
	
	public static String convertBase(int number, int base) {
		String out = "";
		long unsignedNumber;
		if (number<0) {			
			unsignedNumber = number;
			unsignedNumber = unsignedNumber & 0x00000000ffffffffL ;
		}
		else {
			unsignedNumber = number;
		}
		
		while (unsignedNumber > 0) {
			int rest = (int) (unsignedNumber % base);
			out = baseChars.charAt(rest) + out;
			unsignedNumber /= base;
		}
		
		return out;
	}
	
	public static String getRandomString(int charCount) {
		String out = "";
		for (int i=0; i<charCount; i++) {
			out += baseChars.charAt(random.nextInt(baseChars.length()));
		}
		return out;
	}
	
	public static byte[] getRandomByteArray(int fileSize) {
		byte[] bytes = new byte[fileSize];
		random.nextBytes(bytes);
		return bytes;
	}
	public static String toString(Set<? extends Object> set) {
		StringBuilder sb = new StringBuilder();
		for (Object elem : set) {
			sb.append(elem.toString()+":");
		}
		return sb.substring(0, sb.length()-1);
	}
	
}

