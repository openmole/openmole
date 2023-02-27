package org.openmole.gui.client.core

import scaladget.bootstrapnative.bsn.*
import scaladget.tools.*
import org.openmole.gui.client.ext.*

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import org.openmole.gui.shared.data.*
import com.raquo.laminar.api.L.*
import org.openmole.gui.shared.api.{AuthenticationPlugin, AuthenticationPluginFactory, GUIPlugins, ServerAPI}
import scaladget.bootstrapnative.Selector.Options
import scaladget.bootstrapnative.bsn


object Notification {
  enum NotificationLevel:
    case Info, Error
}

import Notification._

case class Notification(level: NotificationLevel, title: String, body: Div)

class NotificationManager:
  val showNotfications = Var(false)
  val stack: Var[Seq[Notification]] = Var(Seq())
  val currentListType: Var[Option[NotificationLevel]] = Var(None)

  def filteredStack(stack: Seq[Notification], notificationLevel: NotificationLevel) = stack.filter(_.level == notificationLevel)


  def addNotification(level: NotificationLevel, title: String, body: Div) = stack.update { s =>
    (s :+ Notification(level, title, div(body, cls := "notification")))
  }
  
  def addAndShowNotificaton(level: NotificationLevel, title: String, body: Div) =
    addNotification(level, title, body)
    showNotfications.set(true)

  def clearNotifications(level: NotificationLevel) = {
    stack.update(s => s.filterNot(_.level == level))
  }

  val notificationList =
    div(
      cls := "notifList",
      child <-- currentListType.signal.combineWith(stack.signal).map { case (level, stack) =>
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
                    backgroundColor := {
                      if (i % 2 == 0) "#ccc" else "#f4f4f4"
                    },
                    div(s.title, fontWeight.bold, padding := "10", cursor.pointer)
                  ).expandOnclick(s.body.amend(color := "white"))
                }
              )
          case _ => emptyNode
        }
      }
    )


  def notifTopIcon(notifCls: String, st: Seq[Notification], notificationLevel: NotificationLevel) =
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
      idAttr := "container",
      cls.toggle("alert-is-shown") <-- showNotfications.signal,
      div(display.flex, flexDirection.column,
        cls := "alert",
        div(display.flex, flexDirection.column,
          child <-- stack.signal.map { s =>
            div(display.flex, flexDirection.row,
              notifTopIcon("bi-x-square-fill errorNotification", s, NotificationLevel.Error),
              notifTopIcon("bi-info-square-fill infoNotification", s, NotificationLevel.Info).amend(marginLeft := "30")
            )
          }
        )
      )
    )