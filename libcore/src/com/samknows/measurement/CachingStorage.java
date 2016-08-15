package com.samknows.measurement;

import java.util.HashMap;

import android.content.Context;

public class CachingStorage extends Storage{
	private final HashMap<String, Object> cache = new HashMap<>();
	
	private static CachingStorage instance;
	private CachingStorage(Context c) {
		super(c);
	}

	public static final void create(Context ctx) {
		instance = new CachingStorage(ctx);
	}
	
	public static CachingStorage getInstance() {
		return instance;
	}

	@Override
	protected void save(String id, Object data) {
		super.save(id, data);
		cache.put(id, data);
	}

	@Override
	protected Object load(String id) {
		Object result = cache.get(id);
		if (result == null) {
			result = super.load(id);
			cache.put(id, result);
		}
		return result;
	}

	@Override
	protected synchronized void drop(String id) {
		super.drop(id);
		cache.remove(id);
	}
	
	
}
