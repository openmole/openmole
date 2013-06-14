package org.openmole.web

import akka.actor.{ Props, ActorSystem, Actor }
import akka.pattern.ask
import scala.concurrent.duration._
import akka.util.Timeout
import concurrent.Await

class Datastore[T, U] extends Actor {
  var data = Map.empty[T, U]
  def receive = {
    case ("put", pair: (T, U)) ⇒ data += pair
    case ("get", key: T)       ⇒ sender ! data.get(key)
    case "getKeys"             ⇒ sender ! data.keys
  }
}

class DataHandler[T, U](val system: ActorSystem) {
  implicit val timeout = Timeout(5 seconds) // needed for `?` below
  val store = system.actorOf(Props[Datastore[T, U]])

  def add(key: T, data: U) {
    store ! ("put" -> (key -> data))
    println("stored " + key + " " + data)
  }

  def get(key: T): Option[U] = Await.result(store ? ("get" -> key), Duration(1, SECONDS)).asInstanceOf[Option[U]]

  def getKeys = Await.result(store ? "getKeys", Duration(1, SECONDS)).asInstanceOf[Iterable[T]]

}