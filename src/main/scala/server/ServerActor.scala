package server

import akka.actor.Actor
import msg.ClientRequest

class ServerActor extends Actor {
  override def receive: Receive = {
    case clientRequest: ClientRequest => println("Server got product name: ".concat(clientRequest.productName))
    case _ => println("Server got unknown message")
  }
}
