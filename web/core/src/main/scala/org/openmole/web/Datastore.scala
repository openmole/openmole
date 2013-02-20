package org.openmole.web

import akka.actor.{ Props, ActorSystem, Actor }
import org.openmole.web.Datastore
import akka.pattern.ask
import scala.concurrent.duration._
import akka.util.Timeout
import concurrent.Await

class Datastore[T, U] extends Actor {
  var moleExecs = Map.empty[T, U]
  def receive = {
    case pair: (T, U) ⇒ moleExecs += pair
    case key: T ⇒ sender ! moleExecs(key)
  }
}

class DataHandler[T, U](val system: ActorSystem) {
  implicit val timeout = Timeout(5 seconds) // needed for `?` below
  val store = system.actorOf(Props[Datastore[T, U]])

  def add(key: T, data: U) {
    store ! (key, data)
  }

  def get(key: T) = Await.result((store ? key), Duration(1, SECONDS)).asInstanceOf[U]

}