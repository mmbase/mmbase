/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

 */
package org.mmbase.util.images;

import java.util.Map;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.io.*;

import org.mmbase.module.core.*;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * An ImageConversionRequest Processor is a daemon Thread which can handle image transformations. Normally a few of these are started.
 * Each one contains a BlockingQueue of Image request jobs it has to do, which is constantly watched for new jobs.
 *
 * @author Rico Jansen
 * @author Michiel Meeuwissen
 * @version $Id: ImageConversionRequestProcessor.java,v 1.2 2007-06-13 18:54:55 nklasens Exp $
 * @see    ImageConversionRequest
 */
public class ImageConversionRequestProcessor implements Runnable {

    private static final Logger log = Logging.getLoggerInstance(ImageConversionRequestProcessor.class);
    private static int idCounter =0;
    private final int processorId;

    private final ImageConverter convert;
    private final BlockingQueue<ImageConversionRequest> queue;
    private final Map<ImageConversionReceiver, ImageConversionRequest> table;

    /**
     * @javadoc
     */
    public ImageConversionRequestProcessor(ImageConverter convert, BlockingQueue<ImageConversionRequest> queue, 
                                           Map<ImageConversionReceiver, ImageConversionRequest> table) {
        this.convert = convert;
        this.queue = queue;
        this.table = table;
        processorId = idCounter++;
        start();
    }

    /**
     * Starts the thread for this ImageRequestProcessor.
     */
    protected void start() {
        MMBaseContext.startThread(this, "ImageConvert[" + processorId +"]");
    }

    // javadoc inherited (from Runnable)
    public void run() {
        MMBase mmbase = MMBase.getMMBase();
        while (!mmbase.isShutdown()) {
            try {
                log.debug("Waiting for request");
                ImageConversionRequest req = queue.take();
                log.debug("Starting request");
                processRequest(req);
                log.debug("Done with request");
            } catch (InterruptedException ie) {
                log.debug(Thread.currentThread().getName() +" was interrupted.");
                continue;
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Takes an ImageConversionRequest object, and performs the conversion.
     * @param req The ImageConversionRequest wich must be executed.
     */
    private void processRequest(ImageConversionRequest req) {

        InputStream inputPicture = req.getInput();
        ImageConversionReceiver rec = req.getReceiver();

        try {
            if (inputPicture == null) {
                if (log.isDebugEnabled()) log.debug("processRequest : input is empty : " + req);
                // no node gets created, so node remains 'null'.
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("processRequest : Converting : " + req );
                }

                List<String> params = req.getParams();
                try {

                    int length = convert.convertImage(inputPicture, req.getInputFormat(), rec.getOutputStream(), params);
                    if (length > 0) {
                        rec.setSize(length);
                        if (rec.wantsDimension()) {
                            Dimension dim = Factory.getImageInformer().getDimension(rec.getInputStream());
                            rec.setDimension(dim);
                        }
                        rec.ready();
                    } else {
                        log.warn("processRequest(): Convert problem params : " + params);
                    }
                } catch (java.io.IOException ioe) {
                    log.error(ioe);
                }
            }
        } finally {
            synchronized (table){
                if (log.isDebugEnabled()) {
                    log.debug("Setting output " + req + " (" + req.count() + " times requested now)");
                }
                req.ready();
                table.remove(rec);
            }
        }
    }
}
