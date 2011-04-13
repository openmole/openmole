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

package org.openmole.ide.core.workflow.action

import org.netbeans.api.visual.action.WidgetAction
import org.netbeans.api.visual.widget.Widget
import org.openmole.ide.core.workflow.implementation.TransitionUI
import org.openmole.ide.core.workflow.implementation.paint.LabeledConnectionWidget
import org.openmole.ide.core.dialog.TransitionDialog

class TransitionActions(transition: TransitionUI,connectionWidget: LabeledConnectionWidget) extends WidgetAction.Adapter{

  override def mouseClicked(widget: Widget, event: WidgetAction.WidgetMouseEvent)= {
    if (event.getClickCount == 2) TransitionDialog.displayTransitionDialog(transition, connectionWidget)
    WidgetAction.State.REJECTED       
  }
}
//import org.netbeans.api.visual.action.WidgetAction;
//import org.netbeans.api.visual.widget.Widget;
//import org.openmole.ide.core.dialog.TransitionDialog;
//import org.openmole.ide.core.workflow.implementation.TransitionUI;
//import org.openmole.ide.core.workflow.implementation.paint.LabeledConnectionWidget;
//
///**
// *
// * @author Mathieu Leclaire <mathieu.leclaire@openmole.org>
// */
//public class TransitionActions extends WidgetAction.Adapter {
//
//    TransitionUI transition;
//    LabeledConnectionWidget connectionWidget;
//
//    public TransitionActions(TransitionUI transition, LabeledConnectionWidget connectionWidget) {
//        System.out.println("TransitionActions constructor   ");
//        this.transition = transition;
//        this.connectionWidget = connectionWidget;
//    }
//
//    @Override
//    public State mouseClicked(Widget widget,
//            WidgetMouseEvent event) {
//        System.out.println(" - mouseClicked - " + transition);
//        if (event.getClickCount() == 2) {
//            TransitionDialog.displayTransitionDialog(transition, connectionWidget);
//        }
//        return State.REJECTED;
//    }
//}