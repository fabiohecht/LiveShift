package net.liveshift.logging;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.liveshift.util.ExecutorPool;

public class LiveShiftMemoryHandler extends Handler {
	
	final private static Logger logger = LoggerFactory.getLogger(LiveShiftMemoryHandler.class);

	final private ConcurrentLinkedQueue<LogRecord> logEntriesToSend = new ConcurrentLinkedQueue<LogRecord>();
	final private LogClient logClient;
	private final ScheduledFuture<?> scheduledTask;

	public LiveShiftMemoryHandler(final long logIntervalMillis,final LogClient logClient) {
		this.logClient=logClient;
		this.scheduledTask = ExecutorPool.getScheduledExecutorService().scheduleAtFixedRate(logSenderRunner, logIntervalMillis, logIntervalMillis, TimeUnit.MILLISECONDS);
	}
	
	Runnable logSenderRunner = new Runnable() {
		
		@Override
		public void run() {
			try {
				sendLogEntriesToServer();

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				
				logger.error("error sending log entries: "+ e.getMessage());
			}
		}
	};
	
	private void sendLogEntriesToServer() {
	
		synchronized (this) {
			
			StringBuilder entriesToSendNow = new StringBuilder();
			int logEntryCount = 0;
			LogRecord entry = null;
			
			while (null != (entry = logEntriesToSend.poll())) {
				entriesToSendNow.append(new LogFormatter().format(entry));
				logEntryCount++;
			}
			
			if (!entriesToSendNow.toString().trim().equals("")) {
				try {
					logClient.sendLog(entriesToSendNow.toString());
					
					if (logger.isDebugEnabled()) {
						logger.debug(logEntryCount + " log entries sent to server");
					}
				} catch (Exception e) {
					e.printStackTrace();
					logger.error(e.getMessage());
				}				
			}
		}
	}
	
	
	@Override
	public void close() throws SecurityException {
		if (logger.isDebugEnabled()) {
			logger.debug("close()");
		}
		this.scheduledTask.cancel(false);
		this.flush();
	}

	@Override
	public void flush() {
		if (logger.isDebugEnabled()) {
			logger.debug("flush()");
		}
		sendLogEntriesToServer();
	}
	
	@Override
	public void publish(LogRecord record) {
		if (record.getLevel().equals(Level.INFO) || record.getLevel().equals(Level.SEVERE) || record.getLevel().equals(Level.WARNING)) {
			logEntriesToSend.add(record);
		}
	}
}
