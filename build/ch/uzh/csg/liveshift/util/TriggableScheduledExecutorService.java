package net.liveshift.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.liveshift.time.Clock;


public class TriggableScheduledExecutorService {

	final private static Logger	logger = LoggerFactory.getLogger(TriggableScheduledExecutorService.class);

	final private ScheduledExecutorService	scheduledExecutorService;

	private String	name;

	private long lastRunTimeMs = 0L;
	private Boolean	lastResult;
	private ScheduledFuture<?>	scheduledNextExecution;

	final private long	minFrequencyIfSuccess;
	final private long	minFrequencyIfFailure;
	final private long	maxFrequency;
	final private Clock	clock;
	final private Callable<Boolean> task;
	
	final private ReentrantLock lock = new ReentrantLock();
	
	private boolean	running = true;
	
	final private Runnable nextExecution = new Runnable() {
		
		@Override
		public void run() {
			try {
				if (running) {
					scheduleNowInternal();
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	};

	public TriggableScheduledExecutorService(final Callable<Boolean> task, final long minFrequencyIfSuccess, final long minFrequencyIfFailure, final long maxFrequency, final Clock clock, final String name) {
		this.task = task;
		this.minFrequencyIfSuccess = minFrequencyIfSuccess;
		this.minFrequencyIfFailure = minFrequencyIfFailure;
		this.maxFrequency = maxFrequency;
		this.name = name;
		
		this.clock = clock;
		this.scheduledExecutorService = ExecutorPool.getScheduledExecutorService();
		
		this.lastResult = false;
		
		this.scheduleNowInternal();
	}
	
	public void scheduleNow() {
		
		Runnable runner = new Runnable() {
			@Override
			public void run() {
				try {
					runIt();
				} catch (Exception e) {
					// just so it doesn't die silently if an unhandled exception happened
					logger.error("error running scheduleNow:"+e.getMessage());
					e.printStackTrace();
				}
			}
		};
		
		ExecutorPool.getGeneralExecutorService().execute(runner);
	}

	public void scheduleNowInternal() {

		Runnable runner = new Runnable() {
			@Override
			public void run() {
				try {
					if (logger.isDebugEnabled()) logger.debug("["+name+"] waiting for synchronized");

					synchronized (TriggableScheduledExecutorService.class) {
						if (logger.isDebugEnabled()) logger.debug("["+name+"] in synchronized");

						runIt();
					}
					if (logger.isDebugEnabled()) logger.debug("["+name+"] after synchronized");

				} catch (Exception e) {
					// just so it doesn't die silently if an unhandled exception happened
					logger.error("error running scheduleNow:"+e.getMessage());
					e.printStackTrace();
				}
			}
		};
		
		ExecutorPool.getGeneralExecutorService().execute(runner);
	}
	
	private void runIt() {
		
		if (this.lock.tryLock())
			try {
				
				if (logger.isDebugEnabled()) logger.debug("["+this.name+"] got lock");
				
				if (!running) {
					if (logger.isDebugEnabled()) logger.debug("["+this.name+"] not running, hope to get deleted soon, not executing");
					return;
				}
				
				long currentTimeMs = this.clock.getTimeInMillis(false);
				long minFrequency = this.lastResult?this.minFrequencyIfSuccess:this.minFrequencyIfFailure;
				
				if (logger.isDebugEnabled()) logger.debug("["+this.name+"] last execution returned "+this.lastResult+", so minFrequency="+minFrequency);
				
				if (currentTimeMs-this.lastRunTimeMs >= minFrequency) {
		
					if (logger.isDebugEnabled()) logger.debug("["+this.name+"] executing now, diff="+ (currentTimeMs-this.lastRunTimeMs) +" > "+ minFrequency);
					
					this.lastRunTimeMs = currentTimeMs;
		
					if (this.scheduledNextExecution!=null)
						this.scheduledNextExecution.cancel(false);
		
					try {
						this.lastResult = this.task.call();
					} catch (Exception e) {
						logger.error("["+this.name+"] error calling task: "+e.getMessage());
						
						e.printStackTrace();
					}
					
					this.scheduleNextExecution(currentTimeMs);
					
				}
				else
					if (logger.isDebugEnabled()) logger.debug("["+this.name+"] not executing now, diff="+ (currentTimeMs-this.lastRunTimeMs) +" < "+ minFrequency);
			
			}
			finally {
				this.lock.unlock();
				if (logger.isDebugEnabled()) logger.debug("["+this.name+"] done, lock is unlocked");
			}
		else
			if (logger.isDebugEnabled()) logger.debug("["+this.name+"] was already locked, not executing it");
		
	}
	
	private void scheduleNextExecution (long startTime) {
		long timeItTook = this.clock.getTimeInMillis(false) - startTime;
		long nextMaxTime = this.maxFrequency-timeItTook;
		nextMaxTime = nextMaxTime>0?nextMaxTime:(this.lastResult?this.minFrequencyIfSuccess:this.minFrequencyIfFailure);
		
		if (logger.isDebugEnabled()) logger.debug("["+this.name+"] done execution, took "+timeItTook+" ms, scheduling next execution for "+nextMaxTime+" ms from now");

		if (!this.scheduledExecutorService.isShutdown())
			this.scheduledNextExecution=this.scheduledExecutorService.schedule(this.nextExecution, nextMaxTime, TimeUnit.MILLISECONDS);
		
	}
	
	public synchronized void shutdown() {

		if (logger.isDebugEnabled()) logger.debug("["+this.name+"] shutting down. goodbye folks!");
		
		this.running  = false;
		
		if (this.scheduledNextExecution!=null)
			this.scheduledNextExecution.cancel(true);
		/* enable only if this.scheduledExecutorService is exclusive runner, now it would cancel the whole thing

		if (this.scheduledExecutorService!=null)
			this.scheduledExecutorService.shutdownNow();
			*/
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public String toString() {
		return "["+ this.name+"] lastrun:"+(this.clock.getTimeInMillis(false)-this.lastRunTimeMs)+" ms ago";
	}

}
