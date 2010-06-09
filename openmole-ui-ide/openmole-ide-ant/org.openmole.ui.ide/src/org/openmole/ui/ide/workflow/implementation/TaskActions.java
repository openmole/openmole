package org.openmole.ui.ide.workflow.implementation;

import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.widget.Widget;
import org.openmole.ui.ide.workflow.implementation.paint.SelectionManager;
import org.openmole.ui.ide.workflow.model.IObjectViewUI;
import org.openmole.ui.ide.workflow.model.IGenericTaskModelUI;

/**
 *
 * @author mathieu
 */
public class TaskActions extends WidgetAction.Adapter {

    private IGenericTaskModelUI model;
    private IObjectViewUI view;

    public TaskActions(IGenericTaskModelUI m,
            IObjectViewUI v) {
        model = m;
        view = v;
    }

  /*  @Override
    public State mouseMoved(Widget widget, WidgetMouseEvent event) {
        if (event.isControlDown()) {
            System.out.println("MOVED " + widget.toString());
            
            System.out.println("");
        }
        return super.mouseMoved(widget, event);
    }/*

    @Override
    public State mouseReleased(Widget widget, WidgetMouseEvent event) {
    System.out.println("RELEASED " + widget.toString());
    return super.mouseReleased(widget, event);
    }

    @Override
    public State dragEnter(Widget widget, WidgetDropTargetDragEvent event) {
    System.out.println("dragEnter " + widget.toString());
    return super.dragEnter(widget, event);
    }

    @Override
    public State dragExit(Widget widget, WidgetDropTargetEvent event) {
    System.out.println("dragExit " + widget.toString());
    return super.dragExit(widget, event);
    }

    @Override
    public State mouseDragged(Widget widget, WidgetMouseEvent event) {
    System.out.println("mouseDragged " + widget.toString());
    return super.mouseDragged(widget, event);
    }*/


    @Override
    public State mouseClicked(Widget widget,
            WidgetMouseEvent event) {
        SelectionManager.getInstance().setSelected(view);
     /*   try {
            if (event.getClickCount() == 2) {
                ControlPanel.getInstance().switchTableTabbedPane(TableModelMapping.getInstance().getTabbedPane(model));
                //  model.updateData();
                return State.CONSUMED;
            }
        } catch (IllegalArgumentException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IllegalAccessException ex) {
            Exceptions.printStackTrace(ex);
        }*/
        return State.REJECTED;
    }

    /* @Override
    public State mouseMoved(Widget widget,
    WidgetAction.WidgetMouseEvent event) {
    mouseClicked(widget, event);
    return State.REJECTED;
    }*/
}
