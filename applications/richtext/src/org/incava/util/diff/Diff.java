package org.incava.util.diff;

import java.util.*;


/**
 * Compares two collections, returning a list of the additions, changes, and
 * deletions between them. A <code>Comparator</code> may be passed as an
 * argument to the constructor, and will thus be used. If not provided, the
 * initial value in the <code>a</code> ("from") collection will be looked at to
 * see if it supports the <code>Comparable</code> interface. If so, its
 * <code>equals</code> and <code>compareTo</code> methods will be invoked on the
 * instances in the "from" and "to" collections; otherwise, for speed, hash
 * codes from the objects will be used instead for comparison.
 *
 * <p>The file FileDiff.java shows an example usage of this class, in an
 * application similar to the Unix "diff" program.</p>
 */
public class Diff
{
    /**
     * The source array, AKA the "from" values.
     */
    protected final Object[] a;

    /**
     * The target array, AKA the "to" values.
     */
    protected final Object[] b;

    /**
     * The list of differences, as <code>Difference</code> instances.
     */
    protected List<Difference> diffs;

    /**
     * The pending, uncommitted difference.
     */
    private Difference pending;

    /**
     * The comparator used, if any.
     */
    private Comparator comparator;

    /**
     * The thresholds.
     */
    private TreeMap<Integer, Integer> thresh;

    /**
     * Constructs the Diff object for the two arrays, using the given comparator.
     */
    public Diff(Object[] a, Object[] b, Comparator comp)
    {
        this.a = a;
        this.b = b;
        this.comparator = comp;
        this.thresh = null;     // created in getLongestCommonSubsequences
    }

    /**
     * Constructs the Diff object for the two arrays, using the default
     * comparison mechanism between the objects, such as <code>equals</code> and
     * <code>compareTo</code>.
     */
    public Diff(Object[] a, Object[] b)
    {
        this(a, b, null);
    }

    /**
     * Constructs the Diff object for the two collections, using the given
     * comparator.
     */
    public Diff(Collection a, Collection b, Comparator comp)
    {
        this(a.toArray(), b.toArray(), comp);
    }

    /**
     * Constructs the Diff object for the two collections, using the default
     * comparison mechanism between the objects, such as <code>equals</code> and
     * <code>compareTo</code>.
     */
    public Diff(Collection a, Collection b)
    {
        this(a, b, null);
    }

    /**
     * Runs diff and returns the results.
     */
    public List<Difference> diff()
    {
        if (diffs == null) {
            diffs = new ArrayList<Difference>();
            traverseSequences();

            // add the last difference, if pending:
            if (pending != null) {
                diffs.add(pending);
            }

        }
        return diffs;
    }

    /**
     * Traverses the sequences, seeking the longest common subsequences,
     * invoking the methods <code>finishedA</code>, <code>finishedB</code>,
     * <code>onANotB</code>, and <code>onBNotA</code>.
     */
    protected void traverseSequences()
    {
        Integer[] matches = getLongestCommonSubsequences();

        int lastA = a.length - 1;
        int lastB = b.length - 1;
        int bi = 0;
        int ai;

        int lastMatch = matches.length - 1;

        for (ai = 0; ai <= lastMatch; ++ai) {
            Integer bLine = matches[ai];

            if (bLine == null) {
                onANotB(ai, bi);
            }
            else {
                while (bi < bLine) {
                    onBNotA(ai, bi++);
                }

                onMatch(ai, bi++);
            }
        }

        boolean calledFinishA = false;
        boolean calledFinishB = false;

        while (ai <= lastA || bi <= lastB) {

            // last A?
            if (ai == lastA + 1 && bi <= lastB) {
                if (!calledFinishA && callFinishedA()) {
                    finishedA(lastA);
                    calledFinishA = true;
                }
                else {
                    while (bi <= lastB) {
                        onBNotA(ai, bi++);
                    }
                }
            }

            // last B?
            if (bi == lastB + 1 && ai <= lastA) {
                if (!calledFinishB && callFinishedB()) {
                    finishedB(lastB);
                    calledFinishB = true;
                }
                else {
                    while (ai <= lastA) {
                        onANotB(ai++, bi);
                    }
                }
            }

            if (ai <= lastA) {
                onANotB(ai++, bi);
            }

            if (bi <= lastB) {
                onBNotA(ai, bi++);
            }
        }
    }

    /**
     * Override and return true in order to have <code>finishedA</code> invoked
     * at the last element in the <code>a</code> array.
     */
    protected boolean callFinishedA()
    {
        return false;
    }

