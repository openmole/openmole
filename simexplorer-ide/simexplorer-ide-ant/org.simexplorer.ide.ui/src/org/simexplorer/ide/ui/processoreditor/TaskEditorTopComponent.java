/*
 *  Copyright © 2007, 2008, Cemagref
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
package org.simexplorer.ide.ui.processoreditor;

import java.awt.CardLayout;
import java.awt.Component;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ToolTipManager;
import org.openide.awt.UndoRedo;
import org.openide.util.Lookup;
import org.openide.util.Lookup.Result;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import org.simexplorer.ide.ui.PanelFactory;
import org.simexplorer.ide.ui.ProcessorChooserPanel;
import org.simexplorer.ide.ui.applicationexplorer.ApplicationsTopComponent;
import org.simexplorer.core.workflow.methods.EditorPanel;
import org.simexplorer.core.workflow.model.metada.MetadataLoader;
import org.openmole.core.model.plan.IPlan;
import org.openmole.core.implementation.task.GenericTask;
import org.openmole.commons.tools.structure.Duo;
import org.simexplorer.core.workflow.model.metada.Metadata;
import org.simexplorer.ui.ide.workflow.model.ExplorationTreeTask;
//import org.openide.util.Utilities;

/**
 * Top component which displays something.
 */
public final class TaskEditorTopComponent extends TopComponent implements LookupListener {

    private static TaskEditorTopComponent instance;
    /** path to the icon used by the component and its open action */
//    static final String ICON_PATH = "SET/PATH/TO/ICON/HERE";
    private static final String PREFERRED_ID = "TaskEditorTopComponent";
    // Lookup result on processors object
    private Result result;
    private CardLayout componentEditorCardLayout;
    private CardLayout designEditorCardLayout;
//    private Map<String, IPlan> methodsByName;
    private Map<Object, JComponent> componentEditors;
    private Map<String, JComponent> designEditors;
    private Map<Class, IPlan> designObjects;
    private Map<EditorPanel, UndoRedo.Manager> undoRedoManagers;
    private UndoRedo.Manager undoRedoManager;
    private static final String NO_EDITOR_PANEL = "no_editor";
    private static final String NO_SELECTED_COMPONENT = "no_selected_component";
    private static final String DESIGN_CHOICE_PANEL = "designConfigurationPanel";
    private static String helpMetaDataKey = NbBundle.getMessage(ProcessorChooserPanel.class, "ProcessorChooserPanel.helpMetaDataKey");
    private static String nohelpProvided = NbBundle.getMessage(ProcessorChooserPanel.class, "ProcessorChooserPanel.nohelpProvided");
    private Map<String, Duo<IPlan, Metadata>> methodsByName;

    private TaskEditorTopComponent() {
        initComponents();
        setName(NbBundle.getMessage(TaskEditorTopComponent.class, "CTL_TaskEditorTopComponent"));
        setToolTipText(NbBundle.getMessage(TaskEditorTopComponent.class, "HINT_TaskEditorTopComponent"));
//        setIcon(Utilities.loadImage(ICON_PATH, true));
        putClientProperty(TopComponent.PROP_CLOSING_DISABLED, Boolean.TRUE);

        methodsByName = new HashMap<String, Duo<IPlan, Metadata>>();
        Collection<? extends IPlan> methods = Lookup.getDefault().lookupAll(IPlan.class);

        for (IPlan method : methods) {
            Metadata metadata = MetadataLoader.loadMetadata(method);
            methodsByName.put(metadata.get("name"), new Duo<IPlan, Metadata>(method, metadata));
        }
        designComboBox.setModel(new DefaultComboBoxModel(methodsByName.keySet().toArray()));
        designEditors = new HashMap<String, JComponent>();
        designEditorCardLayout = (CardLayout) designConfigurationPanel.getLayout();
        designConfigurationPanel.add(new NoEditorFoundPanel(), NO_EDITOR_PANEL);
        designObjects = new HashMap<Class, IPlan>();
        designComboBox.setSelectedIndex(-1);
        designComboBox.setRenderer(new DesignComboBoxRenderer());

        componentEditors = new HashMap<Object, JComponent>();
        componentEditorCardLayout = (CardLayout) componentEditorPanel.getLayout();
        componentEditorPanel.add(designChoicePanel, DESIGN_CHOICE_PANEL);
        componentEditorPanel.add(new NoEditorFoundPanel(), NO_EDITOR_PANEL);
        componentEditorPanel.add(new JLabel("<html>No component of exploration selected.</html>"), NO_SELECTED_COMPONENT);

        undoRedoManagers = new HashMap<EditorPanel, UndoRedo.Manager>();
        componentEditorCardLayout.show(componentEditorPanel, NO_SELECTED_COMPONENT);

    }

