/*
 *  Copyright © 2008, 2009, Cemagref
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation; either version 3 of
 *  the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this program; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston,
 *  MA  02110-1301  USA
 */
package org.simexplorer.ide.ui;

import org.simexplorer.core.workflow.methods.EditorPanel;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.util.Lookup;
import org.openmole.core.implementation.task.ExplorationTask;

public class PanelFactory {

    private static final Logger LOGGER = Logger.getLogger(PanelFactory.class.getName());
    private static Map<Class, Class> cache;
    private static Map<Class, EditorPanel> cache2;

    static {
        cache2 = new HashMap<Class, EditorPanel>();
        Collection<? extends EditorPanel> panels = Lookup.getDefault().lookupAll(EditorPanel.class);
        for (EditorPanel panel : panels) {
            for (Class type : panel.getTypesEditable()) {
                cache2.put(type, panel);
            }
        }
        LOGGER.log(Level.INFO,"Editors found: "+cache2.toString());
    }

    // TODO factorize both methods
    public static <T> EditorPanel<T> getEditor(Class<T> keyType) {
        if (cache == null) {
            cache = new HashMap<Class, Class>();
        }
        LOGGER.log(Level.INFO, "Entering getEditor - keyType = " + keyType);
        if (cache2.containsKey(keyType)) {
            LOGGER.log(Level.INFO, cache2.get(keyType) + " is in lookup cache");
            return cache2.get(keyType);
        } else if (!cache.containsKey(keyType)) {
            LOGGER.log(Level.INFO, " isn't in cache");
            Class<?> editorType = null;
            try {
                editorType = Class.forName(keyType.getCanonicalName() + "Panel");
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "No panel found for the class "
                        + keyType + "\n" + ex.toString());
            }
            LOGGER.log(Level.INFO, " Panel found: " + editorType);
            if (editorType != null) {
                cache.put(keyType, editorType);
                try {
                    EditorPanel<T> processorEditor = (EditorPanel<T>) editorType.newInstance();
                    return processorEditor;
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Error while trying to create the panel "
                            + editorType + "\n", ex);
                }
            }
            cache.put(keyType, null);
            return null;
        } else {
            Class editorType = cache.get(keyType);
            LOGGER.log(Level.INFO, editorType + " is in cache");
            if (editorType == null) {
                return null;
            } else {
                try {
                    return (EditorPanel<T>) editorType.newInstance();
                } catch (InstantiationException ex) {
                    LOGGER.log(Level.WARNING, "Error while trying to instanciate "
                            + editorType, ex);
                    cache.put(keyType, null);
                    return null;
                } catch (IllegalAccessException ex) {
                    LOGGER.log(Level.WARNING, "Error while trying to instanciate "
                            + editorType, ex);
                    cache.put(keyType, null);
                    return null;
                }
            }
        }
    }

    public static <T> EditorPanel<T> getEditorPanelFor(T o) {
        if (cache == null) {
            cache = new HashMap<Class, Class>();
        }
        if (o instanceof ExplorationTask) {
            if (((ExplorationTask) o).getPlan() != null) {
                // TODO add a preview panel
                o = (T) ((ExplorationTask) o).getPlan();
            }
        }
        LOGGER.log(Level.INFO, "Entering getEditorPanelFor - o = " + o);
        if (cache2.containsKey(o.getClass())) {
            LOGGER.log(Level.INFO, cache2.get(o.getClass()) + " is in lookup cache");
            EditorPanel editor = cache2.get(o.getClass());
            editor.setObjectEdited(o);
            return editor;
        } else if (!cache.containsKey(o.getClass())) {
            LOGGER.log(Level.INFO, " isn't in cache");
            Class<?> type = null;
            try {
                if (o instanceof ExplorationTask) {
                    if (((ExplorationTask) o).getPlan()
                            != null) {
                        // TODO add a preview panel
                        o = (T) ((ExplorationTask) o).getPlan();
                    }
                }
                type = Class.forName(o.getClass().getCanonicalName() + "Panel");
                LOGGER.log(Level.INFO, " Panel found: " + type);
                if (type != null) {
                    cache.put(o.getClass(), type);
                    EditorPanel<T> processorEditor = (EditorPanel<T>) type.newInstance();
                    processorEditor.setObjectEdited(o);
                    return processorEditor;
                }
            } catch (Exception ex) {
                LOGGER.log(Level.INFO, "No panel found for the class "
                        + o.getClass(), ex);
            }
            cache.put(o.getClass(), null);
            return null;
        } else {
            Class type = cache.get(o.getClass());
            LOGGER.log(Level.INFO, type + " is in cache");
            if (type == null) {
                return null;
            } else {
                try {
                    EditorPanel<T> processorEditor = (EditorPanel<T>) type.newInstance();
                    processorEditor.setObjectEdited(o);
                    return processorEditor;
                    // TODO synchronize design method
                } catch (InstantiationException ex) {
                    LOGGER.log(Level.WARNING, "Error while trying to instanciate "
                            + type, ex);
                    cache.put(o.getClass(), null);
                    return null;
                } catch (IllegalAccessException ex) {
                    LOGGER.log(Level.WARNING, "Error while trying to instanciate "
                            + type, ex);
                    cache.put(o.getClass(), null);
                    return null;
                }
            }
        }
    }
}
