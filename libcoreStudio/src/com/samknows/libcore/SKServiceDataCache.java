package com.samknows.libcore;

import java.util.HashMap;


public class SKServiceDataCache {
	private HashMap<Key, CachedValue> map = new HashMap<Key, CachedValue>();
	
	public void put(String device, int type, String responce, String start) {
		map.put(new Key(device, type), new CachedValue(responce, System.currentTimeMillis(), start));
	}
	
	public CachedValue get(String device, int type) {
		return map.get(new Key(device, type));
	}
	
	private class Key {
		public String device;
		public int type;
		
		public Key(String device, int type) {
			super();
			this.device = device;
			this.type = type;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + type;
			result = prime * result
					+ ((device == null) ? 0 : device.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Key other = (Key) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (type != other.type)
				return false;
			if (device == null) {
				if (other.device != null)
					return false;
			} else if (!device.equals(other.device))
				return false;
			return true;
		}
		private SKServiceDataCache getOuterType() {
			return SKServiceDataCache.this;
		}
		
		
	}
	
	public class CachedValue {
		public String responce;
		public long cachedTime;
		public String cachedStart;
		public CachedValue(String responce, long cached, String cachedStart) {
			super();
			this.responce = responce;
			this.cachedTime = cached;
			this.cachedStart = cachedStart;
		}
		
		public boolean isExpired() {
			return System.currentTimeMillis() - cachedTime > SKConstants.CACHE_EXPIRATION;
		}
	}
}
