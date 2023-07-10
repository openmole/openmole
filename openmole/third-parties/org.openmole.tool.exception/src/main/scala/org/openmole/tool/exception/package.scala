package org.openmole.tool

package object exception :

  def tryOnError[A, B](onError: ⇒ A)(op: ⇒ B) =
    try op
    catch
      case t: Throwable ⇒
        util.Try(onError)
        throw t

