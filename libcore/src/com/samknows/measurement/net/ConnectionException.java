package com.samknows.measurement.net;

public class ConnectionException extends Exception {
	/**
	 * @param detailMessage
	 */
	public ConnectionException(String detailMessage) {
		super(detailMessage);
	}

	/**
	 * @param throwable
	 */
	public ConnectionException(Throwable throwable) {
		super(throwable);
	}

	/**
	 * @param detailMessage
	 * @param throwable
	 */
	public ConnectionException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}
}
