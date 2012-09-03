/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.model.data

import org.openmole.core.model.data._
import org.openmole.core.model.domain.IBounded
import org.openmole.core.model.domain.IDomain
import org.openmole.ide.core.model.panel.IBoundedDomainPanelUI

trait IBoundedDomainDataUI extends IDataUI {

  def coreObject(proto: Prototype[Double]): IDomain[Double] with IBounded[Double]

  def buildPanelUI: IBoundedDomainPanelUI
}