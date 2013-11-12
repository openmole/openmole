/*
 * Copyright (C) 2013 Mathieu Mathieu Leclaire <mathieu.Mathieu Leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.ide.core.implementation.builder

import org.openmole.core.model.task.ITask
import org.openmole.ide.core.implementation.registry._
import org.openmole.core.implementation.puzzle.Puzzle
import java.awt.Point
import org.openmole.core.model.transition._
import org.openmole.ide.core.implementation.sampling.{ IBuiltCompositionSampling, DomainProxyUI, SamplingProxyUI, SamplingCompositionDataUI }
import org.openmole.core.model.domain.Domain
import org.openmole.core.model.data.Prototype
import org.openmole.ide.core.implementation.data.{ HookDataUI, CapsuleDataUI, CheckData }
import org.openmole.core.model.transition.{ IEndExplorationTransition, IExplorationTransition, IAggregationTransition }
import org.openmole.ide.core.implementation.dialog.StatusBar
import org.openide.DialogDescriptor
import org.openide.DialogDisplayer
import org.openide.NotifyDescriptor
import scala.swing.ScrollPane
import org.openmole.misc.exception.{ InternalProcessingError, UserBadDataError }
import org.openmole.ide.core.implementation.prototype.GenericPrototypeDataUI._
import scala.Some
import org.openmole.ide.core.implementation.registry.DefaultKey
import org.openmole.ide.core.implementation.workflow.{ TransitionUI, CapsuleUI }
import org.openmole.core.implementation.transition.Condition
import org.openmole.ide.core.implementation.registry.DefaultKey
import org.openmole.ide.core.implementation.registry.DefaultKey
import scala.Some
import org.openmole.ide.core.implementation.factory.BuilderFactoryUI
import org.openmole.ide.core.implementation.dataproxy._
import org.openmole.misc.tools.obj.ClassUtils._
import org.openmole.core.model.sampling.Sampling
import org.openmole.ide.core.implementation.registry.DefaultKey
import scala.Some

object Builder {

  def prototypeUI = PrototypeDataProxyUI(stringToDataUI("double"), false)

  def samplingCompositionUI(b: Boolean) = SamplingCompositionDataProxyUI(g = b)

  def hookUI(g: Boolean) = {
    val hookValues = KeyRegistry.hooks.values
    HookDataProxyUI(hookValues.find { _.toString == "Display" }.getOrElse(hookValues.head).buildDataUI, g)
  }

  def sourceUI(g: Boolean) = {
    val sourceValues = KeyRegistry.sources.values
    SourceDataProxyUI(sourceValues.find { _.toString == "CSV" }.getOrElse(sourceValues.head).buildDataUI, g)
  }

  def taskUI(g: Boolean) = {
    val taskValues = KeyRegistry.tasks.values
    TaskDataProxyUI(taskValues.find { _.toString == "Groovy" }.getOrElse(taskValues.head).buildDataUI, g)
  }

  def environmentUI(g: Boolean) = {
    val envValues = KeyRegistry.environments.values
    EnvironmentDataProxyUI(envValues.find { _.toString == "Multi threading" }.getOrElse(envValues.head).buildDataUI, g)
  }

  /* def puzzles(listsPuzzleCompliant: List[List[CapsuleUI]],
              manager: MoleUI,
              uiMap: PuzzleUIMap = new PuzzleUIMap): (List[Puzzle], PuzzleUIMap) = {

    def puzzles0(toBeComputed: List[List[CapsuleUI]], puzzleList: List[Puzzle], uiMap0: PuzzleUIMap): (List[Puzzle], PuzzleUIMap) = {
      if (toBeComputed.isEmpty) (puzzleList, uiMap0)
      else {
        val firsts = manager.firstCapsules(toBeComputed.head)
        val lasts = manager.lastCapsules(toBeComputed.head)
        if (manager.isFirstCompliant(firsts) && manager.isLastCompliant(lasts)) {
          val (p, newUIMap) = puzzle(toBeComputed.head, manager.firstCapsules(toBeComputed.head).head, manager.lastCapsules(toBeComputed.head), uiMap0)
          puzzles0(toBeComputed.tail, puzzleList :+ p, newUIMap)
        }
        else throw new UserBadDataError("A builder can be applied on a none empty sequence of Tasks containing only one first Task and only one final Task")
      }
    }
    puzzles0(listsPuzzleCompliant, List(), uiMap)
  }

  def puzzle(capsulesUI: List[CapsuleUI],
             first: CapsuleUI,
             lasts: Iterable[CapsuleUI],
             uiMap: PuzzleUIMap = PuzzleUIMap()) = {
    val capsuleMap = capsulesUI.map {
      c ⇒ c -> c.dataUI.coreObject(c.scene.dataUI).get
    }.toMap
    val (transitions, dataChannels, islotMap) = MoleFactory.buildConnectors(capsuleMap)
    islotMap += first.islots.head -> Slot(capsuleMap(first))
    (new Puzzle(islotMap(first.islots.head),
      lasts.map {
        capsuleMap
      },
      transitions,
      dataChannels,
      List.empty,
      List.empty,
      Map.empty,
      Map.empty),
      PuzzleUIMap(
        capsuleMap.filter { _._1.dataUI.task.isDefined }.map {
          case (ui, c) ⇒ c.task -> ui.dataUI.task.get
        } ++ uiMap.taskMap,
        MoleFactory.samplingMapping.map {
          case (k, v) ⇒ v -> k
        } ++ uiMap.samplingMap,
        MoleFactory.moleMapping.map {
          case (k, v) ⇒ v -> k
        } ++ uiMap.moleMap))
  }

  def fromPuzzle(p: Puzzle,
                 scene: BuildMoleScene,
                 firstPoint: Point,
                 lastPoint: Point,
                 uiMap: PuzzleUIMap) = {
    val capsuleMap = p.slots.zipWithIndex.map {
      s ⇒
        val proxy = toTaskUI(s._1.capsule.task, uiMap)
        val capsules = scene.dataUI.capsule(proxy)
        val slotUI = {
          if (capsules.isEmpty) {
            Proxies.instance += proxy
            val capsule = CapsuleUI(scene, CapsuleDataUI(Some(proxy)))
            scene.add(capsule, new Point(0, 0))
            capsule.addInputSlot
          }
          else capsules.head.islots.head
        }
        if (s._2 == 0) scene.dataUI.startingCapsule = Some(slotUI.capsule)
        s._1.capsule -> slotUI
    }.toMap

    p.transitions.foreach {
      t ⇒
        val transition = new TransitionUI(
          capsuleMap(t.start).capsule,
          capsuleMap(t.end.capsule),
          t match {
            case ex: IExplorationTransition     ⇒ ExplorationTransitionType
            case agg: IAggregationTransition    ⇒ AggregationTransitionType
            case end: IEndExplorationTransition ⇒ EndTransitionType
            case _: ITransition                 ⇒ SimpleTransitionType
            case x                              ⇒ throw new InternalProcessingError("Unsupported transition class " + x.getClass)
          },
          Some(t.asInstanceOf[Condition].code),
          t.filter match {
            case b: Block[String] ⇒
              capsuleMap(t.start).capsule.outputs.filter(o ⇒ b.filtered.contains(o.dataUI.name))
            case _ ⇒ throw new InternalProcessingError("Filter not supported yet")
          })
        scene.add(transition)
    }
    CheckData.fullCheck(scene)
    scene.refresh
  }    */

  def toTaskUI(t: ITask, uiMap: PuzzleUIMap) =
    KeyRegistry.task(t.getClass).buildDataProxyUI(t, uiMap)

  def toSamplingCompositionUI(s: Sampling): SamplingCompositionDataProxyUI = {
    val (sProxy, bcs) = toSamplingUI(s, new BuiltCompositionSampling)

    SamplingCompositionDataProxyUI(new SamplingCompositionDataUI(sProxy.dataUI.name,
      bcs.builtDomains.toList.zipWithIndex.map {
        d ⇒ (d._1, new Point(10, d._2 * 20))
      },
      bcs.builtSamplings.toList.zipWithIndex.map {
        s ⇒ (s._1, new Point(s._2 * 40, 20))
      },
      List(), //factors
      bcs.builtConnections.toList,
      Some(sProxy),
      (300, 250)))
  }

  def toSamplingUI(s: Sampling,
                   bsc: IBuiltCompositionSampling) = {
    val (proxy, newBSC) = KeyRegistry.sampling(s.getClass).fromCoreObject(s, bsc)
    (proxy, newBSC.copyWithSamplings(proxy))
  }

  def toDomainUI(d: Domain[_],
                 bsc: IBuiltCompositionSampling) = {
    //val (proxy, newBSC) = KeyRegistry.domains(new DefaultKey(d.getClass)).fromCoreObject(d)
    val proxy = DomainProxyUI(KeyRegistry.domains(new DefaultKey(d.getClass)).buildDataUI)
    (proxy, bsc.copyWithDomains(proxy))
  }

  def buildConnectedDomain(proxy: DomainProxyUI,
                           connectedDomain: Domain[_],
                           bcs: IBuiltCompositionSampling): IBuiltCompositionSampling = {
    val (p, newBSC) = Builder.toDomainUI(connectedDomain, bcs)
    newBSC.copyWithConnections((proxy, p))
  }

  def buildConnectedSamplings(proxy: SamplingProxyUI,
                              connectedSamplings: Seq[Sampling],
                              bcs: IBuiltCompositionSampling): IBuiltCompositionSampling = {

    def buildSamplingUI0(remainingSamplings: Seq[Sampling],
                         bcs0: IBuiltCompositionSampling): IBuiltCompositionSampling = {
      if (remainingSamplings.isEmpty) bcs0
      else {
        val newBsc = remainingSamplings.head match {
          case d: Domain[_] ⇒
            val (p, bsc) = Builder.toDomainUI(d, bcs0)
            bsc.copyWithConnections((proxy, p))
          case s: Sampling ⇒
            val (p, bsc) = Builder.toSamplingUI(s, bcs0)
            bsc.copyWithConnections((proxy, p))
          case _ ⇒ bcs0
        }
        buildSamplingUI0(remainingSamplings.tail, newBsc)
      }
    }
    buildSamplingUI0(connectedSamplings, bcs)
  }

  /* def apply(scene: BuildMoleScene,
            b: BuilderFactoryUI,
            sel: List[CapsuleUI] = List()) = {
    try {
      StatusBar.clear
      val selection = {
        if (sel.isEmpty) {
          if (scene.dataUI.puzzlesCompliant.isEmpty) throw new UserBadDataError("Builder error: no Sequence of Task has been found.")
          else scene.dataUI.puzzlesCompliant.head
        }
        else sel
      }
      val selectedPuzzle = puzzles(List(scene.dataUI.puzzleCompliant(selection)), scene.dataUI)
      val compliantPuzzles = scene.dataUI.puzzlesCompliant
      if (compliantPuzzles.isEmpty) StatusBar().warn("No Sequence of Tasks can be applied for a Builder")
      else {
        val (puzzles, uiMap) = Builder.puzzles(compliantPuzzles, scene.dataUI, selectedPuzzle._2)
        val panel = b.buildPanelUI(puzzles, {
          if (selectedPuzzle._1.isEmpty) None else Some(selectedPuzzle._1.head)
        }, scene.dataUI)
        if (DialogDisplayer.getDefault.notify(new DialogDescriptor(new ScrollPane(panel) {
          verticalScrollBarPolicy = ScrollPane.BarPolicy.AsNeeded
        }.peer,
          b.name + " Builder")).equals(NotifyDescriptor.OK_OPTION)) {
          val (puzzle, updatedUIMap) = panel.build(uiMap)
          Builder.fromPuzzle(puzzle,
            scene,
            new Point(0, 0),
            new Point(0, 0),
            updatedUIMap)
        }
      }
    }
    catch {
      case e: UserBadDataError ⇒ StatusBar().warn(e.getMessage)
    }
  } */

}
