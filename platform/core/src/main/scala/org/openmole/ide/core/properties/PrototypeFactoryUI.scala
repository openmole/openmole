/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.properties

import org.openmole.core.implementation.data.Prototype

class PrototypeFactoryUI(cclass: Class[_] ,ipath: String) extends IPrototypeFactoryUI{
  
  override def coreClass= cclass
  
  override def imagePath = ipath 
  
  override def coreObject(name: String) = new Prototype(name,coreClass)
}
