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
package org.openmole.ui.ide.workflow.action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JComponent;
import javax.swing.JFrame;
import org.openmole.ui.ide.dialog.PrototypeManagementPanel;

/**
 *
 * @author mathieu leclaire <mathieu.leclaire@openmole.org>
 */
public class ManagePrototypeAction implements ActionListener {
    private PrototypeManagementPanel prototypePanel;
    
    public ManagePrototypeAction(PrototypeManagementPanel protoPanel) {
        prototypePanel = protoPanel;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {

        System.out.println("++ actionPerformed ++ " + prototypePanel.isVisible() + " !!");
        
//        JFrame frame = new JFrame("Prototype management");
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//
//        //Create and set up the content pane.
//        JComponent newContentPane = new PrototypeManagementPanel(frame);
//        newContentPane.setOpaque(true); //content panes must be opaque
//        frame.setContentPane(newContentPane);

        //Display the window.
       // frame.pack();
        prototypePanel.setVisible(!prototypePanel.isVisible());



//        
//        
//        String inputValue = JOptionPane.showInputDialog("Create a new "+ ((Class) t.getTransferData(ApplicationCustomize.PROTOTYPE_DATA_FLAVOR)).getSimpleName()+" prototype");
//    }
//
//            MoleSceneManager manager = moleScene.getManager();
//            if (inputValue != null){
//                Preferences.getInstance().registerPrototype(new PrototypeUI(inputValue,
//                                                                            (Class) t.getTransferData(ApplicationCustomize.PROTOTYPE_DATA_FLAVOR)));
//             
//        throw new UnsupportedOperationException("Not supported yet.");
//    }

    }
}
