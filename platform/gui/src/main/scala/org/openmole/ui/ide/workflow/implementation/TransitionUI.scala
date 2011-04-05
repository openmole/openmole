/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ui.ide.workflow.implementation

import org.openmole.ui.ide.workflow.implementation.paint.ISlotWidget
import org.openmole.ui.ide.workflow.model.ICapsuleView

class TransitionUI(val source: ICapsuleView,val target: ISlotWidget, var condition: Option[String]= None){}


//    private ICapsuleView source;
//    private ISlotWidget target;
//    private String condition = "";
//
//
//    public TransitionUI(ICapsuleView source, ISlotWidget target) {
//        this.source = source;
//        this.target = target;
//    }
//
//    public TransitionUI(ICapsuleView source, ISlotWidget target,String condition) {
//        this(source,target);
//        this.condition = condition;
//    }
//
//    public ICapsuleView getSource() {
//        return source;
//    }
//
//    public ISlotWidget getTarget() {
//        return target;
//    }
//
//    public String getCondition() {
//        return condition;
//    }
//
//    public void setCondition(String condition) {
//        this.condition = condition;
//    }