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


enum NotificationLevel:
  case Info, Error

import Notification._

case class Notification(level: NotificationLevel, title: String, body: Div, id: String = DataUtils.uuID)

class NotificationManager:
  val showNotfications = Var(false)
  val stack: Var[Seq[Notification]] = Var(Seq())
  val currentListType: Var[Option[NotificationLevel]] = Var(None)
  val currentID: Var[Option[String]] = Var(None)

  def filteredStack(stack: Seq[Notification], notificationLevel: NotificationLevel) = stack.filter(_.level == notificationLevel)

  def remove(notification: Notification) = stack.update(s => s.filterNot(_.id == notification.id))

  def addNotification(level: NotificationLevel, title: String, body: Div) =
    val notif = Notification(level, title, div(body, cls := "notification"))
    stack.update { s =>
      (s :+ notif)
    }
    notif

  def addAndShowNotificaton(level: NotificationLevel, title: String, body: Div = div()) =
    val last = addNotification(level, title, body)
    showNotification(last)

  def showNotification(notification: Notification) =
    showNotfications.set(true)
    currentListType.set(Some(notification.level))
    currentID.set(Some(notification.id))

  def hideNotificationManager =
    currentID.set(None)
    currentListType.set(None)
    showNotfications.set(false)

  def showGetItNotification(level: NotificationLevel, title: String, body: Div = div()) =
    lazy val notif: Notification =
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
                    div(backgroundColor := "white",
                      div(s.title,
                        fontWeight.bold, padding := "10", cursor.pointer, fontWeight.bold, borderLeft := "15px solid #dc3545",
                        backgroundColor := {
                          if (i % 2 == 0) "#ccc" else "#f4f4f4"
                        }
                      ),
                      onClick --> { _ =>
                        currentID.update(_ match {
                          case Some(i) if i == s.id => None
                          case None => Some(s.id)
                        }
                        )
                      },
                      currentID.signal.map { i => i == Some(s.id) }.expand(s.body.amend(color := "white"))
                    )
                  )
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