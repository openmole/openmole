/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.implementation.panelsettings

import org.openmole.ide.core.implementation.data.HookDataUI
import org.openmole.ide.core.implementation.panel.{ SaveSettings, Settings }

trait HookPanelUI extends Settings with SaveSettings {
  type DATAUI = HookDataUI
  // def saveContent(name: String): HookDataUI
}