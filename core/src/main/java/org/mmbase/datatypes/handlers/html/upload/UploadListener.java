/* License:
*   Use this however/wherever you like, just don't blame me if it breaks anything.
*
* Credit:
*   If you're nice, you'll leave this bit:
*
*   Class by Pierre-Alexandre Losson -- http://www.telio.be/blog
*   email : plosson@users.sourceforge.net
*/
package org.mmbase.datatypes.handlers.html.upload;
import javax.servlet.http.HttpServletRequest;
import org.mmbase.datatypes.handlers.html.MultiPart;
import org.mmbase.util.HttpServletRequestUtils;
import org.mmbase.util.logging.Logging;
/**
 *
 * @author Pierre-Alexandre Losson
 * @author Michiel Meeuwissen (adapted for MMBase)
 * @version $Id$
 * @since MMBase-1.9.2
 */

public class UploadListener implements OutputStreamListener {

    public static final String KEY = "org.mmbase.uploadInfo";
    private final HttpServletRequest request;
    private final UploadInfo uploadInfo;
    private final long delay;
    private final long startTime;

    public UploadListener(HttpServletRequest request) {
        this(request, 0);
    }
    public UploadListener(HttpServletRequest request, long debugDelay) {
        this.request = request;
        this.delay = debugDelay;
        this.startTime = System.currentTimeMillis();
        this.uploadInfo = new UploadInfo(HttpServletRequestUtils.getContentLength(request));
    }

    public void start() {
        uploadInfo.fileIndex++;
        updateUploadInfo(UploadInfo.Status.START);
    }

    public void bytesRead(int bytesRead) {
        uploadInfo.bytesRead += bytesRead;
        updateUploadInfo(UploadInfo.Status.PROGRESS);

        if (delay > 0) {
            try {
                Thread.sleep(delay);
                if (Logging.getLoggerInstance(MultiPart.class).isDebugEnabled()) {
                    Thread.sleep(100);
                }

            } catch (InterruptedException e) {
                //
            }
        }
    }

    public void error(String message) {
        updateUploadInfo(UploadInfo.Status.ERROR);
        uploadInfo.error(message);
    }

    public void done() {
        updateUploadInfo(UploadInfo.Status.DONE);
    }

    private long getDelta() {
        return System.currentTimeMillis() - startTime;
    }

    private void updateUploadInfo(UploadInfo.Status status) {
        uploadInfo.setStatus(status);
        request.getSession().setAttribute(KEY, uploadInfo);
    }

}
