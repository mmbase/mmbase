/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

/**
 * (c) 2000 VPRO
 * @version $Id: RawAudioDef.java,v 1.6 2001-04-25 11:54:57 vpro Exp $
 */
package org.mmbase.util.media.audio;

import java.util.Vector;

import org.mmbase.module.core.MMObjectNode;
import org.mmbase.util.Sortable;
import org.mmbase.util.*;
//import org.mmbase.util.CompareInterface;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;


public class RawAudioDef 
    implements     Sortable, CompareInterface // for fast comparing between items
{
    private static Logger log = Logging.getLoggerInstance(RawAudioDef.class.getName());

    public static final int STATUS_VERZOEK  = 1;
    public static final int STATUS_ONDERWEG = 2;
    public static final int STATUS_GEDAAN   = 3;

    public static final int FORMAT_R5       = 2;
    public static final int FORMAT_WAV      = 3;
    public static final int FORMAT_PCM      = 5;
    public static final int FORMAT_G2       = 6;

    public static final int STORAGE_STEREO          = 1;
    public static final int STORAGE_STEREO_NOBACKUP = 2;
    public static final int STORAGE_MONO            = 3;
    public static final int STORAGE_MONO_NOBACKUP   = 4;

    public static final int MINSPEED        = 16000;
    public static final int MAXSPEED        = 96000;
    public static final int MINCHANNELS     = 1;

    public static final int WAV_MAXSPEED    = 44100;
    public static final int G2_MINSPEED     = 16000;
    public static final int G2_MAXSPEED     = 96000;

    public static final String AUDIO_DIR       = "/data/audio/";

    public int    number;
    public int    otype;        // not nes, but anyway
    public String owner;

    public int    id;
    public int    status;
    public int    format;
    public int    speed;
    public int    channels;
    public String url;
    public String cpu;

    public RawAudioDef( MMObjectNode node )
    {
        if( node != null )
        {
        //  -------------------------------------------    
            number        = node.getIntValue("number");
            otype        = node.getIntValue("otype");
            owner        = node.getStringValue("owner");
        //  -------------------------------------------    
            id            = node.getIntValue("id");
            status        = node.getIntValue("status");
            format        = node.getIntValue("format");
            speed        = node.getIntValue("speed");
            channels    = node.getIntValue("channels");
            url            = node.getStringValue("url");
            cpu            = node.getStringValue("cpu");
        //  -------------------------------------------    
        }
        else
            log.error("RawAudioDef("+node+"): node is null!"); 
    }


    public int compare( Object thisO, Object otherO )
    {
        return ((RawAudioDef)thisO).compare( (RawAudioDef)otherO );
    }

    public int compare( Sortable otherItem )
    {
        RawAudioDef other     = (RawAudioDef) otherItem;
        int         result     = 0;

        if (this.id == other.id )
        {
            if (this.speed == other.speed )
            {
                if (this.channels == other.channels )
                {
                    log.warn("compare( this("+ this.toString() +", other("+other.toString()+"): Comparing two identical items!");
                    result = 0;
                }
                else
                {
                    if( this.channels > other.channels )
                        result = 1;
                    else
                        result = -1;
                }
            }
            else
            {
                if (this.speed > other.speed )
                    result = 1;
                else
                    result = -1;
            }
        }
        else
            if (this.id > other.id )
                result = 1;
            else
                result = -1;
        return result;
    }

    /**
    * Sort vector with RawAudios
    */
    public static Vector sort( Vector unsorted )
    {
        return SortedVector.SortVector(unsorted);
    }

    public String getRealAudioUrl( scanpage sp )
    {
        String result = null;

        if( status == STATUS_GEDAAN )
        {
            if( format == FORMAT_R5 )
            {
                result = "pnm://" + AudioUtils.getBestMirrorUrl( sp, url );        
            }
            else
            if( format == FORMAT_G2 )
            {
                result = "rtsp://" + AudioUtils.getBestMirrorUrl( sp, url );
            }
            else
                log.error("getRealAudioUrl(): For number("+number+"): Unknown format("+format+")");
        }
        else 
            log.error("getRealAudioUrl(): For number("+number+"): Asking url while status("+status+") signals 'not finished yet'!");

        return result;
    }


    public String toString()
    {
        return this.getClass().getName() +"( number("+number+") otype("+otype+") owner("+owner+") id("+id+") status("+status+") format("+format+") speed("+speed+") channels("+channels+") url("+url+") cpu("+cpu+") )";
    }
}
