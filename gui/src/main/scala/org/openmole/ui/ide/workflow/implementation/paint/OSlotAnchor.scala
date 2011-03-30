/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ui.ide.workflow.implementation.paint

import java.awt.Point
import org.netbeans.api.visual.anchor.Anchor
import org.openmole.ui.ide.commons.ApplicationCustomize
import org.openmole.ui.ide.control.MoleScenesManager
import org.openmole.ui.ide.workflow.implementation.CapsuleViewUI
import org.openmole.core.model.capsule.IGenericCapsule

class OSlotAnchor(relatedWidget: CapsuleViewUI[_]) extends SlotAnchor(relatedWidget) {

  val x= ApplicationCustomize.TASK_CONTAINER_WIDTH + 22
  val y= ApplicationCustomize.TASK_TITLE_HEIGHT + 22
  
  override def compute(entry: Anchor.Entry)= {
    var detailedEffect= 0
    if (MoleScenesManager.detailedView)
      detailedEffect= ApplicationCustomize.EXPANDED_TASK_CONTAINER_WIDTH -ApplicationCustomize.TASK_CONTAINER_WIDTH
    new Result(relatedWidget.convertLocalToScene(new Point(x + detailedEffect, y)), Anchor.Direction.RIGHT)
  }
}

//
// extends SlotAnchor {
//
//    private final int x = ApplicationCustomize.TASK_CONTAINER_WIDTH + 22;
//    protected final int y = ApplicationCustomize.TASK_TITLE_HEIGHT + 22;
//
//    public OSlotAnchor(CapsuleViewUI relatedWidget) {
//        super(relatedWidget);
//    }
//
//    @Override
//    public Result compute(Entry entry) {
//        int detailedEffect = (MoleScenesManager.getInstance().isDetailedView() ? ApplicationCustomize.EXPANDED_TASK_CONTAINER_WIDTH -ApplicationCustomize.TASK_CONTAINER_WIDTH : 0);
//        return new Result(relatedWidget.convertLocalToScene(new Point(x + detailedEffect, y)), Anchor.Direction.RIGHT);
//    }