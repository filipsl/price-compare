package server

import akka.actor.Actor
import app.Main.safePrintln
import msg.{ClientRequest, PriceWorkerResponse}

import scala.util.Random

class PriceWorker extends Actor {
  override def receive: Receive = {
    case req: ClientRequest =>
      val msToSleep = Random.between(100, 500)
      Thread.sleep(msToSleep)

      val r = scala.util.Random
      val randomPrice = r.nextDouble() * 9 + 1
      safePrintln(s"Price worker delay $msToSleep ms, price $randomPrice: ${req.productName}")

      sender() ! PriceWorkerResponse(req.productName, randomPrice)
    case _ =>
      safePrintln("PriceWorker received unknown message")
  }
}
