/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * PropertyPanel.java
 *
 * Created on 20 avr. 2011, 11:20:26
 */
package org.openmole.ide.core;

import java.awt.Component;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import org.openide.util.Lookup;
import org.openmole.ide.core.commons.Constants;
import org.openmole.ide.core.palette.ElementFactories;
import org.openmole.ide.core.palette.PaletteElementFactory;
import org.openmole.ide.core.control.MoleScenesManager;
import org.openmole.ide.core.palette.PaletteSupport;
import org.openmole.ide.core.properties.IFactoryUI;
import org.openmole.ide.core.properties.IPanelUI;
import org.openmole.ide.core.properties.IPrototypeFactoryUI;
import org.openmole.ide.core.properties.PanelUI;
import scala.collection.JavaConversions;
import scala.collection.JavaConversions.*;
import scala.reflect.generic.Constants.Constant;

/**
 *
 * @author mathieu
 */
public class PropertyPanel extends javax.swing.JPanel {

    private static PropertyPanel instance;
    private IPanelUI currentPanelUI;

    /** Creates new form PropertyPanel */
    public PropertyPanel() {
        initComponents();

        typeComboBox.setRenderer(new ItemRenderer());
        // setEditGraphicalContext(false);

        entityPanelScrollPane.getViewport().addContainerListener(new PanelListener());
        entityPanelScrollPane.setVisible(false);
    }

    public String getNameTextField() {
        return nameTextField.getText();
    }

    public void displayCurrentEntity(PaletteElementFactory elementFactory) {
        // JPanel p = (JPanel)entity.panel();
        createButton.setVisible(false);
        nameTextField.setText(elementFactory.factory().panelUIData().name());
        typeComboBox.removeAllItems();
        typeComboBox.addItem(elementFactory.factory().coreClass().getSimpleName());
        // typeComboBox.setEditable(false);

        currentPanelUI = elementFactory.factory().buildPanelUI();
        updateViewport((JPanel) currentPanelUI);
        entityPanelScrollPane.setVisible(true);
    }

    public void updateViewport(JPanel panel) {
        entityPanelScrollPane.getViewport().removeAll();
        entityPanelScrollPane.setViewportView(panel);

    }

    private void create() {
        if (nameTextField.getText().length() != 0) {
            PaletteElementFactory currentElementFactory = (PaletteElementFactory) (typeComboBox.getSelectedItem());
            PaletteElementFactory pef = new PaletteElementFactory(nameTextField.getText(), currentElementFactory.thumbPath(), Constants.simpleEntityName(currentElementFactory.entityType()), currentElementFactory.factoryUIClass());
            ElementFactories.addElement(pef);
            MoleSceneTopComponent.getDefault().refreshPalette();
            displayCurrentEntity(pef);
            
            //initNewEntity(currentElementFactory.entityType());
            PaletteSupport.selectItem(Constants.simpleEntityName(currentElementFactory.entityType()),currentElementFactory.displayName());
        }
    }
//    private String save() {
//        if (newMode) {
//            //  ((IFactoryUI) typeComboBox.getSelectedItem()).buildEntity(nameTextField.getText());
//            // ElementFactories.addPrototypeElement(new PaletteElementFactory(nameTextField.getText(), ((IFactoryUI) typeComboBox.getSelectedItem())));
//            MoleSceneTopComponent.getDefault().refreshPalette();
//        }
//        nameTextField.setEnabled(false);
//        newMode = false;
//        return "Edit";
//    }
//
//    private void setNewGraphicalContext() {
//        newMode = true;
//        nameTextField.setText("");
//        setEditGraphicalContext(true);
//        // nameTextField.setEnabled(true);
//        //  editToggleButton.setEnabled(true);
//        typeComboBox.setEditable(true);
//    }
//
//    private void setEditGraphicalContext(Boolean b) {
//        nameTextField.setEnabled(b);
//        editToggleButton.setText(b ? "Save" : save());
//    }
//
//    private void setButtons(Boolean b) {
//        newToggleButton.setEnabled(b);
//        editToggleButton.setEnabled(true);
//        typeComboBox.removeAllItems();
//        typeComboBox.setEditable(b);
//    }

