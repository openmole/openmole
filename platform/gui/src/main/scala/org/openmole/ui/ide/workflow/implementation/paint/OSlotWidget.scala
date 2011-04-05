/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ui.ide.workflow.implementation.paint

import java.awt.Point
import org.netbeans.api.visual.widget.Scene
import org.openmole.ui.ide.commons.ApplicationCustomize
import org.openmole.ui.ide.workflow.implementation.CapsuleViewUI

class OSlotWidget(scene: Scene,val capsule: CapsuleViewUI) extends SlotWidget(scene, capsule){
  setImage(ApplicationCustomize.IMAGE_OUTPUT_SLOT)
  setDetailedView(ApplicationCustomize.TASK_CONTAINER_WIDTH)
  
  def setDetailedView(w: Int)= setPreferredLocation(new Point(w - 6, 14 + ApplicationCustomize.TASK_TITLE_HEIGHT))
  
}
// OSlotWidget extends SlotWidget {
//    public OSlotWidget(Scene scene,CapsuleViewUI capsule) {
//        super(scene,capsule);
//        setImage(ApplicationCustomize.IMAGE_OUTPUT_SLOT);
//        setDetailedView(ApplicationCustomize.TASK_CONTAINER_WIDTH);
//    }
//
//    public void setDetailedView(int w){
//        setPreferredLocation(new Point(w - 6, 14 + ApplicationCustomize.TASK_TITLE_HEIGHT));
//    }
