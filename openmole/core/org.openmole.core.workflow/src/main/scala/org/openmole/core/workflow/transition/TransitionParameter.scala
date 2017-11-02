package org.openmole.core.workflow.transition

import org.openmole.core.context.Val
import org.openmole.core.expansion.Condition
import org.openmole.core.workflow.puzzle.{ Puzzle, ToPuzzle }

object ToTransitionParameter {
  def apply[T](f: T ⇒ TransitionParameter): ToTransitionParameter[T] =
    new ToTransitionParameter[T] {
      def apply(t: T) = f(t)
    }

  implicit val transitionParameter: ToTransitionParameter[TransitionParameter] = ToTransitionParameter[TransitionParameter](identity)
  implicit def puzzleToTransitionParameter[T](implicit toPuzzle: ToPuzzle[T]): ToTransitionParameter[T] = ToTransitionParameter[T](t ⇒ TransitionParameter(toPuzzle.toPuzzle(t)))
}

trait ToTransitionParameter[T] {
  def apply(t: T): TransitionParameter
}

case class TransitionParameter(
  puzzleParameter:    Puzzle,
  conditionParameter: Condition = Condition.True,
  filterParameter:    BlockList = BlockList.empty
) {
  def when(condition: Condition) = copy(conditionParameter = condition)
  def filter(filter: BlockList) = copy(filterParameter = filter)
  def keep(prototypes: Val[_]*) = filter(Keep(prototypes: _*))
  def block(prototypes: Val[_]*) = filter(Block(prototypes: _*))
}
