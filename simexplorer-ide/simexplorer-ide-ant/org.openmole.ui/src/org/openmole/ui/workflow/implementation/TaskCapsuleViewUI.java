/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openmole.ui.workflow.implementation;

import org.openmole.ui.workflow.model.IObjectModelUI;
import org.openmole.ui.workflow.provider.TaskCapsuleMenuProvider;
import org.netbeans.api.visual.action.ActionFactory;
import org.netbeans.api.visual.action.EditProvider;
import org.netbeans.api.visual.layout.LayoutFactory;
import org.netbeans.api.visual.widget.Widget;
import org.openmole.ui.workflow.implementation.paint.MyConnectableWidget;
import org.openmole.ui.workflow.model.ICapsuleModelUI;
import org.openmole.ui.workflow.model.ITaskCapsuleViewUI;
import org.openmole.ui.workflow.provider.TaskCapsuleHoverProvider;

/**
 *
 * @author mathieu
 */
public class TaskCapsuleViewUI extends Connectable implements ITaskCapsuleViewUI{

    public TaskCapsuleViewUI(MoleScene sc,
                             ICapsuleModelUI tcm,
                             String name) {

        super(sc,
             tcm,
             Preferences.getInstance().getCapsuleModelSettings().getDefaultBackgroundColor(),
             Preferences.getInstance().getCapsuleModelSettings().getDefaultBorderColor(),
             name);

        connectableWidget = new MyConnectableWidget(scene,
                                                    getBackgroundColor(),
                                                    getBorderColor());

        setLayout(LayoutFactory.createVerticalFlowLayout());
        addChild(connectableWidget);
        getActions().addAction(ActionFactory.createPopupMenuAction(new TaskCapsuleMenuProvider(this)));
        getActions().addAction(ActionFactory.createHoverAction(new TaskCapsuleHoverProvider()));
      //  .getActions ().addAction (createObjectHoverAction ());
    }
    
    private class MyEditProvider implements EditProvider {

        String content;

        MyEditProvider(String cont) {
            content = cont;
        }

        @Override
        public void edit(Widget widget) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}
