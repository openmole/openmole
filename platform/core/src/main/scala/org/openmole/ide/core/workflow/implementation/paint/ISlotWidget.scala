/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.workflow.implementation.paint

import java.awt.Point
import org.netbeans.api.visual.widget.Scene
import org.openmole.ide.core.commons.Constants
import org.openmole.ide.core.workflow.implementation.CapsuleViewUI

class ISlotWidget(scene: Scene,val capsuleView: CapsuleViewUI, val index: Int,val startingSlot: Boolean) extends SlotWidget(scene){
  if (startingSlot) setImage(Constants.IMAGE_START_SLOT)
  else setImage(Constants.IMAGE_INPUT_SLOT)     
  setPreferredLocation(new Point(-12, 14 + index * 20))
}


//extends SlotWidget {
//    private int index;
//    private boolean startingSlot;
//
//    public ISlotWidget(Scene scene,
//            CapsuleViewUI capsuleView,
//            int index,
//            boolean startingSlot) {
//        super(scene,capsuleView);
//        this.index = index;
//        this.startingSlot = startingSlot;
//        setImage(startingSlot ? Constants.IMAGE_START_SLOT : Constants.IMAGE_INPUT_SLOT);
//        setPreferredLocation(new Point(-12, 14 + index * 20));
//    }
//
//    public boolean isStartingSlot() {
//        return startingSlot;
//    }
//
//    public int getIndex() {
//        return index;
//    }