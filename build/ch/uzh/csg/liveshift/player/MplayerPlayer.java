package net.liveshift.player;

import java.awt.Canvas;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;

import net.liveshift.configuration.Configuration.StreamingProtocol;


public class MplayerPlayer implements Player {
	
	private static final int BUFFER_SIZE_KBYTES	= 512;


	class LineRedirecter extends Thread {
	    /** The input stream to read from. */
	    private InputStream in;
	    /** The output stream to write to. */
	    private OutputStream out;

	    /**
	     * @param in the input stream to read from.
	     * @param out the output stream to write to.
	     * @param prefix the prefix used to prefix the lines when outputting to the logger.
	     */
	    LineRedirecter(InputStream in, OutputStream out) {
	        this.in = in;
	        this.out = out;
	    }

	    @Override
		public void run()
	    {
	        try {
	            // creates the decorating reader and writer
	            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
	            PrintStream printStream = new PrintStream(out);
	            String line;

	            // read line by line
	            while ( (line = reader.readLine()) != null) {
	                printStream.println(line);
	            }
	        } catch (IOException ioe) {
	            ioe.printStackTrace();
	        }
	    }

	}

		
	@Override
	public void initializeLocalStream(String localIpAddress, int playerPort, StreamingProtocol protocol) {
		
		//TODO parameter protocol
		
		try {
			Process mplayerProcess = Runtime.getRuntime().exec("/usr/bin/mplayer -slave -quiet -idle /home/fabio/doc/tmp/test2.ts");
	
			// create the piped streams where to redirect the standard output and error of MPlayer
			// specify a bigger pipesize than the default of 1024
			PipedInputStream  readFrom = new PipedInputStream(BUFFER_SIZE_KBYTES*1024);
			PipedOutputStream writeTo = new PipedOutputStream(readFrom);
			BufferedReader mplayerOutErr = new BufferedReader(new InputStreamReader(readFrom));
	
			// create the threads to redirect the standard output and error of MPlayer
			new LineRedirecter(mplayerProcess.getInputStream(), writeTo).start();
			new LineRedirecter(mplayerProcess.getErrorStream(), writeTo).start();
	
			// the standard input of MPlayer
			PrintStream mplayerIn = new PrintStream(mplayerProcess.getOutputStream());
			
			mplayerIn.print("loadfile /home/fabio/doc/tmp/test2.ts 0");
			mplayerIn.print("\n");
			mplayerIn.flush();
			
			mplayerIn.print("get_property length");
			mplayerIn.print("\n");
			mplayerIn.flush();
			String answer;
			int totalTime = -1;
			try {
			    while ((answer = mplayerOutErr.readLine()) != null) {
			        if (answer.startsWith("ANS_length=")) {
			            totalTime = Integer.parseInt(answer.substring("ANS_length=".length()));
			            break;
			        }
			    }
			}
			catch (IOException e) {
			}
			System.out.println(totalTime);

		}
		catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}

	@Override
	public void pausePlayer() {
		// TODO Auto-generated method stub

	}

	@Override
	public void play() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setCanvas(Canvas jvcanvas) {
		// TODO Auto-generated method stub

	}

	@Override
	public void shutdown() {
		// TODO Auto-generated method stub

	}

	@Override
	public void stopPlayer() {
		// TODO Auto-generated method stub

	}

	public void toggleFullscreen(){
		
	}
	
	@Override
	public float getVolumePercent() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setVolumeUp(int amount) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setVolumeDown(int amount) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setVolumePercent(float volumePercent) {
		// TODO Auto-generated method stub
		
	}
}
