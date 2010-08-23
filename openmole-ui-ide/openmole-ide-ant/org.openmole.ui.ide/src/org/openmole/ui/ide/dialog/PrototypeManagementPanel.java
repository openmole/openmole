/*
 * Copyright (C) 2010 mathieu leclaire <mathieu.leclaire@openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigInteger;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.DefaultButtonModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.basic.BasicBorders.RadioButtonBorder;
import org.openide.util.Exceptions;
import org.openmole.ui.ide.exception.MoleExceptionManagement;
import org.openmole.ui.ide.workflow.implementation.Preferences;
import org.openmole.ui.ide.workflow.implementation.PrototypeUI;

/**
 *
 * @author mathieu leclaire <mathieu.leclaire@openmole.org>
 */
public class PrototypeManagementPanel extends JFrame implements ListSelectionListener {

    private static final String addString = "Update";
    private DefaultListModel prototypeListModel;
    private JTextField nameField;
    private JList list;
    private JButton addButton;
    private ButtonGroup typeButtonGroup;
    private HashMap<String,ButtonModel> buttonModelMap  = new HashMap();

    public PrototypeManagementPanel() {

        super("Prototype management");
        prototypeListModel = new DefaultListModel();

        for (PrototypeUI p : Preferences.getInstance().getPrototypes()) {
            //  prototypeListModel.addElement(p.getName() +", "+p.getType().getSimpleName());
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

        //Create the new prototype button.
        addButton = new JButton(addString);
        addButton.setActionCommand(addString);
        addButton.addActionListener(new AddButtonListener());

        typeButtonGroup = new ButtonGroup();
        JPanel typePanel = new JPanel();
        typePanel.setLayout(new BoxLayout(typePanel, BoxLayout.Y_AXIS));



        for (Class c : Preferences.getInstance().getPrototypeTypes()) {
            JRadioButton radio = new JRadioButton(c.getSimpleName());
            radio.setActionCommand(c.getCanonicalName());
            //  radio.addActionListener(new RadioButtonListener());
            typeButtonGroup.add(radio);
            typePanel.add(radio);
            radio.setSelected(true);
            buttonModelMap.put(c.getCanonicalName(), radio.getModel());
        }

        JPanel namePane = new JPanel();
        namePane.add(nameField);
        namePane.add(addButton);


        JPanel controlPane = new JPanel();
        controlPane.setLayout(new BoxLayout(controlPane, BoxLayout.Y_AXIS));
        controlPane.add(namePane);
        controlPane.add(typePanel);

        add(controlPane, BorderLayout.PAGE_START);
        add(listScrollPane, BorderLayout.CENTER);
        setSize(400, 400);
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (!list.isSelectionEmpty()) {
            PrototypeUI proto = (PrototypeUI) prototypeListModel.get(list.getSelectedIndex());
            nameField.setText(proto.getName());
            typeButtonGroup.setSelected(buttonModelMap.get(proto.getType().getCanonicalName()), true);

        }
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

        @Override
        public void actionPerformed(ActionEvent e) {
            PrototypeUI proto = exists(nameField.getText());
            if (proto != null) {
                update(proto);
            } else if (!nameField.getText().equals("")) {
                try {
                    prototypeListModel.addElement(new PrototypeUI(nameField.getText(), Class.forName(typeButtonGroup.getSelection().getActionCommand())));              
                    list.setSelectedIndex(prototypeListModel.getSize());
                    nameField.setText("");
                } catch (ClassNotFoundException ex) {
                    MoleExceptionManagement.showException(ex);
                }
            }
        }
    }

    private void update(PrototypeUI proto) {
        try {
            prototypeListModel.removeElement(proto);
            prototypeListModel.addElement(new PrototypeUI(nameField.getText(), Class.forName(typeButtonGroup.getSelection().getActionCommand())));
            list.setSelectedIndex(prototypeListModel.getSize());
        } catch (ClassNotFoundException ex) {
            MoleExceptionManagement.showException(ex);
        }
    }

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
