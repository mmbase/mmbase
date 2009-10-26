/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util;
import org.mmbase.util.Casting;
import org.mmbase.util.transformers.*;
import org.mmbase.bridge.Node;
import java.util.*;
import org.junit.*;
import static org.junit.Assert.*;
/**
 *
 * @author Michiel Meeuwissen
 * @verion $Id$
 */
public class CastingTest  {


    @Test
    public void testNull() {
        assertEquals("", Casting.toString(null));
        assertEquals("", Casting.wrap(null, new Xml()).toString());
    }
    @Test
    public void testList() {
        List<String> list = new ArrayList<String>();
        list.add("a");
        list.add("b");
        list.add("c");
        assertEquals(list, Casting.toList("a,b,c"));
        assertEquals(list, Casting.toList("a , b , c"));
        assertEquals("a,b,c", Casting.toString(list));
    }
    @Test
    public void testInt() {
        assertEquals(1, Casting.toInt("1"));
        assertEquals(-1, Casting.toInt("asdfasdf"));
        assertEquals(-5, Casting.toInt("asdfasdf", -5));
        assertEquals(-5, Casting.toInt(null, -5));

        assertEquals(5, Casting.toInt("5.3"));
        assertEquals(5, Casting.toInt("5.6"));
        assertEquals(100, Casting.toInt("1e2"));
        assertEquals(-1, Casting.toInt(null));
        assertEquals(8, Casting.toInt(null, 8));
        assertEquals(8, Casting.toInt("bla bloe", 8));
        assertEquals(15, Casting.toInt("15", 8));

        assertEquals("15", Casting.toString(15));
    }
    @Test
    public void testInteger() {
        assertEquals(new Integer(10), (Object) Casting.toInteger("10"));
        assertEquals(new Integer(10), (Object) Casting.toInteger("1e1"));
        assertEquals(new Integer(-1), (Object) Casting.toInteger(null));
    }
    @Test
    public void testLong() {
        assertEquals(new Long(10), (Object) Casting.toLong("10"));
        assertEquals(new Long(10), (Object) Casting.toLong("1e1"));
        assertEquals(new Long(-1), (Object) Casting.toLong(null));
    }

    @Test
    public void testFloat() {
        assertEquals(new Float(-1.0), (Object) Casting.toFloat(null));
    }

    @Test
    public void testDouble() {
        assertEquals(new Double(-1.0), (Object) Casting.toDouble(null));
    }
    @Test
    public void testBinary() {

    }


    @Test
    public void canCast() {
        assertTrue(Casting.canCast(Node.class, Integer.class));

        assertTrue(Casting.canCast(String.class, Integer.class));
        assertTrue(Casting.canCast(String.class, Float.class));


        assertTrue(Casting.canCast(Integer.class, String.class));
        assertTrue(Casting.canCast(Float.class, String.class));
    }


}
