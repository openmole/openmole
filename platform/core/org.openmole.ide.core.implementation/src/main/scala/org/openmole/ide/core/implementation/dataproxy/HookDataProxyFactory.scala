/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.implementation.dataproxy

import org.openmole.ide.core.model.dataproxy.IHookDataProxyFactory
import org.openmole.ide.core.model.factory.IHookFactoryUI
import scala.collection.JavaConversions._

class HookDataProxyFactory(val factory: IHookFactoryUI) extends IHookDataProxyFactory{
  
  override def buildDataProxyUI(name:String) = new HookDataProxyUI(factory.buildDataUI(name))
}
