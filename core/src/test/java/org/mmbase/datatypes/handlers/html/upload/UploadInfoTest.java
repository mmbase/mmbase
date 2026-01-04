package org.mmbase.datatypes.handlers.html.upload;

import junit.framework.TestCase;
import org.junit.Assert;

public class UploadInfoTest extends TestCase {

    public void testInfo() {
        UploadInfo info = new UploadInfo(3973369539L);
        info.setStatus(UploadInfo.Status.PROGRESS);
        info.bytesRead = 100000000;
        float s = info.getFraction();
        Assert.assertEquals(0.025167555f,s, 0.00001f);

        Assert.assertEquals("PROGRESS:-1:100000000/3973369539 (2%,  -1 ms)",info.toString());

    }
}
