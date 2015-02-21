package org.openmole.web.cache

import akka.actor.{ Props, ActorSystem, Actor }
import akka.pattern.ask
import scala.concurrent.duration._
import akka.util.Timeout
import concurrent.Await

class Datastore[T, U] extends Actor {
  var data = Map.empty[T, U]
  def receive = {
    case ("put", pair: (T, U)) ⇒ data += pair
    // asInstanceOf to remove warning "abstract type pattern is unchecked since it is eliminated by erasure"
    // as suggested in http://stackoverflow.com/a/18136667/470341
    case ("get", key)          ⇒ sender ! data.get(key.asInstanceOf[T])
    case ("remove", key)       ⇒ data -= (key.asInstanceOf[T])
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

  def remove(key: T) = store ! "remove" -> key

  def get(key: T): Option[U] = Await.result(store ? ("get" -> key), Duration(1, SECONDS)).asInstanceOf[Option[U]]

  def getKeys = Await.result(store ? "getKeys", Duration(1, SECONDS)).asInstanceOf[Iterable[T]]

}