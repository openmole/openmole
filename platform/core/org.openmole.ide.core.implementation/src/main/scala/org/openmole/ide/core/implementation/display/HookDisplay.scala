/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.implementation.display

import org.openide.util.Lookup
import org.openmole.ide.core.implementation.dataproxy.HookDataProxyFactory
import org.openmole.ide.misc.exception.GUIUserBadDataError
import org.openmole.ide.core.implementation.dataproxy.Proxys
import org.openmole.ide.core.model.dataproxy.IHookDataProxyUI
import org.openmole.ide.core.model.factory.IHookFactoryUI
import org.openmole.ide.core.model.panel.IHookPanelUI
import org.openmole.ide.core.model.display.IDisplay
import scala.collection.mutable.HashSet
import scala.collection.JavaConversions._

class HookDisplay extends IDisplay{
  private var modelHooks = new HashSet[HookDataProxyFactory]
  var currentPanel: Option[IHookPanelUI] = None
  var name= "hook0"
  var dataProxy: Option[IHookDataProxyUI] = None
  
  Lookup.getDefault.lookupAll(classOf[IHookFactoryUI]).foreach(f=>{modelHooks += new HookDataProxyFactory(f)})
  
  override def implementationClasses = modelHooks
  
  override def dataProxyUI(n: String):IHookDataProxyUI = Proxys.getHookDataProxyUI(n).getOrElse(dataProxy.get)

  override def increment = name = "proto" + Displays.nextInt
  
  def  buildPanelUI(n:String) = {
    currentPanel = Some(dataProxyUI(n).dataUI.buildPanelUI)
    currentPanel.get
  }
  
  def saveContent(oldName: String) = {
    dataProxy = Some(dataProxyUI(oldName))
    dataProxyUI(oldName).dataUI = currentPanel.getOrElse(throw new GUIUserBadDataError("No panel to print for entity " + oldName)).saveContent(name)
    Proxys.addHookElement(dataProxyUI(oldName))  
  }
}
