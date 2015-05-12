package com.lithium.mineraloil.waiters;

public class WaitExpiredException extends RuntimeException {

	private static final long serialVersionUID = 1083658839733553210L;
	
	public WaitExpiredException(String errorMessage) {
		super(errorMessage);
	}
}
