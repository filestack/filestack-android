package io.filepicker;

import java.util.HashMap;

class CacheElement {
	private Folder data;
	public CacheElement(Folder data) {
		this.data = data;
	}
	
	public Folder getData() {
		return this.data;
	}
}

class DataCache {
	private static DataCache datacache;
	private HashMap<String, CacheElement> cache;

	private DataCache() {
		this.cache = new HashMap<String, CacheElement>();
	}

	public static DataCache getInstance() {
		synchronized (DataCache.class) {
			if (datacache == null)
				return datacache = new DataCache();
			else
				return datacache;
		}
	}
	
	public void clearCache() {
		cache.clear();
	}
	
	public void put(String url, Folder data) {
		cache.put(url, new CacheElement(data));
	}
	

	public Folder get(String url) {
		CacheElement c = cache.get(url);
		if (c == null) {
			return null;
		} else {
			return c.getData();
		}
	}
}
