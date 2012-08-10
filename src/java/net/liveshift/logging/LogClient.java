package net.liveshift.logging;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.zip.GZIPOutputStream;

import net.liveshift.time.Clock;
import net.liveshift.util.MixedWriter;
import net.liveshift.util.Utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LogClient {
	
	final private static Logger logger = LoggerFactory.getLogger(LogClient.class);

	private static final String METHOD = "zip";  //implemented: zip and urlencoded
	private static final int BYTES_AT_A_TIME = 1024;

	final private String logServerHost;
	final private int logServerPort;
	final private String userId;

	public LogClient(final String logServerAddress, final int logServerPort, final String userId) {
		this.logServerHost = logServerAddress;
		this.logServerPort = logServerPort;
		this.userId = userId;
	}

	public void sendLog(String log) {
		if (!log.equals("")) {

			Socket socket = null;

			try {
				
				socket = this.getNewSocket();
				
				if (socket==null) {
					throw new RuntimeException("unable to connect to log server: "+this.logServerHost+":"+logServerPort);
				}
				
				//sends data				
				OutputStream stream = new BufferedOutputStream(socket.getOutputStream());
				try {
					MixedWriter writer = new MixedWriter(stream, "UTF8");
					
					String path = userId;
					writer.write("POST " + path + " HTTP/1.0\n");
					
					if (METHOD.equals("urlencoded")) {
						writer.write("Content-Type: application/x-www-form-urlencoded\n");
						
						String utf8data = URLEncoder.encode(log, "UTF-8");
						writer.write("Content-Length: " + utf8data.length() + "\n\n");
						
						writer.write(utf8data);
						logger.debug("wrote: " + utf8data.length()+" bytes");
					}
					else if (METHOD.equals("zip")) {
						writer.write("Content-Type: application/gzip\n");
						
						byte[] bytes = log.getBytes("UTF-8");
		
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						GZIPOutputStream zipo = new GZIPOutputStream(baos);
						
						zipo.write(bytes);
						
						zipo.finish();
						if (logger.isDebugEnabled()) {
							logger.debug("sending "+bytes.length+" bytes as "+baos.size()+" bytes");
						}
						writer.write("Content-Length: "+baos.size()+"\n\n");
						writer.write(baos.toByteArray());
					}
					else {
						logger.error("unknown logging method: "+METHOD);
					}
					
				} catch (Exception e) {
					logger.error(e.getMessage());
					e.printStackTrace();
				}
				finally {
					try {
						stream.flush();
						stream.close();
					} catch (Exception e) {
						logger.error(e.getMessage());
						e.printStackTrace();
					}
				}
			} catch (UnknownHostException e) {
				logger.error("Can't send log to server, unknown host "+logServerHost);
			} catch (Exception e) {
				logger.error(e.getMessage());
				e.printStackTrace();
			}
			finally {
				try {
					if (socket!=null) {
						socket.close();
					}
				} catch (IOException e) {
					logger.error(e.getMessage());
					e.printStackTrace();
				}
			}
		}
	}
	
	public int getUploadRateKbytePerSecond() {
		
		float rates = 0, lastRate;
		int count=0; 
		Long lastTime = 0L;
		int fileSize=2*1024*1024;
		while (lastTime != null && lastTime < 5000L && fileSize <= 50*1024*1024) {
			lastTime = getUploadIntervalMillis(fileSize);
			
			lastRate = fileSize/(lastTime/1000F)/1024;
			if (logger.isDebugEnabled()) {
				logger.debug("file size "+fileSize+" took "+lastTime+" ms, rate="+lastRate);
			}
			rates += lastRate;
			count++;
			fileSize*=5;
		}
		
		if (logger.isDebugEnabled()) {
			logger.debug("on average "+(rates/count));
		}

		return (int) (rates/count);
	}
	
	public long getUploadIntervalMillis(int fileSizeByte) {
		//send data until rate stabilizes
		
		Socket socket = null;
		
		try {
			
			socket = this.getNewSocket();
			
			if (socket==null) {
				logger.error("unable to connect to log server");
				throw new RuntimeException("unable to connect to log server");
			}
			
			//sends data
			OutputStream stream = socket.getOutputStream();
			try {
				MixedWriter writer = new MixedWriter(stream, "UTF8");
				
				writer.write("POST _speedtest HTTP/1.0\n");
				writer.write("Content-Type: text/plain\n");
				writer.write("Content-Length: " + fileSizeByte + "\n\n");
				
				long timeMillis = Clock.getMainClock().getTimeInMillis(false);
				int writtenBytes=0, bytesToWrite;				
				byte[] original = Utils.getRandomByteArray(BYTES_AT_A_TIME);
				while (writtenBytes<fileSizeByte) {
					bytesToWrite = Math.min(BYTES_AT_A_TIME, fileSizeByte - writtenBytes);
					if (bytesToWrite==BYTES_AT_A_TIME) {
						writer.write(original);
					}
					else {
						byte[] smaller = new byte[bytesToWrite];
						System.arraycopy(original, 0, smaller, 0, bytesToWrite);
						writer.write(smaller);
					}
					writtenBytes += bytesToWrite;
				}
				writer.flush();
				timeMillis = Clock.getMainClock().getTimeInMillis(false)-timeMillis;
				
				logger.debug("wrote: " + fileSizeByte+" bytes in "+timeMillis+" millis");
				
				return timeMillis;
				
			} catch (IOException e) {
				logger.error(e.getMessage());
				e.printStackTrace();
			}
			finally {
				try {
					if (stream!=null) {
						stream.flush();
						stream.close();
					}
				} catch (Exception e) {
					logger.error(e.getMessage());
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			logger.error("exception sending log data: "+e.getMessage());
			e.printStackTrace();
			throw new RuntimeException("exception sending log data: "+e.getMessage());
		}
		finally {
			try {
				if (socket!=null) {
					socket.close();
				}
			} catch (IOException e) {
				logger.error("exception closing connection: "+e.getMessage());
				e.printStackTrace();
				throw new RuntimeException("exception closing connection: "+e.getMessage());
			}
		}
		
		return -1L;  //should never reach here
	}

	private Socket getNewSocket() throws UnknownHostException {

		// Create a socket to the host
		InetAddress addr = InetAddress.getByName(logServerHost);
		
		if (logger.isDebugEnabled()) {
			logger.debug("log client: use host " + logServerHost+ " port " + logServerPort);
		}
		
		Socket socket = null;
		try {
			SocketAddress sockaddr = new InetSocketAddress(addr, logServerPort);
			socket = new Socket();
			socket.connect(sockaddr);
		} catch (Exception e) {
			logger.error("unable to connect to " + logServerHost + ":" + logServerPort+", "+e.getMessage());
			return null;
		}
		
		if (socket == null || !socket.isConnected()) {
			logger.error("unable to connect to log server: " + logServerHost + ":" + logServerPort);
			return null;
		}
		
		return socket;
		
	}
}
