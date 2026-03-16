package org.mmbase.cache.implementation;

import java.util.Arrays;
import java.util.Collection;
import java.util.Random;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mmbase.cache.Cache;
import org.mmbase.cache.CacheImplementationInterface;
import org.mmbase.util.LRUHashtable;


/**
 Some results (at 2 x (dual core)  Intel(R) Core(TM)2 CPU         T7200  @ 2.00GHz)
 <pre>
 michiel@mitulo:~/mmbase/head$ java org.mmbase.cache.implementation.Test 1025 1000000 50 org.mmbase.cache.implementation.LRUCache
 Size 512
 [+0][+1][+2][+3][+8][+6][+7][+5][+4][+9][+10][+11][+12][+13][+14][+15][+16][+17][+18][+19][+20][+21][+22][+23][+26][+27][+28][+24][+29][+30][+31][+32][+33][+34][+35][+36][+37][+38][+39][+40][+41][+42][+43][+44][+45][+46][+47][+48][+25][+49][-45][-20][-43][-41][-47][-46][-16][-4][-12][-6][-2][-15][-8][-21][-18][-22][-17][-28][-27][-26][-29][-30][-24][-13][-39][-34][-40][-42][-44][-25][-37][-1][-3][-11][-0][-49][-7][-14][-19][-48][-33][-31][-36][-5][-35][-38][-9][-23][-32][-10].
 Creation 8 ns
 Thread starting 147728 us
 Not printed (too huge)
 Run      62635 ms (62635 us/koperation,  1252 us/koperation total from 50 threads)
 Used implementation: class org.mmbase.cache.implementation.LRUCache
 michiel@mitulo:~/mmbase/head$ java org.mmbase.cache.implementation.Test 1025 1000000 50 org.mmbase.util.LRUHashtable
 Size 512
 [+0][+1][+2][+3][+4][+5][+6][+7][+8][+9][+10][+12][+13][+11][+14][+15][+16][+17][+18][+20][+19][+21][+22][+23][+24][+25][+26][+27][+28][+30][+31][+32][+33][+34][+35][+36][+37][+38][+39][+40][+41][+42][+43][+44][+45][+46][+47][+48][+49][+29][-20][-47][-42][-39][-36][-38][-27][-21][-23][-22][-16][-10][-17][-12][-8][-3][-4][-49][-48][-2][-44][-35][-43][-30][-32][-11][-34][-25][-31][-19][-33][-1][-24][-37][-0][-40][-41][-13][-9][-45][-18][-15][-7][-28][-29][-26][-46][-5][-6][-14].
 Creation 8 ns
 Thread starting 130541 us
 Not printed (too huge)
 Run      67879 ms (67879 us/koperation,  1357 us/koperation total from 50 threads)
 Used implementation: class org.mmbase.util.LRUHashtable
 michiel@mitulo:~/mmbase/head$
 </pre>

 * Conclusion: No difference. I think we can just as well use LRUCache, since it is less code.
 *
 * @author  Rico Jansen (in org.mmbase.util.LRUHashtable)
 * @author  Michiel Meeuwissen
 * @version $Id$
 * @see    Cache
 * @since MMBase-1.9
 */
