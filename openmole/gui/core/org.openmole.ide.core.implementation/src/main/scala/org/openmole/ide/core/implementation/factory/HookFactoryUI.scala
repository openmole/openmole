/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.implementation.factory

import org.openmole.core.model.mole._
import org.openmole.ide.core.implementation.data.HookDataUI

trait HookFactoryUI extends FactoryUI {
  override def toString: String = ""

  def buildDataUI: HookDataUI

  def coreClass: Class[_ <: IHook]

  def category = List()

}