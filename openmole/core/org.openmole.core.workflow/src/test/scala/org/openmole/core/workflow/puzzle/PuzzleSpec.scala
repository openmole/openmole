package org.openmole.core.workflow.puzzle
import org.openmole.core.context.Val
import org.openmole.core.exception.InternalProcessingError
import org.openmole.core.workflow.data._
import org.scalatest._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.transition._
import org.openmole.core.workflow.puzzle._
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.task._
import scala.util.Try

class PuzzleSpec extends FlatSpec with Matchers {
  import org.openmole.core.workflow.tools.Stubs._

  "HList containing puzzle" should "be implicily convertible to a ToPuzze" in {
    import shapeless._

    val caps = Capsule(EmptyTask())
    val test = PuzzleContainer(caps, caps) :: 9 :: HNil

    (test: MoleExecution)
  }
}
