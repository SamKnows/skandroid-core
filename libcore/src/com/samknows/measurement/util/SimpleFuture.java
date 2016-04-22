package com.samknows.measurement.util;

import android.support.annotation.NonNull;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SimpleFuture<T> implements Future<T>{
	private T result;
	
	public SimpleFuture(T result) {
		super();
		this.result = result;
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return false;
	}

	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public boolean isDone() {
		return true;
	}

	@Override
	public T get() throws InterruptedException, ExecutionException {
		return result;
	}

	@Override
	public T get(long timeout, @NonNull TimeUnit unit) throws InterruptedException,
			ExecutionException, TimeoutException {
		return result;
	}
	
	

}