    /**
     * Override and return true in order to have <code>finishedB</code> invoked
     * at the last element in the <code>b</code> array.
     */
    protected boolean callFinishedB()
    {
        return false;
    }

    /**
     * Invoked at the last element in <code>a</code>, if
     * <code>callFinishedA</code> returns true.
     */
    protected void finishedA(int lastA)
    {
    }

    /**
     * Invoked at the last element in <code>b</code>, if
     * <code>callFinishedB</code> returns true.
     */
    protected void finishedB(int lastB)
    {
    }

    /**
     * Invoked for elements in <code>a</code> and not in <code>b</code>.
     */
    protected void onANotB(int ai, int bi)
    {
        if (pending == null) {
            pending = new Difference(ai, ai, bi, -1);
        }
        else {
            pending.setDeleted(ai);
        }
    }

    /**
     * Invoked for elements in <code>b</code> and not in <code>a</code>.
     */
    protected void onBNotA(int ai, int bi)
    {
        if (pending == null) {
            pending = new Difference(ai, -1, bi, bi);
        }
        else {
            pending.setAdded(bi);
        }
    }

    /**
     * Invoked for elements matching in <code>a</code> and <code>b</code>.
     */
    protected void onMatch(int ai, int bi)
    {
        if (pending == null) {
            // no current pending
        }
        else {
            diffs.add(pending);
            pending = null;
        }
    }

    /**
     * Compares the two objects, using the comparator provided with the
     * constructor, if any.
     */
    protected boolean equals(Object x, Object y)
    {
        return comparator == null ? x.equals(y) : comparator.compare(x, y) == 0;
    }

    /**
     * Returns an array of the longest common subsequences.
     */
    public Integer[] getLongestCommonSubsequences()
    {
        int aStart = 0;
        int aEnd = a.length - 1;

        int bStart = 0;
        int bEnd = b.length - 1;

        TreeMap<Integer, Integer> matches = new TreeMap<Integer, Integer>();

        while (aStart <= aEnd && bStart <= bEnd && equals(a[aStart], b[bStart])) {
            matches.put(aStart++, bStart++);
        }

        while (aStart <= aEnd && bStart <= bEnd && equals(a[aEnd], b[bEnd])) {
            matches.put(aEnd--, bEnd--);
        }

        Map<Object, List<Integer>> bMatches = null;
        if (comparator == null) {
            if (a.length > 0 && a[0] instanceof Comparable) {
                // this uses the Comparable interface
                bMatches = new TreeMap<Object, List<Integer>>();
            }
            else {
                // this just uses hashCode()
                bMatches = new HashMap<Object, List<Integer>>();
            }
        }
        else {
            // we don't really want them sorted, but this is the only Map
            // implementation (as of JDK 1.4) that takes a comparator.
            bMatches = new TreeMap<Object, List<Integer>>(comparator);
        }

        for (int bi = bStart; bi <= bEnd; ++bi) {
            Object element   = b[bi];
            Object key       = element;
            List<Integer>   positions = bMatches.get(key);
            if (positions == null) {
                positions = new ArrayList<Integer>();
                bMatches.put(key, positions);
            }
            positions.add(bi);
        }

        thresh = new TreeMap<Integer, Integer>();
        Map<Integer, Object[]> links = new HashMap<Integer, Object[]>();

        for (int i = aStart; i <= aEnd; ++i) {
            Object aElement  = a[i]; // keygen here.
            List<Integer>   positions = bMatches.get(aElement);

            if (positions != null) {
                Integer  k   = 0;
                ListIterator<Integer> pit = positions.listIterator(positions.size());
                while (pit.hasPrevious()) {
                    Integer j = pit.previous();

                    k = insert(j, k);

                    if (k == null) {
                        // nothing
                    }
                    else {
                        Object value = k > 0 ? links.get(k - 1) : null;
                        links.put(k, new Object[] { value, i, j });
                    }
                }
            }
        }

        if (thresh.size() > 0) {
            Integer  ti   = thresh.lastKey();
            Object[] link = links.get(ti);
            while (link != null) {
                Integer x = (Integer)link[1];
                Integer y = (Integer)link[2];
                matches.put(x, y);
                link =  (Object[])link[0];
            }
        }

        return toArray(matches);
    }

