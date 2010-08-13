/*
 *  Copyright (C) 2010 leclaire
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the Affero GNU General Public License as published by
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
package org.openmole.ui.ide.workflow.implementation;

import org.openmole.ui.ide.commons.IOType;

import java.util.HashMap;
import java.util.Map;
import org.openmole.ui.ide.workflow.model.ICapsuleModelUI;
import org.openmole.core.model.capsule.IGenericTaskCapsule;
import org.openmole.ui.ide.commons.ApplicationCustomize;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
//public abstract class CapsuleModelUI<T extends IGenericTaskCapsule> extends ObjectModelUI implements ICapsuleModelUI<T>{
public abstract class CapsuleModelUI<T extends IGenericTaskCapsule> extends ObjectModelUI implements ICapsuleModelUI{

    private transient Map<IOType, Integer> nbSlots;
    private IGenericTaskCapsule taskCapsule;
    private final static String category = "Task Tapsules";

    private void setNbSlots() {
        if (nbSlots == null){
            nbSlots = new HashMap();
            nbSlots.put(IOType.INPUT, 0);
            nbSlots.put(IOType.OUTPUT, 0);
        }
    }

    @Override
    public String getCategory() {
        return category;
    }

    @Override
    public int getNbInputslots() {
        setNbSlots();
        return nbSlots.get(IOType.INPUT);
    }

    @Override
    public int getNbOutputslots() {
        setNbSlots();
        return nbSlots.get(IOType.OUTPUT);
    }

    @Override
    public void addOutputSlot() {
        setNbSlots();
        nbSlots.put(IOType.OUTPUT, 1 + nbSlots.get(IOType.OUTPUT));
    }

    @Override
    public void addInputSlot() {
        setNbSlots();
        nbSlots.put(IOType.INPUT, 1 + nbSlots.get(IOType.INPUT));
    }

    @Override
    public boolean isSlotRemovable(IOType type) {
        return (nbSlots.get(type) > 1 ? true : false);
    }

    @Override
    public boolean isSlotAddable(IOType type) {
        return (nbSlots.get(type) < ApplicationCustomize.NB_MAX_SLOTS ? true : false);
    }

    @Override
    public void removeSlot(IOType type) {
        int nb = nbSlots.get(type) - 1;
        nbSlots.put(type, nb);
    }
}
