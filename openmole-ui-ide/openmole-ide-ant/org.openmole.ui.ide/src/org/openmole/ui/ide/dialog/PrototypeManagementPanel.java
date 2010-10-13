/*
 * Copyright (C) 2010 mathieu leclaire <mathieu.leclaire@openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.ui.ide.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import java.util.HashMap;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.openmole.ui.ide.exception.MoleExceptionManagement;
import org.openmole.ui.ide.workflow.implementation.Preferences;
import org.openmole.ui.ide.workflow.implementation.PrototypeUI;

/**
 *
 * @author mathieu leclaire <mathieu.leclaire@openmole.org>
 */
public class PrototypeManagementPanel extends javax.swing.JPanel implements ListSelectionListener,java.awt.event.ActionListener {

    private static final String updateString = "Update";
    private static final String removeString = "Remove";
    private DefaultListModel prototypeListModel;
    private JTextField nameField;
    private JList list;
    private JButton upButton;
    private JButton removeButton;
    private ButtonGroup typeButtonGroup;
    private HashMap<String, ButtonModel> buttonModelMap = new HashMap();

    public PrototypeManagementPanel() {

        super(new BorderLayout());
        prototypeListModel = new DefaultListModel();

        for (PrototypeUI p : Preferences.getInstance().getPrototypes()) {
            prototypeListModel.addElement(p);
        }

        list = new JList(prototypeListModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedIndex(0);
        list.addListSelectionListener(this);
        list.setCellRenderer(new MyCellRenderer());
        JScrollPane listScrollPane = new JScrollPane(list);

        nameField = new JTextField(10);
        nameField.addActionListener(new AddButtonListener());

        //Create the update prototype button.
        upButton = new JButton(updateString);
        upButton.setActionCommand(updateString);
        upButton.addActionListener(new AddButtonListener());

        //Create the remove prototype button.
        removeButton = new JButton(removeString);
        removeButton.setActionCommand(removeString);
        removeButton.addActionListener(new RemoveButtonListener());

        typeButtonGroup = new ButtonGroup();
        JPanel typePanel = new JPanel();
        typePanel.setLayout(new BoxLayout(typePanel, BoxLayout.Y_AXIS));
       // typePanel.setLayout(new BorderLayout());

        for (Class c : Preferences.getInstance().getPrototypeTypes()) {
            JRadioButton radio = new JRadioButton(c.getSimpleName());
            radio.setActionCommand(c.getCanonicalName());
            typeButtonGroup.add(radio);
            typePanel.add(radio,BorderLayout.WEST);
            radio.setSelected(true);
            buttonModelMap.put(c.getCanonicalName(), radio.getModel());
        }

        JPanel namePane = new JPanel();
        namePane.add(nameField);
        namePane.add(upButton);
        namePane.add(removeButton);

        JPanel controlPane = new JPanel();
      //  controlPane.setLayout(new BoxLayout(controlPane, BoxLayout.Y_AXIS));
    //    controlPane.setLayout(new BorderLayout());
        GridLayout lay = new GridLayout(3,1,0,0); 
        
        controlPane.add(namePane);
        controlPane.add(typePanel);

    //    add(controlPane, BorderLayout.PAGE_START);
    //    add(listScrollPane, BorderLayout.CENTER);
        add(controlPane, BorderLayout.NORTH);
        add(new JSeparator(), BorderLayout.CENTER);
        add(listScrollPane, BorderLayout.SOUTH);
        controlPane.setLayout(lay);
     //   setSize(250, 400);
    }

    /**
     * Actions connected to any changement in the list, essentially selecting a row.
     */
    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (!list.isSelectionEmpty()) {
            PrototypeUI proto = (PrototypeUI) prototypeListModel.get(list.getSelectedIndex());
            nameField.setText(proto.getName());
            typeButtonGroup.setSelected(buttonModelMap.get(proto.getType().getCanonicalName()), true);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    class MyCellRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList list,
                Object value, // value to display
                int index, // cell index
                boolean iss, // is the cell selected
                boolean chf) {
            super.getListCellRendererComponent(list, value, index, iss, chf);
            PrototypeUI proto = (PrototypeUI) value;
            setText(proto.getName() + " - " + proto.getType().getSimpleName());
            return this;
        }
    }

    class AddButtonListener implements ActionListener {

        /**
         * Action linked to the adding of a new prototype
         */
        @Override
        public void actionPerformed(ActionEvent e) {
            PrototypeUI proto = exists(nameField.getText());
            if (proto != null) {
                update(proto);
            } else if (!nameField.getText().equals("")) {
                try {
                    PrototypeUI newproto = new PrototypeUI(nameField.getText(), Class.forName(typeButtonGroup.getSelection().getActionCommand()));
                    prototypeListModel.addElement(newproto);
                    list.setSelectedIndex(prototypeListModel.getSize());
                    Preferences.getInstance().registerPrototype(newproto);
                    nameField.setText("");
                } catch (ClassNotFoundException ex) {
                    MoleExceptionManagement.showException(ex);
                }
            }
        }
    }
    
        /**
         * Action linked to the removing of an existing prototype
         */
    class RemoveButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            PrototypeUI proto = exists(nameField.getText());
            if (proto != null) {
                prototypeListModel.removeElement(proto);
                nameField.setText("");
            }
        }
    }

    /**
     * Updates a prototype, in the imutable way: removing the prototype and
     * creating a new one with the new features.
     * 
     * @param proto, the prototype to be updated
     */
    private void update(PrototypeUI proto) {
        try {
            prototypeListModel.removeElement(proto);
            prototypeListModel.addElement(new PrototypeUI(nameField.getText(), Class.forName(typeButtonGroup.getSelection().getActionCommand())));
            list.setSelectedIndex(prototypeListModel.getSize());
        } catch (ClassNotFoundException ex) {
            MoleExceptionManagement.showException(ex);
        }
    }

    /**
     * Search within a prototype still exists.
     * 
     * @param testedName, the name of the protype to be searched.
     * @return the PrototypeUI wether found and null otherwise
     */
    private PrototypeUI exists(String testedName) {
        Enumeration<?> en = prototypeListModel.elements();
        PrototypeUI proto = null;
        while (en.hasMoreElements()) {
            proto = ((PrototypeUI) en.nextElement());
            if (proto.getName().equals(testedName)) {
                return proto;
            }
        }
        return null;
    }
}
