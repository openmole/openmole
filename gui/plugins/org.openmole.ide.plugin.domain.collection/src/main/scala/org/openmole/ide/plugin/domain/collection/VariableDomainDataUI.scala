/*
 * Copyright (C) 2012 Mathieu Leclaire 
 * < mathieu.leclaire at openmole.org >
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.plugin.domain.collection

import org.openmole.ide.core.model.data.{ IFactorDataUI, IDomainDataUI }
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.misc.tools.util.Types._
import java.math.BigInteger
import java.math.BigDecimal
import org.openmole.misc.exception.UserBadDataError
import org.openmole.plugin.domain.collection.VariableDomain
import org.openmole.core.model.data.Prototype
import scala.swing.Label
import org.openmole.ide.core.model.panel.{ IDomainPanelUI, IPanelUI }
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.core.model.sampling.IFinite
import org.openmole.ide.misc.tools.util.Types

object VariableDomainDataUI {
  def apply[T](prototypeArray: Option[IPrototypeDataProxyUI], classString: String) = {
    Types.standardize(classString) match {
      case INT ⇒ new VariableDomainDataUI[Int](prototypeArray)
      case DOUBLE ⇒ new VariableDomainDataUI[Double](prototypeArray)
      case BIG_DECIMAL ⇒ new VariableDomainDataUI[BigDecimal](prototypeArray)
      case BIG_INTEGER ⇒ new VariableDomainDataUI[BigInteger](prototypeArray)
      case LONG ⇒ new VariableDomainDataUI[Long](prototypeArray)
      case STRING ⇒ new VariableDomainDataUI[String](prototypeArray)
      case x: Any ⇒ throw new UserBadDataError("The type " + x + " is not supported")
    }
  }
}

class VariableDomainDataUI[S](val prototypeArray: Option[IPrototypeDataProxyUI] = None)(implicit val domainType: Manifest[S])
    extends IDomainDataUI with IFinite {
  vdomainDataUI ⇒

  val name = "Prototype Array"

  def coreObject = prototypeArray match {
    case Some(p: IPrototypeDataProxyUI) ⇒ new VariableDomain(p.dataUI.coreObject.asInstanceOf[Prototype[Array[S]]])
    case _ ⇒ throw new UserBadDataError("An array of Prototypes is required for a Prototype Array Domain")
  }

  def buildPanelUI = new VariableDomainPanelUI(this)

  def preview = "in " + prototypeArray.getOrElse("None").toString

  def coreClass = classOf[VariableDomainDataUI[S]]
}