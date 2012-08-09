package net.liveshift.signaling;

public interface MessageListener {
	
	/**
	 *	Sets the listener
	 * 
	 * @param incomingMessageHandler
	 */
	public void registerIncomingMessageHandler(IncomingMessageHandler incomingMessageHandler);

	/**
	 * Starts listening
	 * 
	 * @return
	 * @throws Exception 
	 */
	public void startListening() throws Exception;

	/**
	 * Stops listening
	 * 
	 * @return
	 */
	public void stopListening();
}