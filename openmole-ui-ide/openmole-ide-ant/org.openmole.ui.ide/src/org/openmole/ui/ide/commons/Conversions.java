package org.openmole.ui.ide.commons;

import java.util.Collection;

/**
 *
 * @author mathieu
 */
public class Conversions {

    public static String[] collectionToStringArray(Collection<String> col) {
        Object ob[] = col.toArray();
        String st[] = new String[ob.length];
        for (int i = 0; i < ob.length; ++i) {
            st[i] = ob[i].toString();
        }
        return st;
    }
}