    /**
     * Converts the map (indexed by java.lang.Integers) into an array.
     */
    protected static Integer[] toArray(TreeMap<Integer, Integer> map)
    {
        int       size = map.size() == 0 ? 0 : 1 + map.lastKey();
        Integer[] ary  = new Integer[size];
        Iterator<Integer>  it   = map.keySet().iterator();

        while (it.hasNext()) {
            Integer idx = it.next();
            Integer val = map.get(idx);
            ary[idx] = val;
        }
        return ary;
    }

    /**
     * Returns whether the integer is not zero (including if it is not null).
     */
    protected static boolean isNonzero(Integer i)
    {
        return i != null && i != 0;
    }

    /**
     * Returns whether the value in the map for the given index is greater than
     * the given value.
     */
    protected boolean isGreaterThan(Integer index, Integer val)
    {
        Integer lhs = thresh.get(index);
        return lhs != null && val != null && lhs.compareTo(val) > 0;
    }

    /**
     * Returns whether the value in the map for the given index is less than
     * the given value.
     */
    protected boolean isLessThan(Integer index, Integer val)
    {
        Integer lhs = thresh.get(index);
        return lhs != null && (val == null || lhs.compareTo(val) < 0);
    }

    /**
     * Returns the value for the greatest key in the map.
     */
    protected Integer getLastValue()
    {
        return thresh.get(thresh.lastKey());
    }

    /**
     * Adds the given value to the "end" of the threshold map, that is, with the
     * greatest index/key.
     */
    protected void append(Integer value)
    {
        Integer addIdx = null;
        if (thresh.size() == 0) {
            addIdx = 0;
        }
        else {
            Integer lastKey = thresh.lastKey();
            addIdx = lastKey + 1;
        }
        thresh.put(addIdx, value);
    }

    /**
     * Inserts the given values into the threshold map.
     */
    protected Integer insert(Integer j, Integer k)
    {
        if (isNonzero(k) && isGreaterThan(k, j) && isLessThan(k - 1, j)) {
            thresh.put(k, j);
        }
        else {
            int hi = -1;

            if (isNonzero(k)) {
                hi = k;
            }
            else if (thresh.size() > 0) {
                hi = thresh.lastKey();
            }

            // off the end?
            if (hi == -1 || j.compareTo(getLastValue()) > 0) {
                append(j);
                k = hi + 1;
            }
            else {
                // binary search for insertion point:
                int lo = 0;

                while (lo <= hi) {
                    int     index = (hi + lo) / 2;
                    Integer val   = thresh.get(index);
                    int     cmp   = j.compareTo(val);

                    if (cmp == 0) {
                        return null;
                    }
                    else if (cmp > 0) {
                        lo = index + 1;
                    }
                    else {
                        hi = index - 1;
                    }
                }

                thresh.put(lo, j);
                k = lo;
            }
        }

        return k;
    }

    protected void appendToHtml(StringBuilder buf, Object[] a, int i, String claz) {
        buf.append("<td class='" + claz + "'>");
        if (i >= 0 && i < a.length) {
            buf.append("" + a[i]);
        }
        buf.append("</td>");
    }
    protected void appendToHtml(StringBuilder buf, int startA, int endA, int startB, int endB, Difference d) {
        for (int i = 0; i <= Math.max(endA - startA, endB - startB); i++) {
            buf.append("<tr class='" +
                       (d != null ?
                        (i + startB < b.length ? "add " : "") +
                        (i + startA < a.length ? "delete " : "")
                        :
                        "") +
                       "'>");
            buf.append("<td class='difference'>" + (i == 0 ? "" + d + ": " : "") + i +"</td>");
            appendToHtml(buf, a, endA != Difference.NONE ? i + startA : -1, "old");
            appendToHtml(buf, b, endB != Difference.NONE ? i + startB : -1, "new");
            buf.append("</tr>");
        }
    }

    public String toHtml() {
        StringBuilder buf = new StringBuilder();
        int startA = -1;
        int startB = -1;
        buf.append("<tr class='difference'><td colspan='100'>" + diff() + "</td></tr>");
        for (Difference d : diff()) {
            int delEnd = d.getDeletedEnd();
            int delStart= d.getDeletedStart();
            int addEnd = d.getAddedEnd();
            int addStart = d.getAddedStart();
            if (delEnd != Difference.NONE) {
                startA = delEnd;
            }
            if (addEnd != Difference.NONE) {
                 startB = addEnd;
            }
            appendToHtml(buf, delStart, delEnd, addStart, addEnd, d);

        }
        appendToHtml(buf, startA + 1, a.length, startB + 1, b.length, null);
        return buf.toString();

    }

}
