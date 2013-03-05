/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.model.panel

import org.openmole.ide.core.model.data.IHookDataUI

trait IHookPanelUI extends IPanelUI {
  def saveContent(name: String): IHookDataUI
}