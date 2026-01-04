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
/**
 *
 * @author Pierre-Alexandre Losson
 * @author Michiel Meeuwissen (adapted for MMBase)
 * @version $Id$
 * @since MMBase-1.9.2
 */

public class UploadInfo implements java.io.Serializable {
    private static final long serialVersionUID = 0L;

    private final long totalSize;
    private final long startTime = System.currentTimeMillis();

    String errorMessage = null;
    long bytesRead = 0;
    long elapsedTime = -1;

    public enum Status {
        INIT,
        START,
        PROGRESS,
        ERROR,
        DONE;
    }
    Status status = Status.INIT;
    int fileIndex = -1;


    public UploadInfo(long totalSize) {
        this.totalSize = totalSize;
    }

    void error(String mes) {
        status = Status.ERROR;
        errorMessage = mes;
    }
    public String getErrorMessage() {
        return errorMessage;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
        if (status == Status.DONE) {
            elapsedTime = getElapsedTime();
        }
    }

    public long getTotalSize() {
        return totalSize;
    }

    public long getBytesRead() {
        return bytesRead;
    }

    public void setBytesRead(long bytesRead) {
        this.bytesRead = bytesRead;
    }

    public long getElapsedTime() {
        if (elapsedTime < 0) {
            return System.currentTimeMillis() - startTime;
        } else {
            return elapsedTime;
        }
    }
    public void setElapsedTime(long elapsedTime) {
        this.elapsedTime = elapsedTime;
    }

    public boolean isInProgress() {
        return  Status.PROGRESS == status;
    }

    public int getFileIndex() {
        return fileIndex;
    }

    public void setFileIndex(int fileIndex) {
        this.fileIndex = fileIndex;
    }
    public float getFraction() {
        return ((float) bytesRead  / totalSize);
    }
    public int getPercentage() {
        return Math.round(getFraction() * 100);
    }
    @Override
    public String toString() {
        return status + ":" + fileIndex + ":" + bytesRead + "/" + totalSize + " (" + (getPercentage()) + "%,  " + elapsedTime + " ms)";
    }
}
