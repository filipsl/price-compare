package client

import akka.actor.{Actor, ActorRef}
import akka.util.Timeout
import msg.ClientRequest

import scala.concurrent.duration._

class ClientActor(server: ActorRef) extends Actor {

  override def receive: Receive = {
    case productName: String =>
      println("Client got message ".concat(productName))
      getMinPrice(productName)
    case _ =>
      println("Unknown message")
  }

  def getMinPrice(productName: String): Unit = {
    server ! ClientRequest(productName)
  }
}
