/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.simexplorer.ui.tools;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.URLMapper;
import org.openide.loaders.DataObject;
import org.openide.util.NbPreferences;
import org.openide.windows.CloneableTopComponent;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 *
 * @author thierry
 */
public class RecentApplications {

    /** List of recently closed files */
    private static List<HistoryItem> history = new ArrayList<HistoryItem>();

    /** Preferences node for storing history info */
    private static Preferences prefs;

    private static final Object HISTORY_LOCK = new Object();

    /** Name of preferences node where we persist history */
    private static final String PREFS_NODE = "RecentApplicationsHistory";

    /** Separator to encode file path and time into one string in preferences */
    private static final String SEPARATOR = "; time=";

    /** Boundary for items count in history */
    private static final int MAX_HISTORY_ITEMS = 20;

    private RecentApplications () {
    }

    /** Starts to listen for recently closed files */
    public static void init () {
        WindowManager.getDefault().invokeWhenUIReady(new Runnable() {
            public void run() {
                List<HistoryItem> loaded = load();
                synchronized (HISTORY_LOCK) {
                    history.addAll(0, loaded);
                }
                TopComponent.getRegistry().addPropertyChangeListener(new WindowRegistryL());
            }
        });
    }

   /** Returns read-only list of recently closed files */
    public static List<HistoryItem> getRecentFiles () {
        synchronized (HISTORY_LOCK) {
            checkHistory();
            return Collections.unmodifiableList(history);
        }
    }

    /** Loads list of recent files stored in previous system sessions.
     * @return list of stored recent files
     */
    static List<HistoryItem> load () {
        String[] keys;
        Preferences prefs = getPrefs();
        try {
            keys = prefs.keys();
        }
        catch (BackingStoreException ex) {
            Logger.getLogger(RecentApplications.class.getName()).log(Level.FINE, ex.getMessage(), ex);
            return Collections.emptyList();
        }
        List<HistoryItem> result = new ArrayList<HistoryItem>(keys.length + 10);
        HistoryItem hItem;
        for (String curKey : keys) {
            hItem = decode(prefs.get(curKey, null));
            if (hItem != null) {
                result.add(hItem);
            } else {
                // decode failed, so clear crippled item
                prefs.remove(curKey);
            }
        }
        Collections.sort(result);
        return result;
    }

   private static HistoryItem decode (String value) {
        int sepIndex = value.lastIndexOf(SEPARATOR);
        if (sepIndex <= 0) {
            return null;
        }
        URL url = null;
        try {
            url = new URL(value.substring(0, sepIndex));
        } catch (MalformedURLException ex) {
            // url corrupted, skip
            Logger.getLogger(RecentApplications.class.getName()).log(Level.FINE, ex.getMessage(), ex);
            return null;
        }
        long time = 0;
        try {
            time = Long.decode(value.substring(sepIndex + SEPARATOR.length()));
        } catch (NumberFormatException ex) {
            // stored data corrupted, skip
            Logger.getLogger(RecentApplications.class.getName()).log(Level.FINE, ex.getMessage(), ex);
            return null;
        }
        return new HistoryItem(url, time);
    }

    static void storeRemoved (HistoryItem hItem) {
        String stringURL = hItem.getURL().toExternalForm();
        getPrefs().remove(trimToKeySize(stringURL));
    }

    static void storeAdded (HistoryItem hItem) {
        String stringURL = hItem.getURL().toExternalForm();
        String value = stringURL + SEPARATOR + String.valueOf(hItem.getTime());
        getPrefs().put(trimToKeySize(stringURL), value);
    }

    private static String trimToKeySize (String path) {
        int length = path.length();
        if (length > Preferences.MAX_KEY_LENGTH) {
            path = path.substring(length - Preferences.MAX_KEY_LENGTH, length);
        }
        return path;
    }

   static Preferences getPrefs () {
        if (prefs == null) {
            prefs = NbPreferences.forModule(RecentApplications.class).node(PREFS_NODE);
        }
        return prefs;
    }

