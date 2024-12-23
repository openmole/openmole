package org.openmole.gui.client.core

import scaladget.bootstrapnative.bsn.*
import scaladget.tools.*
import org.openmole.gui.client.ext.*

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import org.openmole.gui.shared.data.*
import com.raquo.laminar.api.L.*
import org.openmole.gui.client.core.NotificationManager.{Alternative, notificationContent}
import org.openmole.gui.shared.api.*
import org.openmole.gui.shared.data.NotificationEvent.id
import scaladget.bootstrapnative.Selector.Options
import scaladget.bootstrapnative.bsn
import com.raquo.laminar.api.features.unitArrows
import java.text.SimpleDateFormat

object NotificationManager:

  case class Alternative(name: String, action: String => Unit = _ => {})

  object Alternative:
    def cancel(using panels: Panels) = Alternative("cancel", panels.notifications.removeById)

  case class NotificationLine(time: Long, level: NotificationLevel, title: String, body: HtmlElement, id: String = randomId, serverId: Option[Long] = None)

  def notificationContent(notification: NotificationLine) = (notification.level, notification.title)

  def toService(manager: NotificationManager) =
    new NotificationService:
      override def notify(level: NotificationLevel, title: String, body: HtmlElement, time: Option[Long]): Unit = manager.addNotification(level, title, _=> body, time)

