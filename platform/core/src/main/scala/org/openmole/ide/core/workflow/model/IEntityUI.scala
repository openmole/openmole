/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.workflow.model

import java.awt.Panel
import org.openmole.ide.core.properties.IFactoryUI

trait IEntityUI {
  def name: String
  
  def factory: IFactoryUI
}

//public interface IEntityUI {
//    String getName();
//    Class getType();
//}
