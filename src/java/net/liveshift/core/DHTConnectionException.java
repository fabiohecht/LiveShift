package net.liveshift.core;

public class DHTConnectionException extends Exception {

	private static final long serialVersionUID = -7975084950462481315L;

	public DHTConnectionException() {
	}

	public DHTConnectionException(Exception e) {
		this.initCause(e);
	}
}