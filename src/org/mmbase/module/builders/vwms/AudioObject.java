/*

VPRO (C)

This source file is part of mmbase and is (c) by VPRO until it is being
placed under opensource. This is a private copy ONLY to be used by the
MMBase partners.

*/

package org.mmbase.module.builders.vwms;

import java.util.*;
import java.text.*;

import org.mmbase.util.*;

public class AudioObject {
Execute exec=new Execute();
private String filename;
private int samples;
private int channels;
private int frequency;
private int time; // Ending time
private float length;

	public AudioObject() {

	}

	/** InfoAudio gives us:
 RIFF WAVE file: /data/back1/pool/1/uur1.wav
   Number of samples : 317520000  1998/09/16 11:59:54 UTC
   Sampling frequency: 44100 Hz
   Number of channels: 2 (16-bit integer)
File name: /data/import/pool/1/uur1.wav
Header length: 44
Sampling frequency: 44100
No. samples: 317520000
No. channels: 2
Data type: integer16
File byte order: little-endian
Host byte order: little-endian
	**/


	public static AudioObject get(String pool) {
		return(getInfo(pool+"/0.wav"));
	}

	public static AudioObject getInfo(String filename) {
		Execute sexec=new Execute();
		String inf,t,dat="",tim="";
		AudioObject ao=new AudioObject();
		StringTokenizer tok;
		int line=0,word=0;
		float len;


		System.out.println("AudioObject file "+filename);
		ao.setFilename(filename);
		inf=sexec.execute("/usr/local/bin/InfoAudio "+filename);

		tok=new StringTokenizer(inf," \n\r\t",true);
		while(tok.hasMoreTokens()) {
			t=tok.nextToken();
			System.out.print(t);
			if (t.equals(" ") || t.equals("\t") || t.equals("\r")) {
				// skip
			} else if (t.equals("\n")) {
				word=0;
				line++;
			} else {
				switch(line) {
					case 0:  // filetype and filename
						break;
					case 1:  // number of samples and date and time
						if (word==4) { // should be number of samples
							ao.setSamples(Integer.parseInt(t));
							// Note divide by channels is needed;
						}
						if (word==5) {
							dat=t; // datum
						}
						if (word==6) {
							tim=t; // time
						}
						// we convert to time_t later
						break;
					case 2:	 // sampling frequency
						if (word==2) {
							ao.setFrequency(Integer.parseInt(t));
						}
						break;
					case 3:	 // number of channels and sample-size
						if (word==3) {
							ao.setChannels(Integer.parseInt(t));
						}
						break;
					default:
						break;
				}
				word++;
			}
		}
		// Info audio reports *total* samples not per channel
		ao.setSamples(ao.getSamples()/ao.getChannels());
		// Decode the time (as usual)
		TimeZone tz=TimeZone.getTimeZone("GMT");
		Calendar calendar = new GregorianCalendar(tz);
		Calendar cl=DateSupport.parseDateRev(calendar,dat+" "+tim);
		Date d = cl.getTime ();
		long l = d.getTime ();
		if ((cl.getTimeZone ()).inDaylightTime (d)) {
			l += 60 * 60 * 1000;
		}
		// RICO timetrouble looks good as it uses GMT with MilliOffset
		ao.setTime((int)((l-DateSupport.getMilliOffset())/1000));
		len=(float)(ao.getSamples()/(1.0*ao.getFrequency()));
		ao.setLength(len);

		return(ao);
	}

	public AudioObject cut(String name,int start,int stop,int len) {
		int astart;
		int sampstart,sampstop;
		String ex;

		astart=getTime()-(getSamples()/getFrequency());
		System.out.println("AudioObject start "+astart+","+DateSupport.date2string(astart));
		System.out.println("AudioObject cutting from "+DateSupport.date2string(start)+" to "+DateSupport.date2string(stop));
		if (astart>start) {
			System.out.println("AudioObject : start smaller than start of wav");
			sampstart=0;
		} else {
			sampstart=(start-astart)*getFrequency();
		}
		if (stop>time) {
			System.out.println("AudioObject stop larger than end of wav");
			sampstop=getSamples();
		} else {
			sampstop=(stop-astart)*getFrequency();
		}
		System.out.println("AudioObject about to cut "+sampstart+","+sampstop);
		ex="/usr/local/bin/CopyAudio -l "+sampstart+":"+sampstop+" "+getFilename()+" -F WAVE "+name;
		System.out.println("AudioObject cut exec "+ex);
		String s=exec.execute(ex);
		System.out.println("AudioObject Exec result "+s);
		return(getInfo(name));
	}


	public String getFilename() {
		return(filename);
	}
	public void setFilename(String s) {
		filename=s;
	}
	public int getSamples() {
		return(samples);
	}
	public void setSamples(int s) {
		samples=s;
	}
	public int getChannels() {
		return(channels);
	}
	public void setChannels(int s) {
		channels=s;
	}
	public int getFrequency() {
		return(frequency);
	}
	public void setFrequency(int s) {
		frequency=s;
	}
	public int getTime() {
		return(time);
	}
	public void setTime(int s) {
		time=s;
	}
	public void setLength(float l) {
		length=l;
	}
	public float getLength() {
		return length;
	}

	public String toString() {
		return("AudioObject w="+filename+",s="+samples+",c="+channels+",f="+frequency+",t="+time+"=="+DateSupport.date2string(time)+",l="+getLength());
	}

	public static void main(String args[]) {
		AudioObject ao;

		ao=AudioObject.getInfo(args[0]);
		System.out.println(ao);
	}
}
