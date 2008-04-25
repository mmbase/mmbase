/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module;

import org.mmbase.util.ResourceWatcher;

/**
 * This Reloadable Module extension reloads its configuration and calls reload, automaticly if the
 * module XML changes.
 *
 * @author   Michiel Meeuwissen
 * @since    MMBase-1.8
 * @version  $Id: WatchedReloadableModule.java,v 1.4 2006-06-19 08:38:59 michiel Exp $
 */
public abstract class WatchedReloadableModule extends ReloadableModule {

    private ResourceWatcher configWatcher = new ResourceWatcher() {
            public void onChange(String resource) {
                // resource parameter can be ignored, because it inconveniently contains ".xml".
                if (reloadConfiguration(getName())) {
                    reload();
                }
            }
        };

    /**
     * {@inheritDoc} 
     * On the onload of a reloadable module, a filewatcher is started. You should call
     * super.onload if you need to override this.
     */
    public void onload() {
        configWatcher.setDelay(10 * 1000);
        configWatcher.start();
        configWatcher.add("modules/" +  getName() + ".xml");
    }


}