class NotificationManager:

  import NotificationManager.NotificationLine

  val showNotfications = Var(false)
  val notifications: Var[Seq[NotificationLine]] = Var(Seq())
  val currentListType: Var[Option[NotificationLevel]] = Var(None)
  val currentID: Var[Option[String]] = Var(None)

  def isOpened = currentListType.now().isDefined

  def filteredStack(stack: Seq[NotificationLine], notificationLevel: NotificationLevel) = stack.filter(_.level == notificationLevel)

  def remove(notification: NotificationLine) = removeById(notification.id)

  def removeById(id: String) = notifications.update(s => s.filterNot(_.id == id))

  def addNotification(level: NotificationLevel, title: String, body: String => HtmlElement, time: Option[Long] = None) =
    val id = randomId
    val notif = NotificationLine(time.getOrElse(System.currentTimeMillis()), level, title, div(body(id), cls := "notification"), id)
    notifications.update: s =>
      val cs = s.map { nl => notificationContent(nl) }
      if cs.count(_ == notificationContent(notif)) < 1
      then s :+ notif
      else s
    notif

  def addAndShowNotificaton(level: NotificationLevel, title: String, body: HtmlElement = div(), time: Option[Long] = None) =
    val last = addNotification(level, title, _ => body, time)
    showNotification(last)
    last

  def showNotification(notification: NotificationLine) =
    currentListType.set(Some(notification.level))
    currentID.set(Some(notification.id))

  def hideNotificationManager =
    currentID.set(None)
    currentListType.set(None)

  def showGetItNotification(level: NotificationLevel, title: String, body: HtmlElement = div(), time: Option[Long] = None) =
    lazy val notif: NotificationLine =
      addNotification(
        level,
        title,
        _ =>
          div(
            body.amend(cls := "getItNotification",
              button(btn_primary, "Get it",
                marginTop := "20",
                onClick --> { _ =>
                  remove(notif)
                  hideNotificationManager
                }
              )
            )
          ),
        time
      )
    showNotification(notif)

  def showAlternativeNotification(
                                   level: NotificationLevel,
                                   title: String,
                                   body: Div = div(),
                                   alt1: Alternative = Alternative("OK"),
                                   alt2: Alternative = Alternative("Cancel")) =
    lazy val notif: NotificationLine =
      addNotification(
        level,
        title,
        id =>
          div(
            body.amend(cls := "getItNotification"),
            buttonGroup.amend(
              margin := "15", float.right,
              button(btn_primary, alt1.name, onClick --> { _ => alt1.action(id); removeById(id) }),
              button(btn_secondary_outline, alt2.name, onClick --> { _ => alt2.action(id); removeById(id) })
            )
          )
      )
    showNotification(notif)
    notif

  def addServerNotifications(events: Seq[NotificationEvent]) =
    notifications.update: s =>
      val currentIds = s.flatMap(_.serverId).toSet

      val newEvents =
        for
          event <- events
          if !currentIds.contains(NotificationEvent.id(event))
        yield
          event match
            case e: NotificationEvent.MoleExecutionFinished =>
              val (title, body) =
                e.error match
                  case None => (s"${e.script.name} completed", s"""Execution of ${e.script.path.mkString} was completed at ${CoreUtils.longTimeToString(e.time)}""")
                  case Some(t) => (s"${e.script.name} failed", s"""Execution of ${e.script.path.mkString} failed ${ErrorData.stackTrace(t)} at ${CoreUtils.longTimeToString(e.time)}""")

              NotificationLine(e.time, NotificationLevel.Info, title, org.openmole.gui.client.ext.ClientUtil.errorTextArea(body), randomId, serverId = Some(NotificationEvent.id(event)))

      newEvents ++ s

  def clearNotifications(level: NotificationLevel)(using api: ServerAPI, basePath: BasePath) =
    notifications.update: s =>
      val (cleared, kept) = s.partition(_.level == level)
      val serverClear = cleared.flatMap(_.serverId)
      if !serverClear.isEmpty then api.clearNotification(serverClear)
      kept
    hideNotificationManager 


  case class ListColor(background: String, border: String)

  def listColor(level: NotificationLevel) =
    level match
      case NotificationLevel.Error => "#d35f5f"
      case _ => "#3086b5"

  def notificationList(using api: ServerAPI, basePath: BasePath) =
    div(
      child <-- (currentListType.signal combineWith notifications.signal).map: (level, stack) =>
        level match
          case Some(n: NotificationLevel) =>
            val fStack = filteredStack(stack, n)
            val lColor = listColor(n)
            if fStack.isEmpty
            then emptyNode
            else
              div(cls := "notifList",
                button(cls := "btn btn-purple", "Clear",
                  onClick --> clearNotifications(fStack.head.level),
                  marginBottom := "15px",
                ),
                fStack.sortBy(_.time).reverse.zipWithIndex.map: (s, i) =>
                  div(
                    div(
                      marginRight := "10px",
                      div(
                        overflowWrap.breakWord, overflow.hidden, width := "100%", padding := "10px", overflowY.auto,
                        s"${CoreUtils.longTimeToString(s.time)} - ${s.title}",
                        cursor.pointer, fontWeight.bold, borderLeft := s"15px solid ${lColor}",
                        if (i > 0) borderTop := "1px solid white" else emptyMod,
                        backgroundColor := lColor,
                        color := "white",
                        onClick -->
                          currentID.update:
                            case Some(i) if i == s.id => None
                            case _ => Some(s.id)
                      ),
                      currentID.signal.map {
                        _.contains(s.id)
                      }.expand {
                        s.body
                      }.amend(borderLeft := s"15px solid ${lColor}")
                    )
                  )
              )
          case _ => emptyNode
    )


  def notifTopIcon(notifCls: String, st: Seq[NotificationLine], notificationLevel: NotificationLevel) =
    val nList = filteredStack(st, notificationLevel)

    def trigger = onClick -->
      currentListType.update:
        case Some(aLevel: NotificationLevel) =>
          if aLevel == notificationLevel
          then None
          else Some(notificationLevel)
        case _ => Some(notificationLevel)

    div(
      display.flex, flexDirection.row, marginTop := "20px",
      div(cls := "notificationBackground",
        cls.toggle("notificationSelected") <-- currentListType.signal.map {
          _ == Some(notificationLevel)
        }, i(cls := notifCls), trigger),
      div(cls := "badgeNotification", div(nList.size, cls := "notificationCount"))
    )

  def render =
    div(flexRow, alignItems.flexEnd,
      notifications.toObservable --> Observer[Seq[NotificationLine]] { n => showNotfications.set(!n.isEmpty) },
      idAttr := "container",
      cls.toggle("alert-is-shown") <-- showNotfications.signal,
      div(display.flex, flexDirection.column,
        cls := "alert",
        div(display.flex, flexDirection.column,
          child <-- notifications.signal.map { s =>
            div(display.flex, flexDirection.row,
              notifTopIcon("bi-x-square-fill errorNotification", s, NotificationLevel.Error),
              notifTopIcon("bi-info-square-fill infoNotification", s, NotificationLevel.Info).amend(marginLeft := "30")
            )
          }
        )
      )
    )