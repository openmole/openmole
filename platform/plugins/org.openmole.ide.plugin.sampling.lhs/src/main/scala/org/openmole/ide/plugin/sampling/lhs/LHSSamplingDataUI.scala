/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.sampling.lhs

import org.openmole.ide.core.model.dataproxy._
import org.openmole.ide.core.model.data.IBoundedDomainDataUI
import org.openmole.ide.core.model.data.ISamplingDataUI
import org.openmole.plugin.sampling.lhs.LHSSampling
import org.openmole.core.implementation.sampling.Factor
import org.openmole.core.model.domain.IBounded
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.domain.IIterable
import scala.collection.JavaConversions._
import org.openide.awt.StatusDisplayer
import org.openmole.misc.exception.UserBadDataError

class LHSSamplingDataUI(val name : String="", 
                        val samples : String  = "",
                        val factors : List[(IPrototypeDataProxyUI,String,IBoundedDomainDataUI)] = List.empty) extends ISamplingDataUI {
                     
  implicit def string2Int(s: String): Int = augmentString(s).toInt

  def coreObject = 
    new LHSSampling(try { samples 
      } catch { case e : NumberFormatException => throw new UserBadDataError("An integer is exepected as number of samples") },
                    factors.map{f=>
        val proto = f._1.dataUI.coreObject.asInstanceOf[IPrototype[Double]]
        new Factor(proto,
                   f._3.coreObject(proto))
                   //  .asInstanceOf[IDomain[Double] with IBounded[Double]]
        }.toArray)


  def coreClass = classOf[LHSSampling] 
  
  def imagePath = "img/lhsSampling.png" 
  
  override def fatImagePath = "img/lhsSampling_fat.png" 
  
  def buildPanelUI = new LHSSamplingPanelUI(this)
}
