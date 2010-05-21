package org.openmole.ui.workflow.implementation;

import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.widget.Widget;
import org.openide.util.Exceptions;
import org.openmole.ui.control.ControlPanel;
import org.openmole.ui.control.TableModelMapping;
import org.openmole.ui.workflow.implementation.paint.SelectionManager;
import org.openmole.ui.workflow.model.IObjectViewUI;
import org.openmole.ui.workflow.model.ITaskModelUI;

/**
 *
 * @author mathieu
 */
public class TaskActions extends WidgetAction.Adapter {

    private ITaskModelUI model;
    private IObjectViewUI view;

    public TaskActions(ITaskModelUI m,
            IObjectViewUI v) {
        model = m;
        view = v;
    }

    @Override
    public State dragOver(Widget widget, WidgetDropTargetDragEvent event) {
        System.out.println("OVER " + widget.toString());
        return super.dragOver(widget, event);
    }

    @Override
    public State mouseMoved(Widget widget, WidgetMouseEvent event) {
        return super.mouseMoved(widget, event);
    }

    @Override
    public State mouseClicked(Widget widget,
            WidgetMouseEvent event) {
        SelectionManager.getInstance().setSelected(view);
        try {
            if (event.getClickCount() == 2) {
                ControlPanel.getInstance().switchTableTabbedPane(TableModelMapping.getInstance().getTabbedPane(model));
                model.updateData();
                return State.CONSUMED;
            }
        } catch (IllegalArgumentException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IllegalAccessException ex) {
            Exceptions.printStackTrace(ex);
        }
        return State.REJECTED;
    }

    /* @Override
    public State mouseMoved(Widget widget,
    WidgetAction.WidgetMouseEvent event) {
    mouseClicked(widget, event);
    return State.REJECTED;
    }*/
}
