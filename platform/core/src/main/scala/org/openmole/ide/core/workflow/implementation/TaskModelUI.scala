/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.workflow.implementation

import org.openmole.ide.core.commons.IOType
import scala.collection.mutable.HashSet

class TaskModelUI {
  var prototypesIn= HashSet.empty[PrototypeUI]
  var prototypesOut= HashSet.empty[PrototypeUI]
  
  def addPrototype(p: PrototypeUI, ioType: IOType.Value)= {
    if (ioType.equals(IOType.INPUT)) addPrototypeIn(p)
    else addPrototypeOut(p)
  }

  private def addPrototypeIn(p: PrototypeUI)= prototypesIn+= p
  
  private def addPrototypeOut(p: PrototypeUI)= prototypesOut+= p
}
