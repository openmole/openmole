/*
 * Copyright (C) 2013 Mathieu Leclaire
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
package org.openmole.ide.core.implementation.builder

import org.openmole.ide.core.implementation.dataproxy.{ Proxys, SamplingCompositionDataProxyUI }
import org.openmole.core.model.sampling.Sampling
import org.openmole.core.model.task.ITask
import org.openmole.ide.core.implementation.registry._
import org.openmole.ide.core.model.workflow.{ IMoleSceneManager, IMoleScene, ICapsuleUI }
import org.openmole.core.implementation.puzzle.Puzzle
import java.awt.Point
import org.openmole.core.model.transition._
import org.openmole.ide.core.model.sampling.IBuiltCompositionSampling
import org.openmole.ide.core.model.dataproxy.ISamplingCompositionDataProxyUI
import org.openmole.ide.core.implementation.sampling.{ FactorProxyUI, DomainProxyUI, SamplingProxyUI, SamplingCompositionDataUI }
import org.openmole.core.model.domain.Domain
import org.openmole.core.model.data.Prototype
import org.openmole.ide.core.model.builder.IPuzzleUIMap
import org.openmole.misc.tools.obj.ClassUtils
import org.openmole.misc.tools.obj.ClassUtils._
import org.openmole.ide.core.implementation.data.CheckData
import org.openmole.ide.core.model.commons.TransitionType
import org.openmole.core.model.transition.{ IEndExplorationTransition, IExplorationTransition, IAggregationTransition }
import org.openmole.ide.core.model.factory.IBuilderFactoryUI
import org.openmole.ide.core.implementation.dialog.StatusBar
import org.openmole.ide.core.implementation.execution.ScenesManager
import org.openide.DialogDescriptor
import org.openide.DialogDisplayer
import org.openide.NotifyDescriptor
import scala.swing.ScrollPane
import org.openmole.misc.exception.UserBadDataError

object Builder {

  def samplingCompositionUI(g: Boolean) = new SamplingCompositionDataProxyUI(generated = g)

  def puzzles(listsPuzzleCompliant: List[List[ICapsuleUI]],
              manager: IMoleSceneManager,
              uiMap: IPuzzleUIMap = new PuzzleUIMap): (List[Puzzle], IPuzzleUIMap) = {

    def puzzles0(toBeComputed: List[List[ICapsuleUI]], puzzleList: List[Puzzle], uiMap0: IPuzzleUIMap): (List[Puzzle], IPuzzleUIMap) = {
      if (toBeComputed.isEmpty) (puzzleList, uiMap0)
      else {
        val firsts = manager.firstCapsules(toBeComputed.head)
        val lasts = manager.lastCapsules(toBeComputed.head)
        if (manager.isFirstCompliant(firsts) && manager.isLastCompliant(lasts)) {
          val (p, newUIMap) = puzzle(toBeComputed.head, manager.firstCapsules(toBeComputed.head).head, manager.lastCapsules(toBeComputed.head), uiMap0)
          puzzles0(toBeComputed.tail, puzzleList :+ p, newUIMap)
        } else throw new UserBadDataError("A builder can be applied on a none empty sequence of Tasks containing only one first Task and only one final Task")
      }
    }
    puzzles0(listsPuzzleCompliant, List(), uiMap)
  }

  def puzzle(capsulesUI: List[ICapsuleUI],
             first: ICapsuleUI,
             lasts: Iterable[ICapsuleUI],
             uiMap: IPuzzleUIMap = new PuzzleUIMap) = {
    val capsuleMap = capsulesUI.map {
      c ⇒ c -> c.dataUI.coreObject(c.scene.manager.dataUI)
    }.toMap
    val prototypeMap = MoleFactory.prototypeMapping
    val (transitions, dataChannels, islotMap) = MoleFactory.buildConnectors(capsuleMap, prototypeMap)
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
      Map.empty), new PuzzleUIMap(capsuleMap.map {
      case (ui, c) ⇒ c.task -> ui.dataUI.task.get
    } ++ uiMap.taskMap,
      prototypeMap.map {
        case (k, v) ⇒ v.asInstanceOf[Prototype[Any]] -> k
      } ++ uiMap.prototypeMap,
      MoleFactory.samplingMapping.map {
        case (k, v) ⇒ v -> k
      } ++ uiMap.samplingMap,
      MoleFactory.moleMapping.map {
        case (k, v) ⇒ v -> k
      } ++ uiMap.moleMap))
  }

  def fromPuzzle(p: Puzzle,
                 scene: IMoleScene,
                 firstPoint: Point,
                 lastPoint: Point,
                 uiMap: IPuzzleUIMap) = {
    val capsuleMap = p.slots.zipWithIndex.map {
      s ⇒
        val proxy = toTaskUI(s._1.capsule.task, uiMap)
        val capsules = scene.manager.capsule(proxy)
        val cUI = {
          if (capsules.isEmpty) {
            Proxys += proxy
            SceneFactory.capsuleUI(scene, new Point(0, 0), Some(proxy)).addInputSlot(false)
          } else capsules.head.islots.head
        }
        if (s._2 == 0) scene.manager.setStartingCapsule(cUI.capsule)
        s._1.capsule -> cUI
    }.toMap

    p.transitions.foreach {
      t ⇒
        SceneFactory.transition(scene,
          capsuleMap(t.start).capsule,
          capsuleMap(t.end.capsule),
          t match {
            case ex: IExplorationTransition ⇒ TransitionType.EXPLORATION_TRANSITION
            case agg: IAggregationTransition ⇒ TransitionType.AGGREGATION_TRANSITION
            case end: IEndExplorationTransition ⇒ TransitionType.END_TRANSITION
            case _ ⇒ TransitionType.BASIC_TRANSITION
          }, //TransitionType.Value,
          //  Some(t.condition.),
          li = t.filter match {
            case b: Block[String] ⇒ b.filtered.toList.map {
              p ⇒
                uiMap.prototype(p)
            }
            case _ ⇒ List()
          })
    }
    CheckData.fullCheck(scene)
    scene.refresh
  }

  def toTaskUI(t: ITask, uiMap: IPuzzleUIMap) =
    KeyRegistry.task(t.getClass).buildDataProxyUI(t, uiMap)

  def toSamplingCompositionUI(s: Sampling): ISamplingCompositionDataProxyUI = {
    val (sProxy, bcs) = toSamplingUI(s, new BuiltCompositionSampling)

    new SamplingCompositionDataProxyUI(new SamplingCompositionDataUI(sProxy.dataUI.name,
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
    val proxy = new DomainProxyUI(KeyRegistry.domains(new DefaultKey(d.getClass)).buildDataUI)
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

  def apply(scene: IMoleScene,
            b: IBuilderFactoryUI,
            sel: List[ICapsuleUI] = List()) = {
    try {
      StatusBar().clear
      val selection = {
        if (sel.isEmpty) {
          if (scene.manager.puzzlesCompliant.isEmpty) throw new UserBadDataError("Builder error: no Sequence of Task has been found.")
          else scene.manager.puzzlesCompliant.head
        } else sel
      }
      val selectedPuzzle = puzzles(List(scene.manager.puzzleCompliant(selection)), scene.manager)
      val compliantPuzzles = scene.manager.puzzlesCompliant
      if (compliantPuzzles.isEmpty) StatusBar().warn("No Sequence of Tasks can be applied for a Builder")
      else {
        val (puzzles, uiMap) = Builder.puzzles(compliantPuzzles, scene.manager, selectedPuzzle._2)
        val panel = b.buildPanelUI(puzzles, {
          if (selectedPuzzle._1.isEmpty) None else Some(selectedPuzzle._1.head)
        }, scene.manager)
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
    } catch {
      case e: UserBadDataError ⇒ StatusBar().warn(e.getMessage)
    }
  }

}
