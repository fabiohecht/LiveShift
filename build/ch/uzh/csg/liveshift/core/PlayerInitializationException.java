package net.liveshift.core;

public class PlayerInitializationException extends Exception {

	private static final long serialVersionUID = -4163196144942816301L;
	private String message;
	
	public PlayerInitializationException(Exception e) {
		this.initCause(e);
	}
	
}
