package com.nifty.http.cache;

import java.util.Collections;
import java.util.Map;

/**
 * Created by BaoRui on 2014/12/11.
 */
public interface Cache {
	/**
	 * Retrieves an entry from the cache.
	 *
	 * @param key Cache key
	 * @return An ll in the event of a cache miss
	 */
	public Entry get(String key);

	/**
	 * Adds or replaces an entry to the cache.
	 *
	 * @param key   Cache key
	 * @param entry Data to store and metadata for cache coherency, TTL, etc.
	 */
	public void put(String key, Entry entry);

	/**
	 * Performs any potentially long-running actions needed to initialize the cache;
	 * will be called from a worker thread.
	 */
	public void initialize();

	/**
	 * Invalidates an entry in the cache.
	 *
	 * @param key        Cache key
	 * @param fullExpire True to fully expire the entry, false to soft expire
	 */
	public void invalidate(String key, boolean fullExpire);

	/**
	 * Removes an entry from the cache.
	 *
	 * @param key Cache key
	 */
	public void remove(String key);

	/**
	 * Empties the cache.
	 */
	public void clear();

	/**
	 * Data and metadata for an entry returned by the cache.
	 */
	public static class Entry {
		/**
		 * The data returned from cache.
		 */
		public byte[] data;

		/**
		 * ETag for cache coherency.
		 */
		public String etag;

		/**
		 * Date of this response as reported by the server.
		 */
		public long lastModified;

		/**
		 * TTL for this record.
		 */
		public long ttl;

		/**
		 * 0 true ,1 false
		 */
		public int wayward;

		//		/**
		//		 * Soft TTL for this record.
		//		 */
		//		public long softTtl;

		/**
		 * Immutable response headers as received from server; must be non-null.
		 */
		public Map<String, String> responseHeaders = Collections.emptyMap();

		/**
		 * True if the entry is expired.
		 */
		public boolean isExpired() {
			return this.ttl < System.currentTimeMillis();
		}

		public boolean isWayward() {
			return this.wayward == 0;
		}

		//		/**
		//		 * True if a refresh is needed from the original data source.
		//		 */
		//		public boolean refreshNeeded() {
		//			return this.softTtl < System.currentTimeMillis();
		//		}
	}

}

