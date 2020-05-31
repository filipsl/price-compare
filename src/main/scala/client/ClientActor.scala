package client

import akka.actor.{Actor, ActorRef}
import msg.{ClientRequest, ServerResponse}


class ClientActor(server: ActorRef) extends Actor {

  override def receive: Receive = {
    case productName: String =>
      val name = self.path.name
      println(f"$name%s asked for: ".concat(productName))
      printMinPrice(productName)
    case response: ServerResponse =>
      val productName = response.productName
      val price = response.price
      println(f"Product: $productName%s, price: $price%f")
    case _ =>
      println("Unknown message")
  }

  def printMinPrice(productName: String): Unit = {
    server ! ClientRequest(productName)
  }
}
