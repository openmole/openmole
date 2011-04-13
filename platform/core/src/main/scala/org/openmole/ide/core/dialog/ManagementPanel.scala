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

package org.openmole.ide.core.dialog
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.JButton
import javax.swing.JList
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JRadioButton
import javax.swing.JTextField
import java.awt.GridLayout
import javax.swing.ListSelectionModel
import javax.swing.event.ListSelectionListener
import javax.swing.event.ListSelectionEvent
import javax.swing.ButtonGroup
import javax.swing.ButtonModel
import javax.swing.DefaultListCellRenderer
import javax.swing.DefaultListModel
import org.openmole.ide.core.exception.MoleExceptionManagement
import org.openmole.ide.core.workflow.model.IEntityUI
import scala.collection.mutable.HashMap


class ManagementPanel(manager: IManager) extends JPanel(new BorderLayout) with ListSelectionListener with ActionListener{
  val listModel= new DefaultListModel
  val buttonModelMap=  new HashMap[String, ButtonModel]
  val updateString = "Update"
  val removeString = "Remove"
    
  manager.container.getAll.foreach(listModel.addElement(_))
  
  val li = new JList(listModel)
  li.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
  li.setSelectedIndex(0)
  li.addListSelectionListener(this)
  li.setCellRenderer(new CellRenderer)
  val listScrollPane = new JScrollPane(li)
  
  val nameField = new JTextField(10)
  nameField.addActionListener(new AddButtonListener)
  if (manager.container.getAll.nonEmpty) {
    nameField.setText(manager.container.getAll.head.name)
  }
  
  //Create the update entity button.
  val upButton = new JButton(updateString)
  upButton.setActionCommand(updateString)
  upButton.addActionListener(new AddButtonListener)

  //Create the remove entity button.
  val removeButton = new JButton(removeString)
  removeButton.setActionCommand(removeString)
  removeButton.addActionListener(new RemoveButtonListener)
  
  val typeButtonGroup= new ButtonGroup
  val typePanel = new JPanel
  typePanel.setLayout(new BoxLayout(typePanel, BoxLayout.Y_AXIS))
  manager.classTypes.foreach(c=>{
      val radio = new JRadioButton(c.getSimpleName)
      radio.setActionCommand(c.getCanonicalName)
      typeButtonGroup.add(radio)
      typePanel.add(radio, BorderLayout.WEST)
      radio.setSelected(true)
      buttonModelMap.put(c.getCanonicalName,radio.getModel)
    })
  
  val namePane = new JPanel
  namePane.add(nameField)
  namePane.add(upButton)
  namePane.add(removeButton)

  val controlPane = new JPanel
  val lay = new GridLayout(3, 1, 0, 0)

  controlPane.add(namePane)
  controlPane.add(typePanel)

  add(controlPane, BorderLayout.NORTH)
  add(listScrollPane, BorderLayout.SOUTH)
  controlPane.setLayout(lay)
        
  def addEntity(entity: IEntityUI)= {
    listModel.addElement(entity)
    li.setSelectedIndex(listModel.getSize)
    manager.container.register(entity)
  }


  override def valueChanged(e: ListSelectionEvent)= {
    if (!li.isSelectionEmpty) {
      listModel.get(li.getSelectedIndex) match {
        case en: IEntityUI=> {
            listModel.get(li.getSelectedIndex)
            nameField.setText(en.name)
            typeButtonGroup.setSelected(buttonModelMap(en.entityType.getCanonicalName), true)}
        case _=> MoleExceptionManagement.giveInformation("Entity not found") 
      }
    }
  }
  
  override def actionPerformed(ae: ActionEvent)= throw new UnsupportedOperationException("Not supported yet.")
  
  class CellRenderer extends DefaultListCellRenderer {
    override def getListCellRendererComponent(li: JList, value: Object, index: Int, iss: Boolean, chf: Boolean)={
      super.getListCellRendererComponent(li, value, index, iss, chf)
      value match{
        case en: IEntityUI=>   setText(en.name + " :: " + en.entityType.getSimpleName)
        case _=> MoleExceptionManagement.giveInformation("List renderer can not be set.")                       
      }
      this
    }
  }
  
  class AddButtonListener extends ActionListener{
    override def actionPerformed(e: ActionEvent)= {
      if (!exists.isDefined && ! nameField.getText.equals("")) addEntity(manager.entityInstance(nameField.getText, Class.forName(typeButtonGroup.getSelection.getActionCommand)))
      nameField.setText("")
      li.clearSelection
    }
  }
 