    @Override
    public void resultChanged(LookupEvent lookupEvent) {
        Collection c = result.allInstances();
        if (!c.isEmpty()) {
            final Object selectedObject = c.iterator().next();
            // Displaying the panel according to the type of processor
            if (ExplorationTreeTask.class.isAssignableFrom(selectedObject.getClass())) {
                // particular case of exploration component
                componentEditorCardLayout.show(componentEditorPanel, DESIGN_CHOICE_PANEL);
                undoRedoManager = null;
                // update method selection
                IPlan method = ApplicationsTopComponent.findInstance().getExplorationApplication().getExplorationTask().getPlan();
                if (method != null) {
                    designComboBox.setSelectedItem(MetadataLoader.loadMetadata(method).get("name"));
                }
            } else {
                // common case. We have to find the EditorPanel to display
                EditorPanel editor = (EditorPanel) componentEditors.get(selectedObject);
                if (editor != null) {
                    // component editor is already cached
                    // refresh the editor content
                    editor.setObjectEdited(selectedObject);
                    componentEditorCardLayout.show(componentEditorPanel, Integer.toString(selectedObject.hashCode()));
                    undoRedoManager = undoRedoManagers.get(editor);
                } else {
                    editor = PanelFactory.getEditorPanelFor(selectedObject);
                    if (editor != null) {
                        // add the editor
                        componentEditors.put(selectedObject, editor);
                        componentEditorPanel.add(editor, Integer.toString(selectedObject.hashCode()));
                        componentEditorCardLayout.show(componentEditorPanel, Integer.toString(selectedObject.hashCode()));
                        undoRedoManager = new UndoRedo.Manager();
                        editor.addUndoableEditListener(undoRedoManager);
                        undoRedoManagers.put(editor, undoRedoManager);
                    } else {
                        componentEditorCardLayout.show(componentEditorPanel, NO_EDITOR_PANEL);
                        undoRedoManager = null;
                    }
                }
            }
            componentEditorPanel.revalidate();
        }
    }

