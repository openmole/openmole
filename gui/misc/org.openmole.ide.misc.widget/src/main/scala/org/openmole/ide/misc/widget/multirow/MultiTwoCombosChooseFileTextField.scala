/*
 * Copyright (C) 2011 Mathieu Leclaire
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

package org.openmole.ide.misc.widget.multirow

import org.openmole.ide.misc.tools.image.Images
import org.openmole.ide.misc.widget.ChooseFileTextField
import org.openmole.ide.misc.widget._
import org.openmole.ide.misc.widget.multirow.MultiWidget._
import org.openmole.ide.misc.widget.multirow.RowWidget.Plus
import org.openmole.ide.misc.widget.multirow.RowWidget._
import scala.swing.Button
import scala.swing.MyComboBox
import scala.swing.Label

object MultiTwoCombosChooseFileTextField {

  class TwoCombosChooseFileTextFieldPanel[A, B](comboContent1: List[A],
                                                comboContent2: List[B],
                                                inBetweenString1: String,
                                                inBetweenString2: String,
                                                data: TwoCombosChooseFileTextFieldData[A, B])
      extends PluginPanel("wrap 6") with IPanel[TwoCombosChooseFileTextFieldData[A, B]] {
    val combo1 = new MyComboBox[A](comboContent1.sortBy { _.toString }) {
      data.comboValue1 match {
        case Some(x: A) ⇒ selection.item = x
        case _ ⇒
      }
    }

    val combo2 = new MyComboBox[B](comboContent2.sortBy { _.toString }) {
      data.comboValue2 match {
        case Some(x: B) ⇒ selection.item = x
        case _ ⇒
      }
    }

    val chooseFileText = new ChooseFileTextField(data.filePath)
    val refreshButton = new Button { icon = Images.REFRESH }

    contents += combo1
    contents += new Label(inBetweenString1)
    contents += combo2
    contents += new Label(inBetweenString2)
    contents += chooseFileText
    contents += refreshButton

    def content = new TwoCombosChooseFileTextFieldData(Some(combo1.selection.item),
      Some(combo2.selection.item),
      chooseFileText.text)
  }

  class TwoCombosChooseFileTextFieldData[A, B](val comboValue1: Option[A] = None,
                                               val comboValue2: Option[B] = None,
                                               val filePath: String = "") extends IData

  class TwoCombosChooseFileTextFieldFactory[A, B](comboContent1: List[A],
                                                  comboContent2: List[B],
                                                  inBetweenString1: String,
                                                  inBetweenString2: String) extends IFactory[TwoCombosChooseFileTextFieldData[A, B]] {
    def apply = new TwoCombosChooseFileTextFieldPanel(comboContent1,
      comboContent2,
      inBetweenString1,
      inBetweenString2,
      new TwoCombosChooseFileTextFieldData)
  }
}
import MultiTwoCombosChooseFileTextField._

class MultiTwoCombosChooseFileTextField[A, B](title: String,
                                              comboContent1: List[A],
                                              comboContent2: List[B],
                                              inBetweenString1: String,
                                              inBetweenString2: String,
                                              initPanels: List[TwoCombosChooseFileTextFieldPanel[A, B]],
                                              minus: Minus = NO_EMPTY,
                                              plus: Plus = ADD) extends MultiPanel(title,
  new TwoCombosChooseFileTextFieldFactory(comboContent1,
    comboContent2,
    inBetweenString1,
    inBetweenString2),
  initPanels,
  minus,
  plus)