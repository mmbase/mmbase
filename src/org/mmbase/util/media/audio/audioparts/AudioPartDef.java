/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

/*
	(c) 2000 VPRO
*/

package org.mmbase.util.media.audio.audioparts;

import	java.util.*; 
import java.net.URLEncoder;

import	org.mmbase.module.core.*;
import	org.mmbase.util.*;
import	org.mmbase.util.media.*;
import	org.mmbase.util.media.audio.*;

public class AudioPartDef
{
	private String 	classname	= getClass().getName();
	private boolean	debug		= true;
	private void	debug( String msg ) { System.out.println( classname +":"+ msg ); }
	
	public int		number;
	public int		otype;		// not nes, but anyway
	public String	owner;

	public String	title;
	public String	subtitle;
	public int		source;
	public int		playtime;
	public String	intro;
	public String	body;
	public int		storage;

	public String	starttime;
	public String	stoptime;

	public Vector		rawaudios;		// all the rawaudios connected to this node
	public RawAudioDef	rawaudio;		// the best for the user in speed/channels.

// ----------------------------------------------------------------------------

	public AudioPartDef()
	{ }

	public AudioPartDef( MMBase mmbase, int number )
	{
		if( !setparameters( mmbase, number ) )
			debug("AudioPartDef("+mmbase+","+number+"): ERROR: Not initialised, something went wrong");
	}

	public boolean setparameters( MMBase mmbase, int number )
	{
		boolean result = false; 
		if( mmbase != null )
		{
			if( number > 0 )
			{
				MMObjectBuilder builder = mmbase.getMMObject("audioparts");
				if( builder != null )
				{
					MMObjectNode	node	= builder.getNode( number );
					if( node != null )
						result = setparameters( mmbase, node ); 
					else
						debug("AudioPartDef("+mmbase+","+number+"): ERROR: No node found for this number!");
				}
				else
					debug("AudioPartDef("+mmbase+","+number+"): ERROR: no builder(audioparts) found in mmbase!");
			}
			else
				debug("AudioPartDef("+mmbase+","+number+"): ERROR: Number is not greater than 0!");
		}
		else
			debug("AudioPartDef("+mmbase+","+number+"): ERROR: mmbase not initialised!");

		return result;
	}

// ----------------------------------------------------------------------------

	public AudioPartDef( MMBase mmbase, MMObjectNode node )
	{
		if( !setparameters( mmbase, node ) )
			debug("AudioPartDef("+node+"): ERROR: Not initialised, something went wrong!");
	}

	public boolean setparameters( MMBase mmbase, MMObjectNode node )
	{
		boolean result = false;
		if( node != null )
		{
			number 		= node.getIntValue("number");
			otype		= node.getIntValue("otype");
			owner		= node.getStringValue("owner");

			title		= node.getStringValue("title");
			subtitle	= node.getStringValue("subtitle");
			source		= node.getIntValue("source");
			playtime	= node.getIntValue("playtime");
			intro		= node.getStringValue("intro");
			body		= node.getStringValue("body");
			storage		= node.getIntValue("storage");
			
			getStartStop( mmbase );

			result		= true;
		}
		else
			debug("AudioPartsDef("+node+"): ERROR: Node is null!"); 
	
		return result;
	}

// ----------------------------------------------------------------------------

    public boolean getRawAudios( MMBase mmbase, int wantedspeed, int wantedchannels, boolean sorted )
    {
        boolean result = false;

        rawaudios = AudioUtils.getRawAudios( mmbase , number, sorted );
        if(rawaudios != null )
        {
            rawaudio = AudioUtils.getBestRawAudio( rawaudios, wantedspeed, wantedchannels );
            if( rawaudio != null )
            {
                result = true;
            }
			else
				if( debug ) 
					debug("getRawAudios("+number+","+wantedspeed+","+wantedchannels+","+sorted+"): WARNING: No best rawaudio found for this speed/channels!");
        }
		else
			if( debug ) 
				debug("getRawAudios("+number+","+wantedspeed+","+wantedchannels+","+sorted+"): WARNING: No rawaudios found for this node!");

        return result;
    }

