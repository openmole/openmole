/*
 *  Copyright (C) 2010 leclaire
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
package org.openmole.ui.workflow.implementation;

import java.awt.Color;
import java.awt.Image;
import org.openmole.core.workflow.model.capsule.IGenericTaskCapsule;
import org.openmole.ui.workflow.implementation.paint.MyConnectableWidget;
import org.openmole.ui.workflow.implementation.paint.MyWidget;
import org.openmole.ui.workflow.model.IConnectable;
import org.openmole.ui.workflow.model.ICapsuleModelUI;
import org.openmole.ui.workflow.model.IObjectModelUI;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class Connectable extends ObjectViewUI implements IConnectable {

    private String name;
    protected MyConnectableWidget connectableWidget;
    protected ICapsuleModelUI capsuleModel;

    public Connectable(MoleScene scene,
                       ICapsuleModelUI tcm,
                       Color defaultBackgroundColor,
                       Color defaultBorderColor,
                       String name) {
        super(scene,
                defaultBackgroundColor,
                defaultBorderColor);
        capsuleModel = tcm;
        this.name = name;
        scene.getManager().registerTaskCapsuleModel(name, tcm);
    }

    public Connectable(MoleScene scene,
                       ICapsuleModelUI tcm,
                       Color defaultBackgroundColor,
                       Color defaultBorderColor,
                       Image img,
                       String name) {
        this(scene, tcm,defaultBackgroundColor, defaultBorderColor, name);
        this.backgroundImage = img;
    }

    @Override
    public void setTaskCapsule(IGenericTaskCapsule tc) {
        capsuleModel.setTaskCapsule(tc);
    }

    @Override
    public void addInputSlot() {
        capsuleModel.addInputSlot();
        connectableWidget.addInputSlot();
    }

    @Override
    public void addOutputSlot() {
        capsuleModel.addOutputSlot();
        connectableWidget.addOutputSlot();
    }

    @Override
    public MyConnectableWidget getConnectableWidget() {
        return connectableWidget;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ICapsuleModelUI getTaskCapsuleModel() {
        return capsuleModel;
    }

    @Override
    public IObjectModelUI getModel() {
        return (IObjectModelUI) capsuleModel;
    }

    @Override
    public MyWidget getWidget() {
        return connectableWidget;
    }
}
