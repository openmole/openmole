/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.sampling.combine

import org.openmole.ide.core.model.dataproxy._
import org.openmole.ide.core.model.data._
import org.openmole.plugin.sampling.combine.CompleteSampling
import org.openmole.core.implementation.sampling.DiscreteFactor
import org.openmole.core.model.data.Prototype
import org.openmole.core.model.domain.IDomain
import org.openmole.core.model.domain.IIterable
import scala.collection.JavaConversions._

class CompleteSamplingDataUI(val name: String = "",
                             // val factors: List[(IPrototypeDataProxyUI, String, IDomainDataUI)] = List.empty) extends ISamplingDataUI {
                             override val factors: List[IFactorDataUI] = List.empty,
                             val samplings: List[ISamplingDataProxyUI] = List.empty) extends ISamplingDataUI {

  def coreObject =
    new CompleteSampling(
      factors.flatMap(f ⇒
        f.prototype match {
          case Some(p: IPrototypeDataProxyUI) ⇒ f.domain match {
            case Some(d: IDomainDataUI) ⇒ List(new DiscreteFactor(p.dataUI.coreObject.asInstanceOf[Prototype[Any]],
              d.coreObject(p.dataUI.coreObject).asInstanceOf[IDomain[Any] with IIterable[Any]]))
            case _ ⇒ Nil
          }
          case _ ⇒ Nil
        }).toSeq: _*)

  //      new CompleteSampling(
  //      factors.map(f ⇒ new DiscreteFactor(
  //          f._1.dataUI.coreObject.asInstanceOf[Prototype[Any]],
  //          f._3.coreObject(f._1.dataUI.coreObject).asInstanceOf[IDomain[Any] with IIterable[Any]])).toSeq: _*)

  def coreClass = classOf[CompleteSampling]

  def imagePath = "img/completeSampling.png"

  def fatImagePath = "img/completeSampling_fat.png"

  def buildPanelUI = new CompleteSamplingPanelUI(this)
}