  class RemoveButtonListener extends ActionListener {
    override def actionPerformed(e: ActionEvent){
      
      val entity= exists
      if(entity.isDefined){
        listModel.removeElement(entity.get)
        manager.container.removeEntity(entity.get)
      } else MoleExceptionManagement.showException("The entity " + entity.get.name + " does not exist.")
      if(listModel.isEmpty){
        nameField.setText("")
      } else {
        li.setSelectedIndex(0)
      }
    }
  }
  
  def exists:Option[IEntityUI]= {
    val en= listModel.elements
    while (en.hasMoreElements){
      var entity= Some(en.nextElement.asInstanceOf[IEntityUI])
      if (entity.get.name.equals(nameField.getText) && entity.get.entityType.equals(Class.forName(typeButtonGroup.getSelection.getActionCommand))) return entity
    }
    None
  }
 
}

///*
// * Copyright (C) 2010 mathieu leclaire <mathieu.leclaire@openmole.org>
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// */
//package org.openmole.ide.core.dialog;
//
//import java.awt.BorderLayout;
//import java.awt.Component;
//import java.awt.GridLayout;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
//import java.util.Enumeration;
//import java.util.HashMap;
//import javax.swing.BoxLayout;
//import javax.swing.ButtonGroup;
//import javax.swing.ButtonModel;
//import javax.swing.DefaultListCellRenderer;
//import javax.swing.DefaultListModel;
//import javax.swing.JButton;
//import javax.swing.JList;
//import javax.swing.JPanel;
//import javax.swing.JRadioButton;
//import javax.swing.JScrollPane;
//import javax.swing.JSeparator;
//import javax.swing.JTextField;
//import javax.swing.ListSelectionModel;
//import javax.swing.event.ListSelectionEvent;
//import javax.swing.event.ListSelectionListener;
//import org.openmole.commons.exception.UserBadDataError;
//import org.openmole.ide.core.exception.MoleExceptionManagement;
//import org.openmole.ide.core.workflow.model.IEntityUI;
//import scala.collection.JavaConversions;
//
///**
// *
// * @author mathieu leclaire <mathieu.leclaire@openmole.org>
// */
//public class ManagementPanel extends javax.swing.JPanel implements ListSelectionListener, java.awt.event.ActionListener {
//
//    private static final String updateString = "Update";
//    private static final String removeString = "Remove";
//    private DefaultListModel listModel;
//    private JTextField nameField;
//    private JList list;
//    private JButton upButton;
//    private JButton removeButton;
//    private ButtonGroup typeButtonGroup;
//    private HashMap<String, ButtonModel> buttonModelMap = new HashMap();
//    IManager manager;
//
//    public ManagementPanel(IManager manager) {
//        super(new BorderLayout());
//        this.manager = manager;
//
//        listModel = new DefaultListModel();
//
//        for (IEntityUI e : manager.getContainer().getAll()) {
//            listModel.addElement(e);
//        }
//
//        list = new JList(listModel);
//        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
//        list.setSelectedIndex(0);
//        list.addListSelectionListener(this);
//        list.setCellRenderer(new CellRenderer());
//        JScrollPane listScrollPane = new JScrollPane(list);
//
//        nameField = new JTextField(10);
//        nameField.addActionListener(new AddButtonListener());
//        if (!manager.getContainer().getAll().isEmpty()) {
//            nameField.setText(manager.getContainer().getAll().iterator().next().getName());
//        }
//
//        //Create the update entity button.
//        upButton = new JButton(updateString);
//        upButton.setActionCommand(updateString);
//        upButton.addActionListener(new AddButtonListener());
//
//        //Create the remove entity button.
//        removeButton = new JButton(removeString);
//        removeButton.setActionCommand(removeString);
//        removeButton.addActionListener(new RemoveButtonListener());
//
//        typeButtonGroup = new ButtonGroup();
//        JPanel typePanel = new JPanel();
//        typePanel.setLayout(new BoxLayout(typePanel, BoxLayout.Y_AXIS));
//
//        for (Class c : JavaConversions.asJavaIterable(manager.getClassTypes())) {
//            JRadioButton radio = new JRadioButton(c.getSimpleName());
//            radio.setActionCommand(c.getCanonicalName());
//            typeButtonGroup.add(radio);
//            typePanel.add(radio, BorderLayout.WEST);
//            radio.setSelected(true);
//            buttonModelMap.put(c.getCanonicalName(), radio.getModel());
//        }
//
//        JPanel namePane = new JPanel();
//        namePane.add(nameField);
//        namePane.add(upButton);
//        namePane.add(removeButton);
//
//        JPanel controlPane = new JPanel();
//        GridLayout lay = new GridLayout(3, 1, 0, 0);
//
//        controlPane.add(namePane);
//        controlPane.add(typePanel);
//
//        add(controlPane, BorderLayout.NORTH);
//        add(listScrollPane, BorderLayout.SOUTH);
//        controlPane.setLayout(lay);
//    }
//
//    /**
//     * Actions connected to any changement in the list, essentially selecting a row.
//     */
//    @Override
//    public void valueChanged(ListSelectionEvent e) {
//        if (!list.isSelectionEmpty()) {
//            IEntityUI en = (IEntityUI) listModel.get(list.getSelectedIndex());
//            nameField.setText(en.getName());
//            typeButtonGroup.setSelected(buttonModelMap.get(en.getType().getCanonicalName()), true);
//        }
//    }
//
//    @Override
//    public void actionPerformed(ActionEvent e) {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }
//
//    class CellRenderer extends DefaultListCellRenderer {
//
//        @Override
//        public Component getListCellRendererComponent(JList list,
//                Object value, // value to display
//                int index, // cell index
//                boolean iss, // is the cell selected
//                boolean chf) {
//            super.getListCellRendererComponent(list, value, index, iss, chf);
//            IEntityUI en = (IEntityUI) value;
//            setText(en.getName() + " :: " + en.getType().getSimpleName());
//            return this;
//        }
//    }
//
//    class AddButtonListener implements ActionListener {
//
//        /**
//         * Action linked to the adding of a new entity
//         */
//        @Override
//        public void actionPerformed(ActionEvent e) {
//            IEntityUI entity = null;
//            try {
//                entity = exists(nameField.getText(), Class.forName(typeButtonGroup.getSelection().getActionCommand()));
//            } catch (ClassNotFoundException ex) {
//                MoleExceptionManagement.giveInformation("The type " + typeButtonGroup.getSelection().getActionCommand() + " does not exist.");
//            }
//            if (entity == null) {
//                try {
//                    IEntityUI newentity = manager.getEntityInstance(nameField.getText(), Class.forName(typeButtonGroup.getSelection().getActionCommand()));
//                    addEntity(newentity);
//                } catch (ClassNotFoundException ex) {
//                    MoleExceptionManagement.showException(ex);
//                }
//            }
//            nameField.setText("");
//            list.clearSelection();
//        }
//    }
//
//    /**
//     * Action linked to the removing of an existing entity
//     */
//    class RemoveButtonListener implements ActionListener {
//
//        @Override
//        public void actionPerformed(ActionEvent e) {
//            IEntityUI entity = null;
//            try {
//                entity = exists(nameField.getText(), Class.forName(typeButtonGroup.getSelection().getActionCommand()));
//            } catch (ClassNotFoundException ex) {
//                MoleExceptionManagement.giveInformation("The type " + typeButtonGroup.getSelection().getActionCommand() + " does not exist.");
//            }
//
//            if (entity != null) {
//                listModel.removeElement(entity);
//                try {
//                    manager.getContainer().removeEntity(entity);
//                } catch (UserBadDataError ex) {
//                    MoleExceptionManagement.showException("The entity " + entity.getName() + " does not exist.");
//                }
//            } else {
//                MoleExceptionManagement.giveInformation("The entity " + nameField.getText() + " does not exist and then has not been removed");
//            }
//            if (listModel.isEmpty()) {
//                nameField.setText("");
//            } else {
//                list.setSelectedIndex(0);
//            }
//
//        }
//    }
//
//    private void addEntity(IEntityUI entity) {
//        listModel.addElement(entity);
//        list.setSelectedIndex(listModel.getSize());
//        manager.geaddEntitytContainer().register(entity);
//    }
//
//    /**
//     * Search within a entity still exists.
//     * 
//     * @param testedName, the name of the protype to be searched.
//     * @return the IEntityUI wether found and null otherwise
//     */
//    private IEntityUI exists(String testedName, Class type) {
//        Enumeration<?> en = listModel.elements();
//        IEntityUI entity = null;
//        while (en.hasMoreElements()) {
//            entity = ((IEntityUI) en.nextElement());
//            if (entity.getName().equals(testedName) && entity.getType().equals(type)) {
//                return entity;
//            }
//        }
//        return null;
//    }
//}
