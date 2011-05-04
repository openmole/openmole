/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.workflow.implementation.paint

import java.awt.Point
import org.netbeans.api.visual.anchor.Anchor
import org.openmole.ide.core.commons.Constants
import org.openmole.ide.core.control.MoleScenesManager
import org.openmole.ide.core.workflow.implementation.CapsuleViewUI

class OSlotAnchor(relatedWidget: CapsuleViewUI) extends SlotAnchor(relatedWidget) {

  val x= Constants.TASK_CONTAINER_WIDTH + 10
  val y= Constants.TASK_TITLE_HEIGHT + 22
  
  override def compute(entry: Anchor.Entry)= {
    var detailedEffect= 0
    if (MoleScenesManager.detailedView)
      detailedEffect= Constants.EXPANDED_TASK_CONTAINER_WIDTH -Constants.TASK_CONTAINER_WIDTH
    new Result(relatedWidget.convertLocalToScene(new Point(x + detailedEffect, y)), Anchor.Direction.RIGHT)
  }
}

//
// extends SlotAnchor {
//
//    private final int x = Constants.TASK_CONTAINER_WIDTH + 22;
//    protected final int y = Constants.TASK_TITLE_HEIGHT + 22;
//
//    public OSlotAnchor(CapsuleViewUI relatedWidget) {
//        super(relatedWidget);
//    }
//
//    @Override
//    public Result compute(Entry entry) {
//        int detailedEffect = (MoleScenesManager.getInstance().isDetailedView() ? Constants.EXPANDED_TASK_CONTAINER_WIDTH -Constants.TASK_CONTAINER_WIDTH : 0);
//        return new Result(relatedWidget.convertLocalToScene(new Point(x + detailedEffect, y)), Anchor.Direction.RIGHT);
//    }