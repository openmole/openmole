/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ui.ide.workflow.implementation.paint

import org.netbeans.api.visual.anchor.Anchor
import org.openmole.core.model.capsule.IGenericCapsule
import org.openmole.ui.ide.workflow.implementation.CapsuleViewUI

abstract class SlotAnchor(relatedWidget: CapsuleViewUI[_]) extends Anchor(relatedWidget){}
//
//SlotAnchor  extends Anchor {
//    CapsuleViewUI relatedWidget;
//
//    public SlotAnchor(CapsuleViewUI relatedWidget) {
//        super(relatedWidget);
//        this.relatedWidget = relatedWidget;
//    }