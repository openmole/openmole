/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.model.panel

import org.openmole.ide.core.model.data.IHookDataUI
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI

trait IHookPanelUI extends IOPanelUI {

  def saveContent(name: String): IHookDataUI

  def save(name: String,
           prototypesIn: Seq[IPrototypeDataProxyUI],
           inputParameters: Map[IPrototypeDataProxyUI, String],
           prototypesOut: Seq[IPrototypeDataProxyUI]): IHookDataUI = {
    var dataUI = saveContent(name)
    dataUI.inputs = prototypesIn
    dataUI.outputs = prototypesOut
    dataUI.inputParameters = inputParameters
    dataUI
  }
}