/*
 * Copyright (C) 2012 mathieu
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

package org.openmole.ide.core.implementation.sampling

import org.openmole.misc.exception.UserBadDataError
import org.openmole.ide.misc.widget.MigPanel
import org.openmole.ide.core.implementation.data._
import java.awt._
import org.netbeans.api.visual.action.ActionFactory
import org.netbeans.api.visual.action.ConnectorState
import org.netbeans.api.visual.action.ConnectorState._
import org.netbeans.api.visual.anchor.Anchor
import org.netbeans.api.visual.anchor.AnchorShape
import org.netbeans.api.visual.action.ConnectProvider
import org.netbeans.api.visual.widget._
import java.awt.event.InputEvent
import java.util.concurrent.atomic.AtomicInteger
import org.openmole.ide.core.model.data._
import org.openmole.ide.core.model.panel.ISamplingCompositionPanelUI
import org.openmole.misc.exception.UserBadDataError
import org.openmole.ide.core.implementation.provider._
import org.openmole.ide.core.model.sampling._
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import org.openmole.ide.core.implementation.dialog.StatusBar
import collection.mutable
import org.openmole.ide.core.model.workflow.IConnectorViewUI
import org.openmole.ide.core.implementation.workflow.MoleRouter

object SamplingCompositionPanelUI {
  val DEFAULT_COLOR = new Color(250, 250, 250)
  val DEFAULT_COLOR_CENTER = new Color(228, 228, 228)
}

import SamplingCompositionPanelUI._

class SamplingCompositionPanelUI(val dataUI: ISamplingCompositionDataUI) extends Scene with ISamplingCompositionPanelUI {
  samplingCompositionPanelUI ⇒

  val domains = new HashMap[IDomainProxyUI, SamplingComponent]
  val samplings = new HashMap[ISamplingProxyUI, SamplingComponent]
  var _factors = new HashSet[IFactorProxyUI]
  var _connections = new HashSet[(SamplingComponent, SamplingComponent)]
  val factorWidgets = new mutable.HashMap[IFactorProxyUI, SamplingConnectorWidget]
  var finalSampling: Option[ISamplingProxyUI] = None
  var editedDomainProxy: Option[IDomainDataUI] = None

  dataUI.factors.foreach {
    f ⇒ _factors += f
  }
  setBackground(new Color(77, 77, 77))
  val boxLayer = new LayerWidget(this)
  val connectLayer = new LayerWidget(this)
  addChild(boxLayer)
  addChild(connectLayer)

  setPreferredBounds(new Rectangle(0, 0, 400, 200))

  val transitionId = new AtomicInteger
  val connectProvider = new SamplingConnectionProvider
  val connectAction = ActionFactory.createExtendedConnectAction(null, connectLayer,
    new SamplingConnectionProvider,
    InputEvent.SHIFT_MASK)
  getActions.addAction(ActionFactory.createPopupMenuAction(new SamplingSceneMenuProvider(this)))
  val moveAction = ActionFactory.createMoveAction

  dataUI.domains.foreach {
    d ⇒ addDomain(d._1, d._2, false)
  }
  dataUI.samplings.foreach {
    s ⇒ addSampling(s._1, s._2, false)
  }

  dataUI.connections.foreach {
    c ⇒ connectProvider.createConnection(toSamplingComponent(c._1), toSamplingComponent(c._2))
  }

  dataUI.finalSampling match {
    case Some(fs: ISamplingProxyUI) ⇒ setFinalSampling(fs)
    case _ ⇒
  }

  def connections = _connections.toList

  def factors = _factors.toList

  def computeFactor(sourceProxy: ISamplingCompositionProxyUI,
                    targetProxy: ISamplingCompositionProxyUI) = factors.filter {
    f ⇒
      println("comute f :: " + f.dataUI.target.id + "  " + sourceProxy.id + " " + targetProxy.id)
      f.dataUI.target.id == targetProxy.id && f.dataUI.domain.id == sourceProxy.id
  }.headOption

  def peer = new MigPanel("") {
    peer.add(createView)
  }.peer

  def toSamplingComponent(scw: ISamplingCompositionProxyUI) =
    domainsAndSamplings.getOrElse(scw, throw new UserBadDataError("The graphical representation for the element " + scw.id + " does not exist"))

  def addDomain(domainProxy: IDomainProxyUI,
                location: Point,
                d: Boolean = true) = {
    val cw = new SamplingComponent(this, new DomainWidget(domainProxy, this), location) {
      getActions.addAction(ActionFactory.createPopupMenuAction(new DomainMenuProvider(samplingCompositionPanelUI)))
      getActions.addAction(connectAction)
      getActions.addAction(moveAction)
    }
    boxLayer.addChild(cw)
    validate
    domains += domainProxy -> cw
  }

  def addSampling(samplingProxy: ISamplingProxyUI,
                  location: Point,
                  display: Boolean = true) = {
    val cw = new SamplingComponent(this, new SamplingWidget(samplingProxy, this), location) {
      getActions.addAction(ActionFactory.createPopupMenuAction(new SamplingMenuProvider(samplingCompositionPanelUI)))
      getActions.addAction(connectAction)
      getActions.addAction(moveAction)
    }
    boxLayer.addChild(cw)
    validate
    samplings += samplingProxy -> cw
  }

  def remove(samplingComponent: ISamplingComponent) = {
    samplingComponent.component match {
      case (s: ISamplingCompositionWidget) ⇒
        domainsAndSamplings.isDefinedAt(s.proxy) match {
          case true ⇒
            boxLayer.removeChild(domainsAndSamplings(s.proxy))
            _connections = _connections.filterNot {
              case (ss, tt) ⇒ (ss == samplingComponent) || (tt == samplingComponent)
            }
            _factors = _factors.filterNot {
              f ⇒
                f.dataUI.domain.id == s.proxy.id ||
                  f.dataUI.target.id == s.proxy.id
            }
            samplingComponent.connections.foreach {
              c ⇒ connectLayer.removeChild(c)
            }
            s match {
              case (d: IDomainWidget) ⇒ domains -= d.proxy
              case (s: ISamplingWidget) ⇒ samplings -= s.proxy
              case _ ⇒
            }
          case _ ⇒
        }
      case _ ⇒
    }

    samplingComponent.connections.foreach {
      cw ⇒ connectLayer.removeChild(cw)
    }
    samplingComponent.connections.clear
    _connections.filter {
      c ⇒
        (c._1 == samplingComponent.component.proxy.id) || (c._2 == samplingComponent.component.proxy.id)
    }.foreach {
      e ⇒ _connections -= e
    }
    revalidate
    repaint
  }

  def scene = this

  def setFinalSampling(fsampling: ISamplingProxyUI) = {
    finalSampling match {
      case Some(sp: ISamplingProxyUI) ⇒ setSamplingProxy(sp, false)
      case _ ⇒
    }
    setSamplingProxy(fsampling, true)
    finalSampling = Some(fsampling)
    revalidate
    repaint
  }

  def setSamplingProxy(samplingProxy: ISamplingProxyUI,
                       b: Boolean): Unit = samplingProxy.isFinal = b

  def saveContent(name: String) = {
    new SamplingCompositionDataUI(name,
      domains.map {
        f ⇒ f._1 -> f._2.getPreferredLocation
      }.toList,
      samplings.map {
        s ⇒ s._1 -> s._2.getPreferredLocation
      }.toList,
      factors,
      _connections.toList.map {
        case ((a, b)) ⇒ (a.component.proxy, b.component.proxy)
      },
      finalSampling)
  }

  def domainsAndSamplings: Map[ISamplingCompositionProxyUI, SamplingComponent] = domains ++ samplings toMap

  def testConnections(arityTest: Boolean) = {
    _connections.foreach {
      case ((source, target)) ⇒
        if (!testConnection(source.component, target.component, arityTest)) {
          println("Not good from " + source + " to " + target)
        }

      //updateNext(source, target)
    }
  }

  def testConnection(sourceWidget: ISamplingCompositionWidget,
                     targetWidget: ISamplingCompositionWidget,
                     arityTest: Boolean = true): Boolean = {
    StatusBar.clear
    targetWidget match {
      case domainT: IDomainWidget ⇒
        sourceWidget match {
          case sw: ISamplingWidget ⇒ false
          case dw: IDomainWidget ⇒ {
            if (arityTest) {
              if (_connections.map {
                _._2.component.proxy
              }.contains(domainT.proxy)) {
                StatusBar.warn("Only one connection between Domains is allowed")
                false
              } else true
            } else true
          } &&
            domainT.proxy.dataUI.isAcceptable(dw.proxy.dataUI)
        }
      case samplingT: ISamplingWidget ⇒
        sourceWidget match {
          case sw: ISamplingWidget ⇒
            samplingT.proxy.dataUI.isAcceptable(sw.proxy.dataUI)
          case dw: IDomainWidget ⇒ samplingT.proxy.dataUI.isAcceptable(dw.proxy.dataUI)
          case _ ⇒ false
        }
      case _ ⇒ false
    }
  }

  def testConnection(sourceComponent: ISamplingComponent,
                     targetComponent: ISamplingComponent,
                     arityTest: Boolean): Boolean =
    testConnection(sourceComponent.component, targetComponent.component, arityTest)

  def firstSampling(proxy: ISamplingCompositionProxyUI) = {
    connections.filter {
      _._1.component.proxy.id == proxy.id
    }.map { _._2.component.proxy }.headOption match {
      case Some(next: IDomainProxyUI) ⇒ firstSampling(next)
      case Some(x: ISamplingProxyUI) ⇒ x
      case _ ⇒ proxy
    }
  }

  def firstNoneModifierDomain(domain: IDomainDataUI): Option[IDomainDataUI] =
    domain match {
      case modifier: IDomainDataUI with IModifier ⇒
        modifier.previousDomain.headOption match {
          case Some(pD: IDomainDataUI) ⇒ firstNoneModifierDomain(pD)
          case _ ⇒ None
        }
      case d: IDomainDataUI ⇒ Some(d)
      case _ ⇒ None
    }

  def update(domain: IDomainWidget) = domain.proxy.dataUI match {
    case modifier: IDomainDataUI with IModifier ⇒
      updatePrevious(domain)
    case d: IDomainDataUI ⇒
      val (s, t) = updateNext(domain)
      s match {
        case w: IDomainWidget ⇒
          updatePrevious(w)
          t match {
            case Some(samp: ISamplingWidget) ⇒
              val factor = computeFactor(w.proxy, samp.proxy)
              factor match {
                case Some(f: IFactorProxyUI) ⇒
                  println("OH YEAH for " + Some(domain.proxy.dataUI) + " " + editedDomainProxy)
                  if (editedDomainProxy != Some(domain.proxy.dataUI)) f.dataUI.prototype = None
                  factorWidgets(f).update
                case _ ⇒
              }
            case _ ⇒
          }
      }
  }

  def updateNext(domain: IDomainWidget) = connections.filter {
    _._1.component.proxy.id == domain.proxy.id
  }.headOption match {
    case Some((_, next: ISamplingComponent)) ⇒ updateNext(domain, next.component)
    case _ ⇒ (domain, None)
  }

  def updateNext(source: ISamplingComponent,
                 target: ISamplingComponent) = updateNext(source.component, target.component)

  def updateNext(source: ISamplingCompositionWidget,
                 target: ISamplingCompositionWidget): (ISamplingCompositionWidget, Option[ISamplingCompositionWidget]) = {
    println("FWD update : from " + source.proxy.id + " to " + target.proxy.id)
    source.proxy match {
      case sp: IDomainProxyUI ⇒ target.proxy match {
        case tp: IDomainProxyUI ⇒
          tp.dataUI match {
            case modifier: IDomainDataUI with IModifier ⇒
              println("MODIFIER")
              tp.dataUI = modifier.clone(scala.collection.immutable.List(sp.dataUI))
              connections.filter {
                cc ⇒
                  cc._1.component.proxy.id == source.proxy.id
              }.headOption match {
                case Some((_, c: ISamplingComponent)) ⇒ updateNext(target, c.component)
                case _ ⇒ (source, Some(target))
              }
          }
        case _ ⇒ (source, Some(target))
      }
      case _ ⇒ (source, Some(target))
    }
  }

  def updatePrevious(domain: IDomainWidget) =
    connections.filter {
      _._2.component.proxy.id == domain.proxy.id
    }.headOption match {
      case Some((previousc: ISamplingComponent, _)) ⇒ previousc.component match {
        case previous: IDomainWidget ⇒ updatePrevious(previous, domain)
        case _ ⇒
      }
      case _ ⇒
    }

  def updatePrevious(source: IDomainWidget,
                     target: ISamplingCompositionWidget): Unit = {
    println("BACK update : from " + source.proxy.id + " to " + target.proxy.id)
    target.proxy match {
      case tp: IDomainProxyUI ⇒ tp.dataUI match {
        case modifier: IDomainDataUI with IModifier ⇒ source.proxy match {
          case sp: IDomainProxyUI ⇒
            tp.dataUI = modifier.clone(previousDomain = scala.collection.immutable.List(sp.dataUI))
            source.incomings.foreach {
              i ⇒ updatePrevious(i, source)
            }
          case _ ⇒
        }
        case _ ⇒
      }
      case _ ⇒
    }
  }

  class SamplingConnectionProvider extends ConnectProvider {

    override def isSourceWidget(sourceWidget: Widget): Boolean =
      sourceWidget match {
        case x: SamplingComponent ⇒ true
        case _ ⇒ false
      }

    def boolToConnector(b: Boolean) = {
      if (b) ConnectorState.ACCEPT
      else ConnectorState.REJECT_AND_STOP
    }

    override def isTargetWidget(sourceWidget: Widget,
                                targetWidget: Widget): ConnectorState =
      sourceWidget match {
        case s: ISamplingComponent ⇒ targetWidget match {
          case t: ISamplingComponent ⇒ t.component match {
            case dp: IDomainWidget ⇒
              if (t.component.proxy.id == s.component.proxy.id) ConnectorState.REJECT_AND_STOP
              else boolToConnector(samplingCompositionPanelUI.testConnection(s.component, dp, true))
            case sp: ISamplingWidget ⇒
              boolToConnector(samplingCompositionPanelUI.testConnection(s.component, sp, false))
            case _ ⇒ ConnectorState.REJECT_AND_STOP
          }
          case _ ⇒ ConnectorState.REJECT_AND_STOP
        }
      }

    override def hasCustomTargetWidgetResolver(scene: Scene): Boolean = false

    override def resolveTargetWidget(scene: Scene, sceneLocation: Point): Widget = null

    override def createConnection(sourceWidget: Widget, targetWidget: Widget) = {
      val sourceW = sourceWidget.asInstanceOf[SamplingComponent]
      val targetW = targetWidget.asInstanceOf[SamplingComponent]
      val factorProxyUI = computeFactor(sourceW.component.proxy, targetW.component.proxy) match {
        case Some(f: IFactorProxyUI) ⇒ Some(f)
        case _ ⇒
          sourceW.component match {
            case d: IDomainWidget ⇒
              updatePrevious(d, targetW.component)
              targetW.component match {
                case s: ISamplingWidget ⇒
                  val fp = new FactorProxyUI(new FactorDataUI(d.proxy, s.proxy))
                  _factors += fp
                  Some(fp)
                case _ ⇒ None
              }
            case _ ⇒
          }
      }

      val connection = new SamplingConnectorWidget(sourceW,
        targetW,
        samplingCompositionPanelUI)

      connection.setRouter(new MoleRouter(boxLayer))

      factorProxyUI match {
        case Some(f: IFactorProxyUI) ⇒ factorWidgets += f -> connection
        case _ ⇒
      }

      connectLayer.addChild(connection)
      sourceW.connections += connection
      targetW.connections += connection
      _connections += sourceW -> targetW

    }
  }

}

