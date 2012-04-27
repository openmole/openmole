/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.method.sensitivity

import org.openmole.core.implementation.sampling.Factor
import org.openmole.core.model.data.IPrototype
import org.openmole.ide.core.implementation.data.EmptyDataUIs
import org.openmole.ide.core.model.data.IBoundedDomainDataUI
import org.openmole.ide.core.model.data.ISamplingDataUI
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.misc.exception.UserBadDataError
import org.openmole.plugin.method.sensitivity.SaltelliSampling

class SaltelliSamplingDataUI(val name : String="", 
                             val samples : String  = "",
                             val matrixName : IPrototypeDataProxyUI = EmptyDataUIs.emptyPrototypeProxy,
                             val factors : List[(IPrototypeDataProxyUI,String,IBoundedDomainDataUI)] = List.empty) extends ISamplingDataUI {
                     
  implicit def string2Int(s: String): Int = augmentString(s).toInt

  def coreObject = 
    new SaltelliSampling(
      try samples 
      catch { 
        case e : NumberFormatException => throw new UserBadDataError("An integer is exepected as number of samples") 
      }, 
      if (matrixName.dataUI.coreObject.`type`.erasure == classOf[String]) matrixName.dataUI.coreObject.asInstanceOf[IPrototype[String]]
      else throw new UserBadDataError("The matrix name prototype has to be a string"),
      factors.map{
        f=>
        val proto = f._1.dataUI.coreObject.asInstanceOf[IPrototype[Double]]
        new Factor(proto, f._3.coreObject(proto))
      }: _*)


  def coreClass = classOf[SaltelliSampling] 
  
  def imagePath = "img/saletelliSampling.png" 
  
  override def fatImagePath = "img/saltelliSampling_fat.png" 
  
  def buildPanelUI = new SaltelliSamplingPanelUI(this)
}