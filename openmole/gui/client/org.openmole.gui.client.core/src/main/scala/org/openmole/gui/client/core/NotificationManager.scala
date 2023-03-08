package org.openmole.gui.client.core

import scaladget.bootstrapnative.bsn.*
import scaladget.tools.*
import org.openmole.gui.client.ext.*

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import org.openmole.gui.shared.data.*
import com.raquo.laminar.api.L.*
import org.openmole.gui.shared.api.*
import org.openmole.gui.shared.data.NotificationEvent.id
import scaladget.bootstrapnative.Selector.Options
import scaladget.bootstrapnative.bsn

import java.text.SimpleDateFormat

<<<<<<< Updated upstream
=======
enum NotificationLevel:
  case Info, Error

case class Alternative(name: String, action: () => Unit = () => {})

object Alternative {
  def cancel(notification: Notification)(using panels: Panels) = Alternative("cancel", ()=> panels.notifications.remove(notification))
}

>>>>>>> Stashed changes
//case class Notification(level: NotificationLevel, title: String, body: Div, id: String = DataUtils.uuID)
//import NotificationContent._
object NotificationManager:
  case class NotificationLine(level: NotificationLevel, title: String, body: Div, id: Option[Long] = None)

  def toService(manager: NotificationManager) =
    new NotificationService:
      override def notify(level: NotificationLevel, title: String, body: Div): Unit = manager.addAndShowNotificaton(level, title, body)

class NotificationManager:
  import NotificationManager.NotificationLine

  val showNotfications = Var(false)
  val notifications: Var[Seq[NotificationLine]] = Var(Seq())
  val currentListType: Var[Option[NotificationLevel]] = Var(None)
  val currentID: Var[Option[Long]] = Var(None)

  def filteredStack(stack: Seq[NotificationLine], notificationLevel: NotificationLevel) = stack.filter(_.level == notificationLevel)

  def remove(notification: NotificationLine) = notifications.update(s => s.filterNot(_.id == notification.id))

  def addNotification(level: NotificationLevel, title: String, body: Div) =
    val notif = NotificationLine(level, title, div(body, cls := "notification"))
    notifications.update { s =>
      (s :+ notif)
    }
    notif

  def addAndShowNotificaton(level: NotificationLevel, title: String, body: Div = div()) =
    val last = addNotification(level, title, body)
    showNotification(last)

  def showNotification(notification: NotificationLine) =
    currentListType.set(Some(notification.level))
    currentID.set(notification.id)

  def hideNotificationManager =
    currentID.set(None)
    currentListType.set(None)

  def showGetItNotification(level: NotificationLevel, title: String, body: Div = div()) =
    lazy val notif: NotificationLine =
      addNotification(level, title,
        div(
          body.amend(cls := "getItNotification"),
          button(btn_primary, "Get it",
            margin := "15", float.right,
            onClick --> { _ =>
              remove(notif)
              hideNotificationManager
            })
        )
      )
    showNotification(notif)

  def showAlternativeNotification(
       level: NotificationLevel,
       title: String,
       body: Div = div(),
       alt1: Alternative = Alternative("OK"),
       alt2: Alternative = Alternative("Cancel")
       ) =
    lazy val notif: Notification =
      addNotification(level, title,
        div(
          body.amend(cls := "getItNotification"),
          buttonGroup.amend(
            button(btn_primary, alt1.name,
              margin := "15", float.right,
              onClick --> {_=> alt1.action()}
            ),
            button(btn_secondary_outline, alt2.name,
              margin := "15", float.right,
              onClick --> {_=> alt2.action()}
            )
          )
        )
      )
    showNotification(notif)
    notif

  def addServerNotifications(events: Seq[NotificationEvent]) = notifications.update { s =>
    val currentIds = s.flatMap(_.id).toSet

    val newEvents =
      for
        event <- events
        if !currentIds.contains(NotificationEvent.id(event))
      yield
        event match
          case e: NotificationEvent.MoleExecutionFinished =>
            val (title, body) =
              e.error match
                case None => (s"${e.script.name} completed", s"""Execution of ${e.script.path.mkString("/")} was completed at ${e.date}""")
                case Some(t) => (s"${e.script.name} failed", s"""Execution of ${e.script.path.mkString("/")} failed ${ErrorData.stackTrace(t)} at ${e.date}""")

            NotificationLine(NotificationLevel.Info, title, div(body, cls := "notification"), id = Some(NotificationEvent.id(event)))

    newEvents ++ s
  }

  def addNotification(level: NotificationLevel, title: String, body: Div, serverId: Option[Long] = None) = notifications.update { s =>
    s :+ NotificationLine(level, title, div(body, cls := "notification"))
  }

  def clearNotifications(level: NotificationLevel)(using api: ServerAPI, basePath: BasePath) =
    notifications.update {
      s =>
        val (cleared, kept) = s.partition(_.level == level)
        val serverClear = cleared.flatMap(_.id)
        if !serverClear.isEmpty then api.clearNotification(serverClear)
        kept
    }

  def notificationList(using api: ServerAPI, basePath: BasePath) =
    div(
      cls := "notifList",
      child <-- (currentListType.signal combineWith notifications.signal).map { case (level, stack) =>
        level match {
          case Some(n: NotificationLevel) =>
            val fStack = filteredStack(stack, n)
            if fStack.isEmpty
            then emptyNode
            else
              div(padding := "15",
                button(cls := "btn btn-purple", "Clear",
                  onClick --> { _ => clearNotifications(fStack.head.level)
                  }, marginBottom := "15px",
                ),
                fStack.zipWithIndex.map { case (s, i) =>
                  div(
                    div(backgroundColor := "white",
                      div(s.title,
                        fontWeight.bold, padding := "10", cursor.pointer, fontWeight.bold, borderLeft := "15px solid #dc3545",
                        backgroundColor := {
                          if (i % 2 == 0) "#ccc" else "#f4f4f4"
                        }
                      ),
                      onClick --> { _ =>
                        currentID.update(_ match {
                          case Some(i) if Some(i) == s.id => None
                          case None => s.id
                        }
                        )
                      },
                      currentID.signal.map { i => i == s.id }.expand(s.body.amend(color := "white"))
                    )
                  )
                }
              )
          case _ => emptyNode
        }
      }
    )


  def notifTopIcon(notifCls: String, st: Seq[NotificationLine], notificationLevel: NotificationLevel) =
    val nList = filteredStack(st, notificationLevel)

    def trigger = onClick --> { _ =>
      currentListType.update(_ match {
        case Some(aLevel: NotificationLevel) =>
          if aLevel == notificationLevel
          then None
          else Some(notificationLevel)
        case _ => Some(notificationLevel)
      }
      )
    }

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