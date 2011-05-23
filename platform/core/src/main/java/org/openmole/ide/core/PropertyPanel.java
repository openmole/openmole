/*
 * Copyright (C) 2011 Mathieu leclaire <mathieu.leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.ide.core;

import java.awt.Component;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import org.openmole.ide.core.commons.Constants;
import org.openmole.ide.core.palette.ElementFactories;
import org.openmole.ide.core.palette.PaletteElementFactory;
import org.openmole.ide.core.control.MoleScenesManager;
import org.openmole.ide.core.palette.ModelElementFactory;
import org.openmole.ide.core.properties.IPanelUI;
import org.openmole.ide.core.properties.PanelUIData;
import scala.collection.JavaConversions;
import scala.collection.JavaConversions.*;

public class PropertyPanel extends javax.swing.JPanel {

    private static PropertyPanel instance;
    private PaletteElementFactory currentElement;
    private IPanelUI currentPanelUI;
    private Boolean initEntity = false;
    private Map<String, Boolean> locked = new HashMap<String, Boolean>();

    public PropertyPanel() {
        initComponents();
        locked.put(Constants.TASK(), false);
        locked.put(Constants.PROTOTYPE(), false);
        locked.put(Constants.SAMPLING(), false);
        locked.put(Constants.ENVIRONMENT(), false);

        typeComboBox.setRenderer(new ItemRenderer());
        entityPanelScrollPane.setVisible(false);
    }

    public String getNameTextField() {
        return nameTextField.getText();
    }

//    public void save() {
//        if (!initEntity) {
//            if (currentElement != null) {
//                if (currentPanelUI != null) {
//                    PanelUIData pdata = currentPanelUI.saveContent();
//                    pdata.name_$eq(nameTextField.getText());
//                    currentElement.entity().updatePanelUIData(pdata);
//                }
//            }
//        }
//    }

    public void displayCurrentEntity(){
        displayCurrentEntity(currentElement);
    }
    
    public void displayCurrentEntity(PaletteElementFactory elementFactory) {
        currentElement = elementFactory;
        currentPanelUI = elementFactory.entity().factoryUI().buildPanelUI();

        nameTextField.setText(elementFactory.entity().panelUIData().name());
        typeComboBox.removeAllItems();
        typeComboBox.setEnabled(false);

        currentPanelUI.loadContent(currentElement.entity().panelUIData());

        updateViewport((JPanel) currentPanelUI);
        entityPanelScrollPane.setVisible(true);
        locked.put(elementFactory.entity().entityType(), false);
        initEntity = false;
    }

    public void updateViewport(JPanel panel) {
        entityPanelScrollPane.getViewport().removeAll();
        entityPanelScrollPane.setViewportView(panel);

    }

    private void save() {
        if (initEntity) {
            if (nameTextField.getText().length() != 0) {
                ModelElementFactory currentModelElementFactory = (ModelElementFactory) (typeComboBox.getSelectedItem());
                PaletteElementFactory element = currentModelElementFactory.buildPaletteElementFactory(nameTextField.getText());
                MoleSceneTopComponent.getDefault().refreshPalette();
                displayCurrentEntity(element);
                locked.put(currentModelElementFactory.entityType(), false);
            }
        } else {
            if (currentElement != null) {
                if (currentPanelUI != null) {
                    PanelUIData pdata = currentPanelUI.saveContent();
                    pdata.name_$eq(nameTextField.getText());
                    currentElement.entity().updatePanelUIData(pdata);
                }
            }
        }
        MoleSceneTopComponent.getDefault().refreshPalette();
        initEntity = false;
    }

    private void initNewEntity(String entityType) {

        typeComboBox.setEnabled(true);
        typeComboBox.removeAllItems();
        for (ModelElementFactory pef : JavaConversions.asJavaIterable(ElementFactories.modelElements().apply(entityType))) {
            typeComboBox.addItem(pef);
        }
        if (entityPanelScrollPane.getViewport() != null) {
            entityPanelScrollPane.getViewport().removeAll();
            //   nameTextField.setText(MoleScenesManager.getName(entityType, locked.get(entityType)));
            nameTextField.setText(MoleScenesManager.incrementCounter(entityType));
        }
        initEntity = true;

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
        nameLabel = new javax.swing.JLabel();
        nameTextField = new javax.swing.JTextField();
        typeLabel = new javax.swing.JLabel();
        typeComboBox = new javax.swing.JComboBox();
        entityPanelScrollPane = new javax.swing.JScrollPane();
        saveButton = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        environmentToggleButton = new javax.swing.JToggleButton();
        prototypeToggleButton = new javax.swing.JToggleButton();
        taskToggleButton = new javax.swing.JToggleButton();
        samplingToggleButton = new javax.swing.JToggleButton();
        cancelButton = new javax.swing.JButton();

        jButton1.setText(org.openide.util.NbBundle.getMessage(PropertyPanel.class, "PropertyPanel.jButton1.text")); // NOI18N

        nameLabel.setText(org.openide.util.NbBundle.getMessage(PropertyPanel.class, "PropertyPanel.nameLabel.text")); // NOI18N

        nameTextField.setText(org.openide.util.NbBundle.getMessage(PropertyPanel.class, "PropertyPanel.nameTextField.text")); // NOI18N

        typeLabel.setText(org.openide.util.NbBundle.getMessage(PropertyPanel.class, "PropertyPanel.typeLabel.text")); // NOI18N

        typeComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                typeComboBoxActionPerformed(evt);
            }
        });

        saveButton.setText(org.openide.util.NbBundle.getMessage(PropertyPanel.class, "PropertyPanel.saveButton.text")); // NOI18N
        saveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveButtonActionPerformed(evt);
            }
        });

        environmentToggleButton.setText(org.openide.util.NbBundle.getMessage(PropertyPanel.class, "PropertyPanel.environmentToggleButton.text")); // NOI18N
        environmentToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                environmentToggleButtonActionPerformed(evt);
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

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(taskToggleButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(prototypeToggleButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(samplingToggleButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(environmentToggleButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel1Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {environmentToggleButton, prototypeToggleButton, samplingToggleButton, taskToggleButton});

        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(taskToggleButton)
                    .addComponent(prototypeToggleButton)
                    .addComponent(samplingToggleButton)
                    .addComponent(environmentToggleButton))
                .addContainerGap())
        );

        cancelButton.setText(org.openide.util.NbBundle.getMessage(PropertyPanel.class, "PropertyPanel.cancelButton.text")); // NOI18N
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(entityPanelScrollPane, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 421, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(nameLabel)
                                    .addComponent(typeLabel))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(typeComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(nameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 289, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(cancelButton, 0, 0, Short.MAX_VALUE)
                                    .addComponent(saveButton, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(3, 3, 3)))
                        .addGap(27, 27, 27))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(14, 14, 14)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(saveButton)
                    .addComponent(nameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(typeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cancelButton))
                .addGap(20, 20, 20)
                .addComponent(entityPanelScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 254, Short.MAX_VALUE)
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.CENTER, layout.createSequentialGroup()
                .addGap(122, 122, 122)
                .addComponent(typeLabel)
                .addGap(292, 292, 292))
            .addGroup(layout.createSequentialGroup()
                .addGap(86, 86, 86)
                .addComponent(nameLabel)
                .addGap(328, 328, 328))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void prototypeToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_prototypeToggleButtonActionPerformed
        // setButtons(true);
        initNewEntity(Constants.PROTOTYPE());
    }//GEN-LAST:event_prototypeToggleButtonActionPerformed

    private void samplingToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_samplingToggleButtonActionPerformed
        // setButtons(false);
        initNewEntity(Constants.SAMPLING());
    }//GEN-LAST:event_samplingToggleButtonActionPerformed

    private void environmentToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_environmentToggleButtonActionPerformed
        // setButtons(true);
        initNewEntity(Constants.ENVIRONMENT());
    }//GEN-LAST:event_environmentToggleButtonActionPerformed

    private void typeComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_typeComboBoxActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_typeComboBoxActionPerformed

    private void taskToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_taskToggleButtonActionPerformed
        // setButtons(false);
        initNewEntity(Constants.TASK());

    }//GEN-LAST:event_taskToggleButtonActionPerformed

    private void saveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveButtonActionPerformed
        // TODO add your handling code here:
        save();
    }//GEN-LAST:event_saveButtonActionPerformed

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        displayCurrentEntity();
    }//GEN-LAST:event_cancelButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelButton;
    private javax.swing.JScrollPane entityPanelScrollPane;
    private javax.swing.JToggleButton environmentToggleButton;
    private javax.swing.JButton jButton1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JLabel nameLabel;
    private javax.swing.JTextField nameTextField;
    private javax.swing.JToggleButton prototypeToggleButton;
    private javax.swing.JToggleButton samplingToggleButton;
    private javax.swing.JButton saveButton;
    private javax.swing.JToggleButton taskToggleButton;
    private javax.swing.JComboBox typeComboBox;
    private javax.swing.JLabel typeLabel;
    // End of variables declaration//GEN-END:variables

    class ItemRenderer extends BasicComboBoxRenderer {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value != null) {
                if (value.getClass().equals(ModelElementFactory.class)) {
                    ModelElementFactory pef = (ModelElementFactory) value;
                    setText(pef.displayName());
                }
            }
            return this;
        }
    }

    public static synchronized PropertyPanel getDefault() {
        if (instance == null) {
            instance = new PropertyPanel();
        }
        return instance;
    }
}
