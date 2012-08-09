package net.liveshift.encoder;

import java.util.Date;

import net.liveshift.configuration.Configuration.StreamingProtocol;
import net.liveshift.core.Channel;
import net.liveshift.encoder.DummyEncoder;
import net.liveshift.storage.SegmentAssembler;
import net.liveshift.video.EncoderReceiver;
import net.liveshift.video.PacketData;
import net.liveshift.video.TcpEncoderReceiver;

import org.junit.Test;


public class DummyEncoderTest {

	private DummyEncoder dummyEncoder;
	
	class DummySegmentAssembler extends SegmentAssembler {
		
		int len, last;
		
		@Override
		public boolean putPacket(PacketData packetData) {
			
			//System.err.println("l="+packetData.getVideoData().length);

			len+=packetData.getVideoData().length*8;
			
			int t = (int)(new Date().getTime()/1000);
			
			
			if (last!=t)
			{
				System.out.println(last+ " size:"+len+" bits");
				last=t;
				len=0;
			}
			return true;
		}
	}
	
	class DummyEncoderReceiver extends TcpEncoderReceiver {
		public DummyEncoderReceiver(int encoderPort) {
			super(new Channel("test", (byte)1, 1), new DummySegmentAssembler(), encoderPort);
		}
	
	}
	
	@Test
	public void testDummyEncoder() {
		
		int port = 15350;
		
		this.dummyEncoder = new DummyEncoder();
		
		DummyEncoderReceiver dec = new DummyEncoderReceiver(port);
		dec.start();
		
		dummyEncoder.createNewStreamingSourceFromFile(null, "127.0.0.1", port, StreamingProtocol.UDP);
		dummyEncoder.start();
		
		try {
			Thread.sleep(500000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
