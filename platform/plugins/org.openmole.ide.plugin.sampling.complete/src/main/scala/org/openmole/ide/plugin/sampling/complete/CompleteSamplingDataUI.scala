/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.sampling.complete

import org.openmole.ide.core.model.dataproxy._
import org.openmole.ide.core.model.data.IDomainDataUI
import org.openmole.ide.core.model.data.ISamplingDataUI
import org.openmole.plugin.sampling.combine.CompleteSampling
import org.openmole.core.implementation.sampling.Factor
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.domain.IDomain
import org.openmole.ide.core.implementation.data.EmptyDataUIs._
import scala.collection.JavaConversions._

class CompleteSamplingDataUI(val name: String, val factors: List[(IPrototypeDataProxyUI,String,IDomainDataUI)]) extends ISamplingDataUI {
  def this(n:String) = this(n,List.empty)

  def coreObject = new CompleteSampling(factors.map(f=>new Factor(
        f._1.dataUI.coreObject.asInstanceOf[IPrototype[Any]],
      //  DomainDataProxyFactory.factoryByName(f._2).buildDataProxyUI(name).dataUI.coreObject)))
      f._3.coreObject(f._1.dataUI.coreObject).asInstanceOf[IDomain[Any]])))

  def coreClass = classOf[CompleteSampling] 
  
  def imagePath = "img/completeSampling.png" 
  
  def buildPanelUI = new CompleteSamplingPanelUI(this)
}