@RunWith(Parameterized.class)
public class PerformanceTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            {1025, 1000000, 4, LRUCache.class},
            {1025, 1000000, 4, LRUHashtable.class},
            {1025, 1000000, 4, FIFOCache.class},
            {1025, 1000000, 4, CacheImplementationInterface.class},

        });
    }

    int treesiz;
    int opers;
    int thrds;
    Class<?> impl;

    public PerformanceTest(int treesiz, int opers, int thrds, Class impl) {
        this.treesiz =treesiz;
        this.opers = opers;
        this.thrds = thrds;
        this.impl = impl;
    }


    @Test
    public void test() throws Exception {
        main(new String[] {"" + treesiz, "" + opers, "" + thrds, impl.getName()});
    }

    public static void main(String[] argv) throws Exception {
        Class<?> impl = LRUCache.class;
        int treesiz = 1024;
        int opers = 1000000;
        int thrds  = 1;

        try {
            if (argv.length > 0) {
                treesiz = Integer.parseInt(argv[0]);
            }
            if (argv.length > 1) {
                opers = Integer.parseInt(argv[1]);
            }
            if (argv.length > 2) {
                thrds = Integer.parseInt(argv[2]);
            }

            if (argv.length > 3) {
                impl = Class.forName(argv[3]);
            }
        } catch (Exception e) {
            System.out.println("Usage: java org.mmbase.util.LRUHashtable <size of table> <number of operation to do> <threads> <class name>");
            return;
        }


        final CacheImplementationInterface<String, String> treap = (CacheImplementationInterface<String, String>) impl.newInstance();
        treap.setMaxSize(treesiz / 2);
        System.out.println("Created " + treap.getClass() + " " + treap + " with treesiz " + treesiz);
        long ll1 = System.currentTimeMillis();

        // fill the map
        for (int i = 0; i < treesiz; i++) {
            treap.put(""+i,""+i);
        }
        long ll2=System.currentTimeMillis();
        System.out.println("Size "+treap.size());

        if (treesiz <= 1024) {
            System.out.println("LRUHashtable initially " + treap.entrySet());
        }
        final int TREESIZ = treesiz;
        final int OPERS = opers;

        final  int score[][] = new int[TREESIZ][thrds];
        long ll3 = System.nanoTime();

        final Thread[] threads = new Thread[thrds];
        for (int t = 0; t < thrds; t++) {
            final int  threadnr = t;
            Runnable runnable = new Runnable() {
                public void run() {
                    if (threads.length > 1) {
                        System.out.print("[+" + threadnr + "]");
                    }
                    Random rnd = new Random();
                    for (int i = 0; i < OPERS ;i++) {
                        // Put and get mixed
                        int j = Math.abs(rnd.nextInt())% (TREESIZ/2)+(TREESIZ/4);
                        int k = Math.abs(rnd.nextInt())% 2;
                        switch (k) {
                            case 0:
                                treap.put(""+j,""+j);
                                score[j][threadnr]++;
                                break;
                            case 1:
                                treap.get(""+j);
                                score[j][threadnr]++;
                                break;
                        }
                        // Only a get
                        j = Math.abs(rnd.nextInt())%(TREESIZ);
                        treap.get(""+j);
                        score[j][threadnr]++;
                    }
                    if (threads.length > 1) {
                        System.out.print("[-" + threadnr + "]");
                    }
                }
            };
            threads[t] = new Thread(runnable);
            threads[t].start();
        }
        long ll4 = System.nanoTime();
        for (int i = 0; i < thrds; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException ie) {
                System.err.println("Interrupted");
            }
        }
        System.out.println(".");
        long ll5 = System.nanoTime();
        if (TREESIZ <= 1024) {
            System.out.println("LRUHashtable afterwards " + treap.entrySet());

            for (int i = 0; i <TREESIZ; i++) {
                int totscore = 0;
                for (int j = 0; j < thrds; j++) {
                    totscore += score[i][j];
                }
                System.out.println("" + i + " score " + totscore);
            }
        }
        System.out.println("Creation " + (ll2 - ll1) + " ns");
        System.out.println("Thread starting " + (ll4 - ll3) / 1000 + " us");
        if (TREESIZ <= 1024) {
            System.out.println("Print    " + (ll3 - ll2) / 1000000 + " ms");
        } else {
            System.out.println("Not printed (too huge)");
        }
        long timePerKop = (ll5 - ll3) * 1000 / (opers);
        System.out.println("Run      " + (ll5-ll3) / 1000000+ " ms (" + timePerKop / 1000 + " us/koperation,  " + (timePerKop / thrds) / 1000 + " us/koperation total from " + thrds + " threads)");
        System.out.println("Used implementation: " + impl);
    }

  /*  ReadWriteLock lock = new ReentrantReadWriteLock();


    @Test
    public void testLock() {
        lock.writeLock().lock();
        lock.readLock().lock();
        lock.readLock().lock();

        lock.writeLock().lock();
        try {

        } finally {
            lock.writeLock().lock();
            lock.readLock().unlock();
            lock.readLock().unlock();
            lock.writeLock().unlock();

        }
    }
*/


}
