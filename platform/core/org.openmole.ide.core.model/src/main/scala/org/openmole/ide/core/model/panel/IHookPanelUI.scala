/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.model.panel

import scala.swing.Panel
import org.openmole.ide.core.model.data.IHookDataUI

trait IHookPanelUI extends Panel with IPanelUI{
  def saveContent(name:String): IHookDataUI
}