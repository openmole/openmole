/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.sampling.complete


import java.io.File
import org.openmole.ide.misc.exception.GUIUserBadDataError
import org.openmole.ide.core.implementation.dataproxy.PrototypeDataProxyUI
import org.openmole.ide.core.model.data.ISamplingDataUI
import org.openmole.plugin.sampling.complete.CompleteSampling
import org.openmole.core.model.domain.IDomain
import org.openmole.core.model.sampling.IFactor
import org.openmole.ide.core.implementation.data.EmptyDataUIs._
import scala.collection.mutable.ListBuffer

class CompleteSamplingDataUI(val name: String, factors: Iterable[(IFactor[T,IDomain[T]]) forSome {type T}]) extends ISamplingDataUI {
  def this(n: String) = this(n,new ListBuffer[(IFactor[T,IDomain[T]]) forSome {type T}])
  
  println("CompleteSamplingDataUI:: " +  factors.size)
  override def coreObject = new CompleteSampling(factors)

  override def coreClass = classOf[CompleteSampling] 
  
  override def imagePath = "img/completeSampling.png" 
  
  override def buildPanelUI = new CompleteSamplingPanelUI(this)
}
