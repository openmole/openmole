/*
 *  Copyright (C) 2010 Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ui.ide.workflow.provider;

import java.awt.Point;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import javax.swing.JOptionPane;
import org.netbeans.api.visual.action.ConnectorState;
import org.netbeans.api.visual.widget.Widget;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.ui.ide.commons.ApplicationCustomize;
import org.openmole.ui.ide.commons.IOType;
import org.openmole.ui.ide.exception.MoleExceptionManagement;
import org.openmole.ui.ide.workflow.implementation.MoleScene;
import org.openmole.ui.ide.workflow.implementation.MoleSceneManager;
import org.openmole.ui.ide.workflow.implementation.Preferences;
import org.openmole.ui.ide.workflow.implementation.PrototypeUI;
import org.openmole.ui.ide.workflow.implementation.TaskCapsuleViewUI;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class DnDAddPrototypeProvider extends DnDProvider{
    private boolean encapsulated = false;
    private TaskCapsuleViewUI view;
    private MoleScene moleScene;

    private boolean dialogOK = false;

    public DnDAddPrototypeProvider(MoleScene molescene,
                                   TaskCapsuleViewUI view) {
        super(molescene);
        this.moleScene = molescene;
        this.view = view;
    }

    public void setEncapsulated(boolean encapsulated) {
        this.encapsulated = encapsulated;
    }

    @Override
    public ConnectorState isAcceptable(Widget widget, Point point, Transferable transferable) {
        ConnectorState state = ConnectorState.REJECT;
        if (transferable.isDataFlavorSupported(ApplicationCustomize.PROTOTYPE_DATA_FLAVOR)&&
            encapsulated == true)
            state = ConnectorState.ACCEPT;
        return state;
    }

    @Override
    public void accept(Widget widget, Point point, Transferable t) {
        try {


            String inputValue = JOptionPane.showInputDialog("Create a new "+((Class) t.getTransferData(ApplicationCustomize.PROTOTYPE_DATA_FLAVOR)).getSimpleName()+" prototype");

            MoleSceneManager manager = moleScene.getManager();
            if (inputValue != null){
                Preferences.getInstance().registerPrototype(new PrototypeUI(inputValue,
                                                                            (Class) t.getTransferData(ApplicationCustomize.PROTOTYPE_DATA_FLAVOR)));

            //CategoryBuilder.getInstance().getPrototypeInstanceCategory(CategoryName.PROTOTYPE_INSTANCE).getChildren().refreshNodes();
            //MoleSceneTopComponentTopComponent.getDefault().refreshPalette();

            if (point.x < ApplicationCustomize.TASK_CONTAINER_WIDTH/2) view.getTaskModel().addPrototype(Preferences.getInstance().getPrototype(inputValue), IOType.INPUT);
            else view.getTaskModel().addPrototype(Preferences.getInstance().getPrototype(inputValue), IOType.OUTPUT);
            }
        } catch (UserBadDataError ex) {
            MoleExceptionManagement.showException(ex);
        } catch (UnsupportedFlavorException ex) {
            MoleExceptionManagement.showException(ex);
        } catch (IOException ex) {
            MoleExceptionManagement.showException(ex);
        }
    }

}
