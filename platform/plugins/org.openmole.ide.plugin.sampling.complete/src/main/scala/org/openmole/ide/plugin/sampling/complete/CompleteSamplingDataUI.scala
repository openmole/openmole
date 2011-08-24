/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.sampling.complete

import org.openmole.ide.core.model.dataproxy.IDomainDataProxyUI
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.model.data.ISamplingDataUI
import org.openmole.plugin.sampling.complete.CompleteSampling
import org.openmole.core.implementation.sampling.Factor
import org.openmole.core.model.data.IPrototype
import org.openmole.ide.core.implementation.data.EmptyDataUIs._

  class CompleteSamplingDataUI(val name: String, val factors: List[(IPrototypeDataProxyUI,IDomainDataProxyUI)]) extends ISamplingDataUI {
    def this(n:String) = this(n,List.empty)

  override def coreObject = new CompleteSampling(factors.map(f=>new Factor(f._1.dataUI.coreObject.asInstanceOf[IPrototype[Any]],
                                                                           f._2.dataUI.coreObject)))

  override def coreClass = classOf[CompleteSampling] 
  
  override def imagePath = "img/completeSampling.png" 
  
  override def buildPanelUI = new CompleteSamplingPanelUI(this)
}
