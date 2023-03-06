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


object NotificationContent:
  enum NotificationLevel:
    case Info, Error


import NotificationContent._

case class NotificationContent(level: NotificationLevel, title: String, body: Div, serverId: Option[Long] = None)

class NotificationManager:

  val showNotfications = Var(false)
  val notifications: Var[Seq[NotificationContent]] = Var(Seq())
  val currentListType: Var[Option[NotificationLevel]] = Var(None)

  def filteredStack(stack: Seq[NotificationContent], notificationLevel: NotificationLevel) = stack.filter(_.level == notificationLevel)


  def addServerNotifications(events: Seq[NotificationEvent]) = notifications.update { s =>
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
                case None => (s"${e.script.name} completed", s"""Execution of ${e.script.path.mkString("/")} was completed at ${e.date}""")
                case Some(t) => (s"${e.script.name} failed", s"""Execution of ${e.script.path.mkString("/")} failed ${ErrorData.stackTrace(t)} at ${e.date}""")

            NotificationContent(NotificationLevel.Info, title, div(body, cls := "notification"), serverId = Some(NotificationEvent.id(event)))

    newEvents ++ s
  }

  def addNotification(level: NotificationLevel, title: String, body: Div, serverId: Option[Long] = None) = notifications.update { s =>
    s :+ NotificationContent(level, title, div(body, cls := "notification"))
  }

  def clearNotifications(level: NotificationLevel)(using api: ServerAPI, basePath: BasePath) =
    notifications.update {
      s =>
        val (cleared, kept) = s.partition(_.level == level)
        val serverClear = cleared.flatMap(_.serverId)
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


  def notifTopIcon(notifCls: String, st: Seq[NotificationContent], notificationLevel: NotificationLevel) =
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
      notifications.toObservable --> Observer[Seq[NotificationContent]] { n => showNotfications.set(!n.isEmpty) },
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