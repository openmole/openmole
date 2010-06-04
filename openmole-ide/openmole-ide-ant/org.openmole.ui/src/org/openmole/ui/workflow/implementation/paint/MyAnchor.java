package org.openmole.ui.workflow.implementation.paint;

import org.openmole.ui.commons.IOType;
import org.netbeans.api.visual.anchor.Anchor;

/**
 *
 * @author mathieu
 */
public class MyAnchor extends Anchor {

    int index;
    IOType slotType;
    MyConnectableWidget relatedWidget;

    public MyAnchor(MyConnectableWidget relatedWidget,
                    IOType slotType,
                    int index) {
        super(relatedWidget);
        this.index = index;
        this.slotType = slotType;
        this.relatedWidget = relatedWidget;
    }

    @Override
    public Result compute(Entry entry) {
        return new Result(relatedWidget.convertLocalToScene((slotType == IOType.INPUT) ? relatedWidget.getInputSlotPoint(index):relatedWidget.getOutputSlotPoint(index)),
                          (slotType == IOType.INPUT) ? Anchor.Direction.LEFT : Anchor.Direction.RIGHT);
    }
}
