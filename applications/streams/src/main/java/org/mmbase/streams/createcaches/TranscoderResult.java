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

package org.mmbase.streams.createcaches;

import org.mmbase.bridge.Node;
import org.mmbase.applications.media.State;
import org.mmbase.applications.media.MimeType;

import java.io.*;
import java.net.*;
import org.mmbase.util.logging.*;


/**
 * Container for the result of a JobDefinition This is the result of an actual transcoding.
 * This means that it does have a 'destination' node {@link #getDestination()} and URI {@link #getOut()}.
 *
 * @author Michiel Meeuwissen
 * @version $Id$
 */
class TranscoderResult extends Result {
    private static final Logger LOG = Logging.getLoggerInstance(TranscoderResult.class);
    final Node dest;
    final URI out;
    final File directory;

    TranscoderResult(File directory, JobDefinition def, Node dest, URI in, URI out) {
        super(def, in);
        assert out != null;
        this.dest = dest;
        this.out = out;
        dest.setIntValue("state",  State.REQUEST.getValue());
        dest.commit();
        this.directory = directory;
        LOG.info("Created Result " + this + " " + definition.transcoder.getClass().getName());
    }

    public Node getDestination() {
        return dest;
    }

    public URI getOut() {
        return out;
    }
    public void ready() {
        super.ready();
        if (dest != null) {
            File outFile = new File(directory, dest.getStringValue("url").replace("/", File.separator));
            LOG.debug("Looking at: " + outFile);
            int count = 0;
            while ((!outFile.exists() || outFile.length() <1) && count < 20) {
                LOG.service("Result ready but, but file " + outFile + (outFile.exists() ? "is too small" : "doesn't exists") + ".  Waiting 10 sec. to be sure filesystem is ready (" + count + ")");
                try {
                    Thread.currentThread().sleep(10000);
                    count++;
                } catch (InterruptedException ie) {
                    LOG.info("Interrupted");
                    return;
                }
            }
            dest.setLongValue("filesize", outFile.length());
            if (outFile.length() >= 1) {     // @TODO: there should maybe be other ways to detect if a transcoding failed
                dest.setIntValue("state", State.DONE.getValue());
            } else {
                LOG.warn("Filesize of " + outFile + " < 1, setting " + dest.getNumber() + " to failed");
                dest.setIntValue("state", State.FAILED.getValue());
            }
            if (definition.getLabel() != null && dest.getNodeManager().hasField("label")) {
                dest.setStringValue("label", definition.getLabel());
            }
            dest.commit();
        }
    }

    @Override
    public String toString() {
        if (dest != null) {
            return dest.getNumber() + ":" + out;
        } else {
            return "(NO RESULT:" + definition.toString() + ")";
        }

    }
    public MimeType getMimeType() {
        return new MimeType(getDestination().getStringValue("mimetype"));
    }


}
