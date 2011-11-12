/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.sampling.complete

import org.openmole.ide.core.model.dataproxy._
import org.openmole.ide.core.implementation.dataproxy.DomainDataProxyFactory
import org.openmole.ide.core.model.data.IDomainDataUI
import org.openmole.ide.core.model.data.ISamplingDataUI
import org.openmole.plugin.sampling.complete.CompleteSampling
import org.openmole.core.implementation.sampling.Factor
import org.openmole.core.model.data.IPrototype
import org.openmole.ide.core.implementation.data.EmptyDataUIs._
import scala.collection.JavaConversions._

class CompleteSamplingDataUI(val name: String, val factors: List[(IPrototypeDataProxyUI,String,IDomainDataUI[_])]) extends ISamplingDataUI {
  def this(n:String) = this(n,List.empty)

  override def coreObject = new CompleteSampling(factors.map(f=>new Factor(
        f._1.dataUI.coreObject.asInstanceOf[IPrototype[Any]],
      //  DomainDataProxyFactory.factoryByName(f._2).buildDataProxyUI(name).dataUI.coreObject)))
      f._3.coreObject)))

  override def coreClass = classOf[CompleteSampling] 
  
  override def imagePath = "img/completeSampling.png" 
  
  override def buildPanelUI = new CompleteSamplingPanelUI(this)
}
