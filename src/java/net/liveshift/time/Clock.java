package net.liveshift.time;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Clock class to be used to keep a clock in a script that can be set to whatever
 * 
 * @author Fabio Victora Hecht, NTP part by Adam Buckley
 * 
*/

public class Clock implements Comparable<Clock> {
	
	private long shift;  //milliseconds that this clock is shifted from RTC
	private boolean paused = false;
	private long pausedOn = 0;
	private long timeZoneDifference;
	
	static final private Clock instance = new Clock(); 

	final private static Logger logger = LoggerFactory.getLogger(Clock.class);
	
	public static Clock getMainClock() {
		return instance;
	}
	
	public Clock() {
		Calendar cal = new GregorianCalendar();
		this.timeZoneDifference = (cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET));
		
		this.setTime(System.currentTimeMillis(), false);  //should be UTC already
		
		//this.debug("set on constructor");
		
	}

	public void setPaused(boolean paused) {
		if (paused) {
			this.pausedOn = this.getTimeInMillis(false);
			this.paused = true;
		}
		else {
			this.paused = false;
			this.setTime(this.pausedOn, false);
			this.pausedOn = 0;
		}
	}
	public boolean isPaused() {
		return this.paused;
	}
	
	/**
	 * 
	 * @param milliseconds
	 * @param boundToCurrent does not allow this clock to be ahead of the current time
	 */
	public void delay(long milliseconds, boolean boundToCurrent) {
		
		//validates for time not ahead of curent time
		
		if (this.paused) {
			
			if (boundToCurrent && this.pausedOn - milliseconds > Clock.getMainClock().shift) {
				this.pausedOn = Clock.getMainClock().shift;
			}
			else {
				this.pausedOn -= milliseconds;
			}

		}
		else {
			if (boundToCurrent && this.shift + milliseconds < Clock.getMainClock().shift) {
				this.shift = Clock.getMainClock().shift;
			}
			else {
				this.shift += milliseconds;
			}
		}
	}
	
	public void setTime(long timeInMilliSeconds, boolean changeToUtc) {
		
		if (changeToUtc) {
			timeInMilliSeconds -= this.timeZoneDifference;
		}
		
		if (this.paused)
			this.pausedOn = timeInMilliSeconds;
		else {
			this.shift = System.currentTimeMillis()-timeInMilliSeconds;
		}
	}
	public long getTimeInMillis(boolean changeToLocalTimeZone) {
		
		long time;
		
		if (this.paused)
			time = this.pausedOn;
		else {
			time = System.currentTimeMillis()-this.shift;
		}

		if (changeToLocalTimeZone) {
			time += this.timeZoneDifference;
		}
		
		if (logger.isDebugEnabled()) logger.debug("got time:" + new Date(time));
		
		return time;
	}
	
	public void reset() {
		this.shift = 0;
		this.paused = false;
		this.pausedOn = 0;
	}

	public boolean syncNtp(String serverName) throws IOException {

		// Send request
		DatagramSocket socket = new DatagramSocket();
		InetAddress address = InetAddress.getByName(serverName);
		byte[] buf = new NtpMessage().toByteArray();
		DatagramPacket packet =
			new DatagramPacket(buf, buf.length, address, 123);
		
		// Set the transmit timestamp *just* before sending the packet
		// ToDo: Does this actually improve performance or not?
		NtpMessage.encodeTimestamp(packet.getData(), 40,
			(System.currentTimeMillis()/1000.0) + 2208988800.0);
		
		socket.send(packet);
		
		
		// Get response
		if (logger.isDebugEnabled()) logger.debug("NTP request sent, waiting for response...");
		
		packet = new DatagramPacket(buf, buf.length);
		socket.setSoTimeout(4000);
		try {
			socket.receive(packet);
		}
		catch (SocketTimeoutException e) {
			logger.error("NTP request timeout. Clock not synchronized.");
			return false;
		}
		
		// Immediately record the incoming timestamp
		double destinationTimestamp =
			(System.currentTimeMillis()/1000.0) + 2208988800.0;
		
		// Process response
		NtpMessage msg = new NtpMessage(packet.getData());
		socket.close();
		
		// Corrected, according to RFC2030 errata
		double roundTripDelay = (destinationTimestamp-msg.originateTimestamp) -
			(msg.transmitTimestamp-msg.receiveTimestamp);
			
		double localClockOffset =
			((msg.receiveTimestamp - msg.originateTimestamp) +
			(msg.transmitTimestamp - destinationTimestamp)) / 2;
		
		// Display response
		if (logger.isDebugEnabled()) logger.debug("NTP server: " + serverName);
		if (logger.isDebugEnabled()) logger.debug(msg.toString());
		
		if (logger.isDebugEnabled()) logger.debug("Dest. timestamp:     " +
			NtpMessage.timestampToString(destinationTimestamp));
		
		if (logger.isDebugEnabled()) logger.debug("Round-trip delay: " +
			new DecimalFormat("0.00").format(roundTripDelay*1000) + " ms");
		
		if (logger.isDebugEnabled()) logger.debug("Local clock offset: " +
			new DecimalFormat("0.00").format(localClockOffset*1000) + " ms");
		
		//sets this clock
		this.delay(-Math.round(localClockOffset*1000), false);
		
		//this.debug("after NTP");
				
		return true;
		
	}
	

	private void debug(String include) {

		if (logger.isDebugEnabled()) logger.debug(include+": "+
				new Date(this.getTimeInMillis(false)).toString() + " UTC, " +
				new Date(this.getTimeInMillis(true)).toString() + " local ("+
				this.getTimeZoneDifferenceSeconds()/60+" min difference)");
			
	}
	private void fullDebug() {

		long curtime = new Date().getTime();
		long clocktimeUTC = this.getTimeInMillis(false);
		long clocktimeLocal = this.getTimeInMillis(true);
		
		System.out.println("sys="+new Date(curtime)+" "+curtime/1000);
		System.out.println("Uclk="+new Date(clocktimeUTC)+" "+clocktimeUTC/1000);
		System.out.println("Udif="+(curtime-clocktimeUTC)/1000+"s");
		System.out.println("Lclk="+new Date(clocktimeLocal)+" "+clocktimeLocal/1000);
		System.out.println("Ldif="+(curtime-clocktimeLocal)/1000+"s");
	}

	public static String formatDatetime(long timeMillis) {
		DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		Date date = new Date(timeMillis);
		return dateFormat.format(date);
	}
	
	public String getDateString() {
		DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
		Date date = new Date(this.getTimeInMillis(false));
		return dateFormat.format(date);
	}
	public String getTimeString() {
        DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        Date date = new Date(this.getTimeInMillis(false));
        return dateFormat.format(date);
    }

	public long getTimeZoneDifferenceSeconds() {
		return this.timeZoneDifference/1000L;
	}

	@Override
	public int compareTo(Clock clock) {
		
		if (this.shift > clock.shift)
			return 1;
		else if (this.shift < clock.shift)
			return -1;
		else
			return 0;
	}
}

