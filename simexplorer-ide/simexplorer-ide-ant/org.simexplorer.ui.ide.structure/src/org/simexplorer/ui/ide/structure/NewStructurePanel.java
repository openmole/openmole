/*
 *
 *  Copyright (c) 2009, Cemagref
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
package org.simexplorer.ui.ide.structure;

import java.util.HashMap;
import java.util.Map;
import javax.swing.DefaultComboBoxModel;
import org.openmole.core.structuregenerator.ComplexNode;
import org.openmole.core.structuregenerator.PrototypeNode;
import org.openmole.core.structuregenerator.SequenceNode;
import org.openmole.core.structuregenerator.StructureNode;
import org.openmole.core.implementation.data.Prototype;
import org.simexplorer.ui.tools.SimpleWizard;
import org.simexplorer.ui.ide.workflow.model.MetadataProxy;

public class NewStructurePanel extends SimpleWizard {

    private static final String COMPLEX_TYPE = "Complex";
    private Map<String, Class> types;
    private DefaultComboBoxModel comboBoxModel;
    private StructureNode structureNode;
    // used to store the nested prototype in a SequenceNode
    private PrototypeNode innerPrototype;
    private ComplexNode parent;

    /** Creates new form NewStructurePanel */
    public NewStructurePanel() {
        types = new HashMap<String, Class>();
        types.put("Integer", Integer.class);
        types.put("Double", Double.class);
        types.put("String", String.class);
        types.put("Boolean", Boolean.class);
        types.put(COMPLEX_TYPE, null);
        comboBoxModel = new DefaultComboBoxModel(types.keySet().toArray(new String[]{}));
        initComponents();

    }

    public NewStructurePanel(StructureNode structureNode, ComplexNode parent) {
        this();
        this.structureNode = structureNode;
        this.parent = parent;
        this.nameTextField.setText(structureNode.getName());
        // fetching node type
        StructureNode node = structureNode;
        this.sequenceCheckBox.setSelected(structureNode instanceof SequenceNode);
        while (node instanceof SequenceNode) {
            node = ((SequenceNode) structureNode).getInnerNode();
        }
        if (node instanceof PrototypeNodeNode) {
            this.innerPrototype = (PrototypeNode) node;
            this.dataTypeComboBox.setSelectedItem(innerPrototype.getPrototype().getType().getSimpleName());
            comboBoxModel.removeElement(COMPLEX_TYPE);
        } else {
            this.dataTypeComboBox.setSelectedItem(COMPLEX_TYPE);
            this.dataTypeComboBox.setEnabled(false);
        }
        // put the metadata field
        jTextAreaDescription.setText(MetadataProxy.getMetadata(structureNode, "description"));
        jTextFieldUnit.setText(MetadataProxy.getMetadata(structureNode, "units"));
    }

    public StructureNode getStructure() {
        if (this.structureNode == null) {
            if (dataTypeComboBox.getSelectedItem().equals(COMPLEX_TYPE)) {
                this.structureNode = new ComplexNode(nameTextField.getText());
            } else {
                this.structureNode = new PrototypeNode(new Prototype(nameTextField.getText(), types.get(dataTypeComboBox.getSelectedItem())));
            }
            if (sequenceCheckBox.isSelected()) {
                this.structureNode = new SequenceNode(structureNode);
            }
        } else {
            // apply sequence node option
            if (sequenceCheckBox.isSelected()) {
                if (!(structureNode instanceof SequenceNode)) {
                    SequenceNode sequenceNode = new SequenceNode(structureNode);
                    parent.remove(structureNode);
                    parent.add(sequenceNode);
                }
            } else {
                if (structureNode instanceof SequenceNode) {
                    parent.remove(structureNode);
                    structureNode = ((SequenceNode) structureNode).getInnerNode();
                    parent.add(structureNode);
                }
            }
            // apply name and type
            if (innerPrototype != null) {
                innerPrototype.setType(types.get(dataTypeComboBox.getSelectedItem()));
            }
            // FIXME waiting for API change
            structureNode.setName(nameTextField.getText());
        }
        // get the metadata from fields
        MetadataProxy.setMetadata(structureNode, "description", jTextAreaDescription.getText());
        MetadataProxy.setMetadata(structureNode, "units", jTextFieldUnit.getText());
        return structureNode;
    }

    @Override
    public String isInputValid() {
        if (nameTextField.getText().length() <= 0) {
            return "Enter a name";
        } // TODO fix that?
        /*else if (!VerifyName.correct(nameTextField.getText())) {
            return VerifyName.ERROR_MESSAGE;
        } */else {
            return null;
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

        jLabel1 = new javax.swing.JLabel();
        nameTextField = new javax.swing.JTextField();
        typeLabel = new javax.swing.JLabel();
        dataTypeComboBox = new javax.swing.JComboBox();
        sequenceCheckBox = new javax.swing.JCheckBox();
        messageLabel = buildMessageLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextAreaDescription = new javax.swing.JTextArea();
        jTextFieldUnit = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();

        jLabel1.setText(org.openide.util.NbBundle.getMessage(NewStructurePanel.class, "NewStructurePanel.jLabel1.text")); // NOI18N

        typeLabel.setText(org.openide.util.NbBundle.getMessage(NewStructurePanel.class, "NewStructurePanel.typeLabel.text")); // NOI18N

        dataTypeComboBox.setModel(comboBoxModel);

        sequenceCheckBox.setText(org.openide.util.NbBundle.getMessage(NewStructurePanel.class, "NewStructurePanel.sequenceCheckBox.text")); // NOI18N

        messageLabel.setText(org.openide.util.NbBundle.getMessage(NewStructurePanel.class, "NewStructurePanel.messageLabel.text")); // NOI18N

        jTextAreaDescription.setColumns(20);
        jTextAreaDescription.setRows(5);
        jTextAreaDescription.setMinimumSize(new java.awt.Dimension(200, 30));
        jScrollPane1.setViewportView(jTextAreaDescription);

        jTextFieldUnit.setText(org.openide.util.NbBundle.getMessage(NewStructurePanel.class, "NewStructurePanel.jTextFieldUnit.text")); // NOI18N

        jLabel4.setText(org.openide.util.NbBundle.getMessage(NewStructurePanel.class, "NewStructurePanel.jLabel4.text")); // NOI18N

        jLabel5.setText(org.openide.util.NbBundle.getMessage(NewStructurePanel.class, "NewStructurePanel.jLabel5.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(sequenceCheckBox)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 377, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(messageLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 491, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel4)
                            .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, 123, Short.MAX_VALUE)
                            .addComponent(typeLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 123, Short.MAX_VALUE)
                            .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 123, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(nameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 356, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(dataTypeComboBox, 0, 356, Short.MAX_VALUE)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 356, Short.MAX_VALUE)
                            .addComponent(jTextFieldUnit, javax.swing.GroupLayout.DEFAULT_SIZE, 356, Short.MAX_VALUE))))
                .addGap(0, 0, 0))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE, false)
                    .addComponent(jLabel1)
                    .addComponent(nameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(typeLabel)
                    .addComponent(dataTypeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(sequenceCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 79, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel5)
                            .addComponent(jTextFieldUnit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 80, Short.MAX_VALUE)
                        .addGap(37, 37, 37)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(messageLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox dataTypeComboBox;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea jTextAreaDescription;
    private javax.swing.JTextField jTextFieldUnit;
    private javax.swing.JLabel messageLabel;
    private javax.swing.JTextField nameTextField;
    private javax.swing.JCheckBox sequenceCheckBox;
    private javax.swing.JLabel typeLabel;
    // End of variables declaration//GEN-END:variables
}
