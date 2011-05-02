/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.properties

import scala.collection.mutable.ListBuffer

object NativeFactories {
  val prototypeFactories = new ListBuffer[IFactoryUI]
  prototypeFactories += new PrototypeFactoryUI(classOf[java.lang.Integer],"img/thumb/integer.png")
  prototypeFactories += new PrototypeFactoryUI(classOf[java.lang.Double],"img/thumb/double.png")
  prototypeFactories += new PrototypeFactoryUI(classOf[java.io.File],"img/thumb/file.png")
  prototypeFactories += new PrototypeFactoryUI(classOf[java.lang.String],"img/thumb/string.png")
  prototypeFactories += new PrototypeFactoryUI(classOf[java.math.BigDecimal],"img/thumb/bigdecimal.png")
  prototypeFactories += new PrototypeFactoryUI(classOf[java.math.BigInteger],"img/thumb/biginteger.png")
}