	public void getStartStop( MMBase mmbase )
	{
		if( mmbase != null )
		{
			if( number > 0 )
			{
				MMObjectBuilder builder = mmbase.getMMObject("properties");
				if( builder != null )
				{
					MMObjectNode node = builder.getNode( number );
					if( node != null )
					{
						MMObjectNode start = (MMObjectNode)node.getProperty("starttime");
						MMObjectNode stop  = (MMObjectNode)node.getProperty("stoptime");

						if( start != null )
							starttime = start.getStringValue("value");
						if( stop  != null )
							stoptime = stop.getStringValue("value");
					}	
				}	
				else
					debug("getStartStop(): builder(properties("+builder+")) not found!");
			}
			else
				debug("getStartStop(): number("+number+") not valid!");
		}
		else
			debug("getStartStop("+mmbase+"): mmbase not valid!");
	}

	public String toText()
	{
		StringBuffer b = new StringBuffer();
		b.append( classname +":" + "number("+number+")\n");
		b.append( classname +":" + "otype("+otype+")\n");
		b.append( classname +":" + "owner("+owner+")\n");

		b.append( classname +":" + "title("+title+")\n");
		b.append( classname +":" + "subtitle("+subtitle+")\n");
		b.append( classname +":" + "source("+source+")\n");
		b.append( classname +":" + "playtime("+playtime+")\n");
		b.append( classname +":" + "intro("+intro+")\n");
		b.append( classname +":" + "body("+body+")\n");
		b.append( classname +":" + "storage("+storage+")\n");
		b.append( classname +":" + "starttime("+starttime+")\n");
		b.append( classname +":" + "stoptime("+stoptime+")\n");
		b.append( classname +":" + "rawaudios found : " + rawaudios.size() + "\n");
		b.append( classname +":" + "best rawaudio   : " + rawaudio.toString() + "\n"); 
		return b.toString();
	}

	/**
	 * Gets the Realaudio url and ads the 'title','start' and 'end' name and values parameters.
	 * davzev: Removed the double quotes around the values since Real SMIL doesn't handle it correctly.
	 * @param sp the scanepage
	 * @return a String with the url.
	 */
	public String getRealAudioUrl( scanpage sp ) {
		String result = null;
		result = rawaudio.getRealAudioUrl( sp );

		if( result != null ) {

			if( title != null && !title.equals("") ) {
				//result += "?title=\""+title+"\"";
				//Our realplayer can't handle url encoded string.
				//result += "?title="+MediaUtils.plusToProcent20(URLEncoder.encode(title)); //URLEncode value.
				result += "?title="+title;
			} else {
				//result += "?title=\"\"";
				result += "?title=";
			}

			String ss = null;
			if( starttime != null && !starttime.equals("")) {
				//ss = "&start=\""+starttime+"\"";
				ss = "&start="+starttime;
			}

			if( stoptime != null && !stoptime.equals("")) {
				if( ss != null ) {
					//ss += "&end=\""+stoptime+"\"";
					ss += "&end="+stoptime;
				} else {
					//ss = "&end=\""+stoptime+"\"";
					ss  = "&end="+stoptime;
				}
			}

			if( ss != null && !ss.equals(""))
				result += ss;				
		}
		return result;
	}

	public String toString()
	{
		return classname +"( number("+number+") otype("+otype+") owner("+owner+") title("+title+") subtitle("+subtitle+") source("+source+") playtime("+playtime+") intro("+intro+") body("+body+") storage("+storage+"), starttime("+starttime+"), stoptime("+stoptime+"), rawaudios("+rawaudios.size()+"), rawaudio("+rawaudio.toString()+")";
	}
}
