/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.workflow.implementation

import org.openmole.ide.core.control.MoleScenesManager
import org.openmole.ide.core.properties.IPrototypeFactoryUI
import org.openmole.ide.core.workflow.model.IEntityUI

class PrototypeUI(var name: String,val factory: IPrototypeFactoryUI) extends IEntityUI{
  def this(factory: IPrototypeFactoryUI) = this(MoleScenesManager.incrementPrototypeName,factory)
}

//
//public class PrototyeUI extends EntityUI{
//
//    public PrototyeUI() {
//    super();
//    }
//    
//    public PrototyeUI(String name, Class type) {
//        super(name,type);
//    }
//}