package com.finalist.newsletter.publisher.cache;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CacheFactory {

	private static ICache cache = null;

	/** */
	/**
	 * get caches'cahe
	 * @param caches
	 * @return 
	 */
	private static final Log logger = LogFactory.getLog(CacheFactory.class);

	public static ICache getCacheInstance(Class caches) {
		if (cache == null) {
			try {
				cache = (ICache) caches.newInstance();
			} 
			catch (InstantiationException e) {
				logger.debug("the point cache is error,caches'paramater must be an ICache'instance");

			}
			catch (IllegalAccessException e) {
				logger.debug("the point cache is error,caches'paramater must be an ICache'instance");
			}
		}
		return cache;
	}

	public static ICache getDefaultCache() {		
		return getDefaultCache();
	}
	
	public static ICache getDefaultCache(long time) {
		if (cache == null) {
			cache = new DefaultCache(time);
		} 
		else if (!(cache instanceof DefaultCache)) {
			cache = new DefaultCache(time);
		}
		return cache;
	}

}
