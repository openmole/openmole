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

package org.openmole.ui.ide.workflow.action

import org.netbeans.api.visual.action.WidgetAction
import org.openmole.ui.ide.workflow.model.ICapsuleView
import org.openmole.ui.ide.workflow.model.IGenericTaskModelUI

class TaskActions(model: IGenericTaskModelUI[_], view: ICapsuleView) extends WidgetAction.Adapter {
}
//
//public class TaskActions extends WidgetAction.Adapter {
//
//    private IGenericTaskModelUI model;
//    private ICapsuleView view;
//
//    public TaskActions(IGenericTaskModelUI m,
//                       ICapsuleView v) {
//        model = m;
//        view = v;
//    }