package com.finalist.newsletter.publisher.cache;

import java.util.Iterator;
import java.util.Map;
import java.util.TimerTask;

public class CacheFreshTask extends TimerTask{
	 private DefaultCache cache;
	 
	    public CacheFreshTask(DefaultCache cache) {
	        this.cache = cache;
	    }

	    public void run() {
	        synchronized (cache.getDatas()) {
	            Iterator<Map.Entry<String, CacheInfo>> iterator
	                = cache.getDatas().entrySet().iterator();	            
	            while (iterator.hasNext()) {
	                Map.Entry<String, CacheInfo> entry = iterator.next();
	                CacheInfo ci = entry.getValue();
	                if (ci.getTotalSeconds() != ICache.Forever) {
	                    ci.setSecondsRemain(ci.getSecondsRemain() - 1);  
	                    if (ci.getSecondsRemain() <= 0) {
	                        iterator.remove();
	                    }
	                }
	            }
	        }
	    }

}