    private void initNewEntity(String entityType) {
        //currentEntity = entityType;
        createButton.setVisible(true);
        typeComboBox.removeAllItems();
        for (PaletteElementFactory pef : JavaConversions.asJavaIterable(ElementFactories.paletteElements().apply(entityType))) {
            typeComboBox.addItem(pef);
        }
        if (entityPanelScrollPane.getViewport() != null) {
            entityPanelScrollPane.getViewport().removeAll();
            nameTextField.setText(MoleScenesManager.incrementCounter(entityType));
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jButton1 = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        nameTextField = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        typeComboBox = new javax.swing.JComboBox();
        prototypeToggleButton = new javax.swing.JToggleButton();
        taskToggleButton = new javax.swing.JToggleButton();
        samplingToggleButton = new javax.swing.JToggleButton();
        environmentToggleButton = new javax.swing.JToggleButton();
        entityPanelScrollPane = new javax.swing.JScrollPane();
        createButton = new javax.swing.JButton();

        jButton1.setText(org.openide.util.NbBundle.getMessage(PropertyPanel.class, "PropertyPanel.jButton1.text")); // NOI18N

        jLabel1.setText(org.openide.util.NbBundle.getMessage(PropertyPanel.class, "PropertyPanel.jLabel1.text")); // NOI18N

        nameTextField.setText(org.openide.util.NbBundle.getMessage(PropertyPanel.class, "PropertyPanel.nameTextField.text")); // NOI18N

        jLabel2.setText(org.openide.util.NbBundle.getMessage(PropertyPanel.class, "PropertyPanel.jLabel2.text")); // NOI18N

        typeComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                typeComboBoxActionPerformed(evt);
            }
        });

        prototypeToggleButton.setText(org.openide.util.NbBundle.getMessage(PropertyPanel.class, "PropertyPanel.prototypeToggleButton.text")); // NOI18N
        prototypeToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                prototypeToggleButtonActionPerformed(evt);
            }
        });

        taskToggleButton.setText(org.openide.util.NbBundle.getMessage(PropertyPanel.class, "PropertyPanel.taskToggleButton.text")); // NOI18N
        taskToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                taskToggleButtonActionPerformed(evt);
            }
        });

        samplingToggleButton.setText(org.openide.util.NbBundle.getMessage(PropertyPanel.class, "PropertyPanel.samplingToggleButton.text")); // NOI18N
        samplingToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                samplingToggleButtonActionPerformed(evt);
            }
        });

        environmentToggleButton.setText(org.openide.util.NbBundle.getMessage(PropertyPanel.class, "PropertyPanel.environmentToggleButton.text")); // NOI18N
        environmentToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                environmentToggleButtonActionPerformed(evt);
            }
        });

        createButton.setText(org.openide.util.NbBundle.getMessage(PropertyPanel.class, "PropertyPanel.createButton.text")); // NOI18N
        createButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                createButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(entityPanelScrollPane, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 422, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel2)
                                    .addComponent(jLabel1))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(typeComboBox, 0, 262, Short.MAX_VALUE)
                                    .addComponent(nameTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 262, Short.MAX_VALUE)))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(taskToggleButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(prototypeToggleButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(samplingToggleButton)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(createButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(environmentToggleButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {environmentToggleButton, prototypeToggleButton, samplingToggleButton, taskToggleButton});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(taskToggleButton)
                    .addComponent(prototypeToggleButton)
                    .addComponent(samplingToggleButton)
                    .addComponent(environmentToggleButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(createButton, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(nameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel1))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                            .addComponent(jLabel2)
                            .addComponent(typeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addGap(18, 18, 18)
                .addComponent(entityPanelScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 284, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void prototypeToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_prototypeToggleButtonActionPerformed
        // setButtons(true);
        initNewEntity(Constants.PROTOTYPE_MODEL());
    }//GEN-LAST:event_prototypeToggleButtonActionPerformed

    private void samplingToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_samplingToggleButtonActionPerformed
        // setButtons(false);
        initNewEntity(Constants.SAMPLING_MODEL());
    }//GEN-LAST:event_samplingToggleButtonActionPerformed

    private void environmentToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_environmentToggleButtonActionPerformed
        // setButtons(true);
        initNewEntity(Constants.ENVIRONMENT_MODEL());
    }//GEN-LAST:event_environmentToggleButtonActionPerformed

    private void typeComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_typeComboBoxActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_typeComboBoxActionPerformed

    private void taskToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_taskToggleButtonActionPerformed
        // setButtons(false);
        initNewEntity(Constants.TASK_MODEL());

    }//GEN-LAST:event_taskToggleButtonActionPerformed

    private void createButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_createButtonActionPerformed
        // TODO add your handling code here:

        create();
    }//GEN-LAST:event_createButtonActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton createButton;
    private javax.swing.JScrollPane entityPanelScrollPane;
    private javax.swing.JToggleButton environmentToggleButton;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JTextField nameTextField;
    private javax.swing.JToggleButton prototypeToggleButton;
    private javax.swing.JToggleButton samplingToggleButton;
    private javax.swing.JToggleButton taskToggleButton;
    private javax.swing.JComboBox typeComboBox;
    // End of variables declaration//GEN-END:variables

    class ItemRenderer extends BasicComboBoxRenderer {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value != null) {
                if (value.getClass().equals(PaletteElementFactory.class)) {
                    PaletteElementFactory pef = (PaletteElementFactory) value;
                    setText(pef.displayName());
                }
            }
            return this;
        }
    }

    class PanelListener implements ContainerListener {

        @Override
        public void componentAdded(ContainerEvent ce) {
            System.out.println("PanelUI added " + currentPanelUI.getClass());
            currentPanelUI.loadContent();
        }

        @Override
        public void componentRemoved(ContainerEvent ce) {
            System.out.println("PanelUI closed " + currentPanelUI.getClass());
            currentPanelUI.saveContent();
        }
    }

    public static synchronized PropertyPanel getDefault() {
        if (instance == null) {
            instance = new PropertyPanel();
        }
        return instance;
    }
}
