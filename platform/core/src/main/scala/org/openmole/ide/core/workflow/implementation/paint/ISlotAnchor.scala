/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.workflow.implementation.paint

import java.awt.Point
import org.netbeans.api.visual.anchor.Anchor
import org.netbeans.api.visual.anchor.Anchor.Entry
import org.openmole.ide.core.workflow.implementation.CapsuleViewUI

class ISlotAnchor(relatedWidget: CapsuleViewUI,index: Int) extends SlotAnchor(relatedWidget) {

    val x = -4
    
  override def compute(entry: Entry): Result = new Result(relatedWidget.convertLocalToScene(new Point(x, index * 20 + 22)), Anchor.Direction.LEFT)
    
  
}
//    public ISlotAnchor(CapsuleViewUI relatedWidget,
//            int index) {
//        super(relatedWidget);
//        this.index = index;
//    }
//
//    @Override
//    public Result compute(Entry entry) {
//        return new Result(relatedWidget.convertLocalToScene(new Point(x, index * 20 + 22)), Anchor.Direction.LEFT);
//    }


