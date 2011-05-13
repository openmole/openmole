/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.prototype.base

import java.io.File
import org.openmole.ide.core.exception.GUIUserBadDataError
import org.openmole.ide.core.properties.PanelUIData

class BasePrototypePanelUIData extends PanelUIData {
  
  var simpleTypeString = "Integer"
  
  def typeClass = {
    simpleTypeString match {
      case "Integer"=> classOf[Int]
      case "Double"=> classOf[Double]
      case "File"=> classOf[File]
      case "String"=> classOf[String]
      case "BigInteger"=> classOf[BigInt]
      case "BigDecimal"=> classOf[BigDecimal]
      case _=> throw new GUIUserBadDataError("Unknown type " + simpleTypeString + " for prototype " + name)
    }
  }
}
