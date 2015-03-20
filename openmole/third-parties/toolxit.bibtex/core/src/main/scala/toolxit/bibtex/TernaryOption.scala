/*
* This file is part of the ToolXiT project.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package toolxit.bibtex

/**
 * This abstract class extends the standard Scala Option type with a new state: Error(msg)
 *
 * @author Lucas Satabin
 *
 */
sealed abstract class TOption[+A] extends Product with Serializable {

  self ⇒

  def isEmpty: Boolean

  def isError: Boolean

  def isDefined: Boolean = !isEmpty

  def get: A

  def message: String

  @inline final def getOrElse[B >: A](default: ⇒ B): B =
    if (isEmpty) default else this.get

  @inline final def map[B](f: A ⇒ B): TOption[B] =
    if (isError) TError(message) else if (isEmpty) TNone else TSome(f(this.get))

  @inline final def flatMap[B](f: A ⇒ TOption[B]): TOption[B] =
    if (isError) TError(message) else if (isEmpty) TNone else f(this.get)

  @inline final def filter(p: A ⇒ Boolean): TOption[A] =
    if (isEmpty || p(this.get)) this else TNone

  @inline final def foreach[U](f: A ⇒ U) {
    if (!isEmpty) f(this.get)
  }

  @inline final def orElse[B >: A](alternative: ⇒ TOption[B]): TOption[B] =
    if (isEmpty) alternative else this
}

case class TSome[+A](value: A) extends TOption[A] {
  def isEmpty = false
  def isError = false
  def get = value
  def message = throw new NoSuchElementException("TSome.message")
}

case object TNone extends TOption[Nothing] {
  def isEmpty = true
  def isError = false
  def get = throw new NoSuchElementException("TNone.get")
  def message = throw new NoSuchElementException("TNone.message")
}

case class TError(val message: String, val exc: Exception = null) extends TOption[Nothing] {
  def isEmpty = true
  def isError = true
  def get = throw new NoSuchElementException("TError.get")
}