    @Override
    public UndoRedo getUndoRedo() {
        if (undoRedoManager != null) {
            return undoRedoManager;
        } else {
            return super.getUndoRedo();
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        designChoicePanel = new javax.swing.JPanel();
        designComboBox = new javax.swing.JComboBox();
        designConfigurationPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        componentEditorPanel = new javax.swing.JPanel();

        designComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                designComboBoxActionPerformed(evt);
            }
        });

        designConfigurationPanel.setLayout(new java.awt.CardLayout());

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(TaskEditorTopComponent.class, "TaskEditorTopComponent.jLabel1.text")); // NOI18N

        javax.swing.GroupLayout designChoicePanelLayout = new javax.swing.GroupLayout(designChoicePanel);
        designChoicePanel.setLayout(designChoicePanelLayout);
        designChoicePanelLayout.setHorizontalGroup(
            designChoicePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(designChoicePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(designChoicePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(designConfigurationPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 429, Short.MAX_VALUE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 429, Short.MAX_VALUE)
                    .addComponent(designComboBox, javax.swing.GroupLayout.Alignment.TRAILING, 0, 429, Short.MAX_VALUE))
                .addContainerGap())
        );
        designChoicePanelLayout.setVerticalGroup(
            designChoicePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, designChoicePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(designComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(designConfigurationPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 393, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        componentEditorPanel.setLayout(new java.awt.CardLayout());

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(componentEditorPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 376, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(componentEditorPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 276, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void designComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_designComboBoxActionPerformed
        if (designComboBox.getSelectedIndex() >= 0) {
            if (designEditors.containsKey(designComboBox.getSelectedItem())) {
                // display configuration panel already created
                designEditorCardLayout.show(designConfigurationPanel, (String) designComboBox.getSelectedItem());
            } else {
                // create configuration panel
                JComponent component = PanelFactory.getEditor(methodsByName.get(designComboBox.getSelectedItem()).getLeft().getClass());
                // caching and displaying
                IPlan design = methodsByName.get(designComboBox.getSelectedItem()).getLeft();
                designObjects.put(methodsByName.get(designComboBox.getSelectedItem()).getClass(), design);
                if (component != null) {
                    ((EditorPanel<IPlan>) component).setObjectEdited(design);
                    designEditors.put((String) designComboBox.getSelectedItem(), component);
                    designConfigurationPanel.add(component, (String) designComboBox.getSelectedItem());
                    designEditorCardLayout.show(designConfigurationPanel, (String) designComboBox.getSelectedItem());

                } else {
                    designEditorCardLayout.show(designConfigurationPanel, NO_EDITOR_PANEL);
                }
            }
            ApplicationsTopComponent.findInstance().getExplorationApplication().getExplorationTask().setPlan(designObjects.get(methodsByName.get(designComboBox.getSelectedItem()).getClass()));
            designConfigurationPanel.revalidate();
        }
}//GEN-LAST:event_designComboBoxActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel componentEditorPanel;
    private javax.swing.JPanel designChoicePanel;
    private javax.swing.JComboBox designComboBox;
    private javax.swing.JPanel designConfigurationPanel;
    private javax.swing.JLabel jLabel1;
    // End of variables declaration//GEN-END:variables

    /**
     * Gets default instance. Do not use directly: reserved for *.settings files only,
     * i.e. deserialization routines; otherwise you could get a non-deserialized instance.
     * To obtain the singleton instance, use {@link #findInstance}.
     */
    public static synchronized TaskEditorTopComponent getDefault() {
        if (instance == null) {
            instance = new TaskEditorTopComponent();
        }
        return instance;
    }

    /**
     * Obtain the TaskEditorTopComponent instance. Never call {@link #getDefault} directly!
     */
    public static synchronized TaskEditorTopComponent findInstance() {
        TopComponent win = WindowManager.getDefault().findTopComponent(PREFERRED_ID);
        if (win == null) {
            Logger.getLogger(TaskEditorTopComponent.class.getName()).warning(
                    "Cannot find " + PREFERRED_ID + " component. It will not be located properly in the window system.");
            return getDefault();
        }
        if (win instanceof TaskEditorTopComponent) {
            return (TaskEditorTopComponent) win;
        }
        Logger.getLogger(TaskEditorTopComponent.class.getName()).warning(
                "There seem to be multiple components with the '" + PREFERRED_ID + "' ID. That is a potential source of errors and unexpected behavior.");
        return getDefault();
    }

    @Override
    public int getPersistenceType() {
        return TopComponent.PERSISTENCE_ALWAYS;
    }

    @Override
    public void componentOpened() {
        Lookup.Template tpl = new Lookup.Template(GenericTask.class);
        result = Utilities.actionsGlobalContext().lookup(tpl);
        result.addLookupListener(this);
    }

    @Override
    public void componentClosed() {
        result.removeLookupListener(this);
        result = null;
    }

    /** replaces this in object stream */
    @Override
    public Object writeReplace() {
        return new ResolvableHelper();
    }

    @Override
    protected String preferredID() {
        return PREFERRED_ID;
    }

    final static class ResolvableHelper implements Serializable {

        private static final long serialVersionUID = 1L;

        public Object readResolve() {
            return TaskEditorTopComponent.getDefault();
        }
    }

    class DesignComboBoxRenderer extends JLabel implements ListCellRenderer {

        public DesignComboBoxRenderer() {
            setOpaque(true);
            setHorizontalAlignment(LEFT);
            setVerticalAlignment(CENTER);
        }

        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                boolean cellHasFocus) {
            if (value == null) {
                setText("");
            } else {
                String text = "<html><body>" + value.toString() + "</body></html>";
                String description = (methodsByName.get(list.getModel().getElementAt(index)) != null) ? //                String description = (processorsByName.get(processorsComboBox.getSelectedItem())!=null)?
                        methodsByName.get(list.getModel().getElementAt(index)).getRight().get(helpMetaDataKey) : null;

                description = (description != null) ? description : nohelpProvided;
                String toolTip = "<html><body>  <b>" + value.toString() + "</b> <i>" + description + "</i>  </body></html>";
                setToolTipText(toolTip);
// forces tooltip staying on top
                ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
                list.setOpaque(true);
                setText(text);
            }
            setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
            setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
            return this;
        }
    }
}
