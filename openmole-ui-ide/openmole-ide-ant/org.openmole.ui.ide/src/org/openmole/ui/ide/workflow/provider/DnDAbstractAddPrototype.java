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

package org.openmole.ui.ide.workflow.provider;

import org.openmole.ui.ide.workflow.implementation.MoleScene;
import org.openmole.ui.ide.workflow.implementation.TaskCapsuleViewUI;

/**
 *
 * @author mathieu leclaire <mathieu.leclaire@openmole.org>
 */
public abstract class DnDAbstractAddPrototype extends DnDProvider {
    
    protected boolean encapsulated = false;
    protected TaskCapsuleViewUI view;
    protected MoleScene moleScene;
    
     public DnDAbstractAddPrototype(MoleScene molescene,
                                   TaskCapsuleViewUI view) {
        super(molescene);
        this.moleScene = molescene;
        this.view = view;
    }

    public void setEncapsulated(boolean encapsulated) {
        this.encapsulated = encapsulated;
    }
}
