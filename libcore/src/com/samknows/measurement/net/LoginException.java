package com.samknows.measurement.net;

public class LoginException extends Exception {
	/**
	 * @param detailMessage
	 */
	public LoginException(String detailMessage) {
		super(detailMessage);
	}

	/**
	 * @param throwable
	 */
	public LoginException(Throwable throwable) {
		super(throwable);
	}

	/**
	 * @param detailMessage
	 * @param throwable
	 */
	public LoginException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}
}
