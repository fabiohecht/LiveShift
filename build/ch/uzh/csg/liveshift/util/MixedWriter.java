package net.liveshift.util;

import java.io.IOException;
import java.io.OutputStream;

public class MixedWriter {
	
	final private OutputStream out;
	final private String charsetName;
	
	public MixedWriter(OutputStream out, String charsetName) {
		this.out = out;
		this.charsetName = charsetName;
	}
	
	public void write(String str) throws IOException {
		this.write(str.getBytes(this.charsetName));
	}
	
	public void write(byte[] bytes) throws IOException {
		out.write(bytes);
	}
	
	public void close() throws IOException {
		this.out.close();
	}
	
	public void flush() throws IOException {
		this.out.flush();
	}
	
}
