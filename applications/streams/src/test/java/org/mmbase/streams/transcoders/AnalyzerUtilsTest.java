/*

This file is part of the MMBase Streams application,
which is part of MMBase - an open source content management system.
    Copyright (C) 2009 André van Toly, Michiel Meeuwissen

MMBase Streams is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

MMBase Streams is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with MMBase. If not, see <http://www.gnu.org/licenses/>.

*/

package org.mmbase.streams.transcoders;

import org.mmbase.util.externalprocess.ProcessException;
import java.io.*;
import org.junit.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;

/**
 * Low level tests of Regexps and stuff.
 * @author Michiel Meeuwissen
 */

public class AnalyzerUtilsTest {




    Transcoder getFFMpegTranscoder() {
        FFMpegRecognizer rec = new FFMpegRecognizer();
        Transcoder trans = new RecognizerTranscoder(rec);
        return trans;
    }


    @Test
    public void duration() throws Exception {
        File testFile = new File(System.getProperty("user.dir"), "samples" + File.separator + "basic.mp4");
        assumeTrue(testFile.exists());

        AnalyzerTestLogger test = new AnalyzerTestLogger() {
                @Override
                 protected void log(String s) {
                    if (util.duration(s, source, destination)) {
                        this.success = true;
                    }
                }
        };
        try {
            getFFMpegTranscoder().transcode(testFile.toURI(), null, test);
        } catch (ProcessException io) {
            assumeTrue(false);
        }
        assertTrue(test.success);

    }

    @Test
    public void unknown() throws Exception {
        File testFile = new File(System.getProperty("user.dir"), "samples" + File.separator + "unknown.wav");
        assumeTrue(testFile.exists());

        AnalyzerTestLogger test = new AnalyzerTestLogger() {
                @Override
                protected void log(String s) {
                    if (util.unsupported(s, source, destination)) {
                        System.out.println("UNSUPPORT " + s);
                        this.success = true;
                    } else {
                        System.out.println("SUPPORT " + s);
                    }
                }
            };
        try {
            getFFMpegTranscoder().transcode(testFile.toURI(), null, test);
            assertTrue(test.success);
        } catch (ProcessException pe) {
            assumeTrue(false);
        }

    }

    @Test
    public void known() throws Exception {
        File testFile = new File(System.getProperty("user.dir"), "samples" + File.separator + "basic.wav");
        assumeTrue(testFile.exists());

        AnalyzerTestLogger test = new AnalyzerTestLogger() {
                @Override
                protected void log(String s) {
                    if (util.unsupported(s, source, destination)) {
                        this.success = true;
                    }
                }
            };
        try {
            getFFMpegTranscoder().transcode(testFile.toURI(), null, test);
            assertFalse(test.success);
        } catch (ProcessException pe) {
            assumeTrue(false);
        }

    }


    @Test
    public void unsupported() throws Exception {
        File testFile = new File(System.getProperty("user.dir"), "samples" + File.separator + "unsupported.rm");
        assumeTrue(testFile.exists());

        AnalyzerTestLogger test = new AnalyzerTestLogger() {
                @Override
                protected void log(String s) {
                    if (util.unsupported(s, source, destination)) {
                        this.success = true;
                    }
                }
            };
        try {
            getFFMpegTranscoder().transcode(testFile.toURI(), null, test);
            assertTrue(test.success);
        } catch (ProcessException pe) {
            assumeTrue(false);
        }

    }


}