    /** Adds file represented by given TopComponent to the list,
     * if conditions are met.
     */
    private static void addFile (TopComponent tc) {
        if (tc instanceof CloneableTopComponent) {
            URL fileURL = obtainURL(tc);
            if (fileURL != null) {
                boolean added = false;
                synchronized (HISTORY_LOCK) {
                    // avoid duplicates
                    HistoryItem hItem = findHistoryItem(fileURL);
                    if (hItem == null) {
                        hItem = new HistoryItem(fileURL, System.currentTimeMillis());
                        history.add(0, hItem);
                        storeAdded(hItem);
                        added = true;
                        // keep manageable size of history
                        // remove the oldest item if needed
                        if (history.size() > MAX_HISTORY_ITEMS) {
                            HistoryItem oldest = history.get(history.size() - 1);
                            history.remove(oldest);
                            storeRemoved(oldest);
                        }
                    }
                }
            }
        }
    }

    /** Removes file represented by given TopComponent from the list */
    private static void removeFile (TopComponent tc) {
        if (tc instanceof CloneableTopComponent) {
            URL fileURL = obtainURL(tc);
            if (fileURL != null) {
                boolean removed = false;
                synchronized (HISTORY_LOCK) {
                    HistoryItem hItem = findHistoryItem(fileURL);
                    if (hItem != null) {
                        history.remove(hItem);
                        storeRemoved(hItem);
                        removed = true;
                    }
                }
            }
        }
    }

    private static URL obtainURL (TopComponent tc) {
        DataObject dObj = tc.getLookup().lookup(DataObject.class);
        if (dObj != null) {
            FileObject fo = dObj.getPrimaryFile();
            if (fo != null) {
                return convertFile2URL(fo);
            }
        }
        return null;
    }

    private static HistoryItem findHistoryItem (URL url) {
        for (HistoryItem hItem : history) {
            if (url.equals(hItem.getURL())) {
                return hItem;
            }
        }
        return null;
    }

    static URL convertFile2URL (FileObject fo) {
        URL url = URLMapper.findURL(fo, URLMapper.EXTERNAL);
        if (url == null) {
            Logger.getLogger(RecentApplications.class.getName()).log(Level.FINE,
                    "convertFile2URL: URL can't be found for FileObject " + fo); // NOI18N
        }
        return url;
    }

    static FileObject convertURL2File (URL url) {
        FileObject fo = URLMapper.findFileObject(url);
        if (fo == null) {
            Logger.getLogger(RecentApplications.class.getName()).log(Level.FINE,
                    "convertURL2File: File can't be found for URL " + url); // NOI18N
        }
        return fo;
    }

    /** Checks recent files history and removes non-valid entries */
    private static void checkHistory () {
        // note, code optimized for the frequent case that there are no invalid entries
        List<HistoryItem> invalidEntries = new ArrayList<HistoryItem>(3);
        FileObject fo = null;
        for (HistoryItem historyItem : history) {
            fo = convertURL2File(historyItem.getURL());
            if (fo == null || !fo.isValid()) {
                invalidEntries.add(historyItem);
            }
        }
        for (HistoryItem historyItem : invalidEntries) {
            history.remove(historyItem);
        }
    }




    /** One item of the recently closed files history
     * Comparable by the time field, ascending from most recent to older items.
     */
    public static final class HistoryItem<T extends HistoryItem> implements Comparable<T> {

        private long time;
        private URL fileURL;

        HistoryItem (URL fileURL, long time) {
            this.fileURL = fileURL;
            this.time = time;
        }

        public URL getURL () {
            return fileURL;
        }

        public long getTime () {
            return time;
        }

        public int compareTo(T other) {
            long diff = time - other.getTime();
            return diff < 0 ? 1 : diff > 0 ? -1 : 0;
        }

    }
    /** Receives info about opened and closed TopComponents from window system.
     */
    private static class WindowRegistryL implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent evt) {
            if (TopComponent.Registry.PROP_TC_CLOSED.equals(evt.getPropertyName())) {
                addFile((TopComponent) evt.getNewValue());
            }
            if (TopComponent.Registry.PROP_TC_OPENED.equals(evt.getPropertyName())) {
                removeFile((TopComponent) evt.getNewValue());
            }
        }

    }


}
