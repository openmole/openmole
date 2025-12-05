package org.openmole.core.argument

import org.scalatest.*

import scala.util.Random

class ArgumentSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers:

  "OptionalArgument" should "accept the folowing syntaxes" in:
     val x1: OptionalArgument[FromContext[Int]] = 10
     val x2: OptionalArgument[FromContext[Int]] = None
     val x3: OptionalArgument[FromContext[Int]] = "10 * 10"




