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

package org.mmbase.streams.thumbnails;

import java.io.File;
import java.util.List;
import org.mmbase.bridge.*;
import org.mmbase.bridge.util.Queries;
import org.mmbase.streams.createcaches.Job;
import org.mmbase.streams.createcaches.Processor;
import org.mmbase.streams.createcaches.Stage;
import org.mmbase.util.functions.NodeFunction;
import org.mmbase.util.functions.Parameter;
import org.mmbase.util.functions.Parameters;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;


/**
 * Function on mediafragments (videofragments really) to make thumbnails of video streams.
 * It uses a timeout to wait for the results of {@link WaitFunction} so it can
 * produce a default image (alias 'default_video_thumbnail') if
 * an exception occurs.
 *
 * @author Michiel Meeuwissen
 * @version $Id$
 */

public class ThumbNailFunction extends NodeFunction<Node> {

    private static final Logger LOG = Logging.getLoggerInstance(ThumbNailFunction.class);

    public final static Parameter<Long> OFFSET = new Parameter<Long>("offset", java.lang.Long.class);
    public final static Parameter<Boolean> WAIT = new Parameter<Boolean>("wait", java.lang.Boolean.class, Boolean.FALSE);
    public final static Parameter<Integer> TIMEOUT = new Parameter<Integer>("timeout", java.lang.Integer.class, 2);
    public final static Parameter[] PARAMETERS = { OFFSET, WAIT, TIMEOUT };
    public ThumbNailFunction() {
        super("thumbnail", PARAMETERS);
    }


    protected static Node getSourceNode(Node node) {
        if (node == null) {
            return null;
        }
        NodeManager videofragments = node.getCloud().getNodeManager("videofragments");
        NodeManager videosources   = node.getCloud().getNodeManager("videostreamsources");
        NodeManager videocaches    = node.getCloud().getNodeManager("videostreamsourcescaches");
        NodeManager  manager = node.getNodeManager();
        if (manager.equals(videofragments) || videofragments.getDescendants().contains(manager)) {
            Node root = node.getFunctionValue("root", null).toNode();
            NodeList related = root.getRelatedNodes("videostreamsources", "related", "destination");
            if (! related.isEmpty()) {
                return related.get(0);
            } else {
                //  simply does not have any sources
                return null;
            }
        } else if (manager.equals(videosources) || videosources.getDescendants().contains(manager)) {
            return node;
        } else if (manager.equals(videocaches) || videocaches.getDescendants().contains(manager)) {
            Node fragment = node.getNodeValue("mediafragment");
            return getSourceNode(fragment);
        }
        LOG.warn("Could not determin source node for " + manager + " " + node);
        return null;
    }
    protected static Node getDefault(Cloud cloud) {
        if (cloud.hasNode("default_video_thumbnail")) {
            return cloud.getNode("default_video_thumbnail");
        } else {
            LOG.warn("No default video thumbnail found with alias 'default_video_thumbnail', e.g. an images node.");
            return null;
        }
    }

    static Node getThumbNail(final Node node, Long offset) {
        Node sourceNode = getSourceNode(node);
        if (sourceNode == null) {
            return getDefault(node.getCloud());
        }
        Job job = Processor.getJob(sourceNode);
        if (job != null && job.getStage().ordinal() < Stage.TRANSCODER.ordinal()) {
            // not yet transcoding, still in stage recognizer
            LOG.service("Not ready transcoding yet, returning default image.");
            return getDefault(node.getCloud());
        }
        long videoLength = node.getLongValue("length");

        if (offset == null) {
            offset = videoLength / 2;
        }

        // round of the offset a bit, otherwise it would be possible to create a thumbnail for every millisecond, which
        // seems a bit overdone....
        if (videoLength > 5000) {
            offset = 1000 * (offset / 1000); // round off to seconds
        } else {
            offset = 100 * (offset / 100);
        }


        Cloud myCloud = node.getCloud().getCloudContext().getCloud("mmbase", "class", null);
        NodeManager thumbs = myCloud.getNodeManager("thumbnails");
        Node thumb = findNode(thumbs, sourceNode, offset);
        if (thumb != null) {
            return thumb;
        } else {
            LOG.service("No thumbnail found for " + sourceNode + " at offset " + offset + ", creating one.");
            synchronized (ThumbNailFunction.class) {
                thumb = findNode(thumbs, sourceNode, offset);
                if (thumb != null) {
                    // created by other thread in the mean time.
                    LOG.service("Thumbnail was created by other thread, returning that one.");
                    return thumb;
                }
                File input = (File) sourceNode.getFunctionValue("file", null).get();
                if (input == null || !input.exists() || !input.canRead() || input.length() == 0) {
                    LOG.debug("Source seems broken, no point in creating a thumbnail");
                    return getDefault(node.getCloud());
                }
                thumb = thumbs.createNode();
                thumb.setValue("id", sourceNode);
                thumb.setValue("time", offset);
                thumb.setValue("height", sourceNode.getIntValue("height"));
                thumb.setValue("width", sourceNode.getIntValue("width"));
                thumb.commit();
                LOG.service("Created thumbnail " + thumb.getNumber());
                return thumb;
            }
        }
    }

    private static Node findNode(NodeManager thumbs, Node sourceNode, long offset) {
        NodeQuery q = thumbs.createQuery();
        Queries.addConstraint(q, q.createConstraint(q.getStepField(thumbs.getField("id")), sourceNode));
        Queries.addConstraint(q, q.createConstraint(q.getStepField(thumbs.getField("time")), offset));
        List<Node> thumbNodes = thumbs.getList(q);
        if (thumbNodes.isEmpty()) {
            return null;
        } else {
            return thumbNodes.get(0);
        }
    }

    @Override
    protected Node getFunctionValue(final Node node, final Parameters parameters) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("params: " + parameters);
        }

        Node thumb = getThumbNail(node, parameters.get(OFFSET));
        int timeout = parameters.get(TIMEOUT);

        if (thumb.isNull("handle") && timeout > 0) {
            LOG.service("Waiting " + timeout + " seconds.");

            long result = WaitFunction.wait(thumb, timeout);

            if (LOG.isDebugEnabled()) {
                LOG.debug("result " + result);
            }

            if (result < 1) {
                if (LOG.isDebugEnabled()) {
                    LOG.service("Returning default thumb, result: " + result );
                }
                return getDefault(thumb.getCloud());
            }
        }

        return thumb;
    }


}
