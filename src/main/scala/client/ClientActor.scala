package client

import akka.actor.{Actor, ActorRef}
import app.Main.safePrintln
import msg.{ClientRequest, ServerResponse}


class ClientActor(server: ActorRef) extends Actor {

  override def receive: Receive = {
    case productName: String =>
      val name = self.path.name
      safePrintln(f"REQUEST:$name%s asked for: ".concat(productName))
      requestMinPrice(productName)
    case response: ServerResponse =>
      val name = self.path.name
      val productName = response.productName
      val price = response.price
      if(price > 0){
        safePrintln(f"RESPONSE:$name  product: $productName%s, price: $price%f")
      }else{
        safePrintln(f"RESPONSE:$name  product: $productName%s, NO PRICE")
      }
    case _ =>
      safePrintln("Unknown message")
  }

  def requestMinPrice(productName: String): Unit = {
    server ! ClientRequest(productName)
  }
}
