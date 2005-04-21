package org.mmbase.module;

import org.mmbase.util.CompareInterface;

public 	class		VotesCompareVote
		implements	CompareInterface
{
	public int compare( Object thisObject, Object otherObject )
    {
		Vote thisVote  = (Vote) thisObject;
		Vote otherVote = (Vote) otherObject;
	
        int result = 0;

        if( thisVote.vote >  otherVote.vote )
            result = 01;
        else
        if( thisVote.vote == otherVote.vote )
            result = 00;
        else
        if( thisVote.vote < otherVote.vote )
            result = -1;

        return result;
    }
}
