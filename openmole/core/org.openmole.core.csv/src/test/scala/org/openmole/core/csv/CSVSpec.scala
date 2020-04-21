package org.openmole.core.csv

import java.io.PrintStream

import org.openmole.tool.stream.StringOutputStream
import org.scalatest._
import org.scalatest.junit._

class CSVSpec extends FlatSpec with Matchers {

  def result(f: PrintStream ⇒ Unit): String = {
    val result = new StringOutputStream()
    val printStream = new PrintStream(result)
    f(printStream)
    printStream.close()
    result.builder.toString
  }

  "Function" should "produce conform csv" in {
    assert(
      result(writeVariablesToCSV(_, None, Seq(42, 56, Array(89, 89)))) ===
        """42,56,"[89,89]"
          |""".stripMargin
    )

    assert(
      result(writeVariablesToCSV(_, None, Seq(42, 56, Array(89, 101)), unrollArrays = true)) ===
        """42,56,89
        |42,56,101
        |""".stripMargin
    )
  }
}
