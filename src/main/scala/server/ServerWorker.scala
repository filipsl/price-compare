package server

import akka.actor.{Actor, Kill, PoisonPill, Props}
import akka.pattern.ask
import akka.util.Timeout
import app.Main.safePrintln
import msg.{ClientRequest, PriceWorkerResponse, ServerWorkerResponse}

import scala.concurrent.duration._
import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

class ServerWorker extends Actor {
  implicit val timeout: Timeout = 300 millis

  override def receive: Receive = {
    case request: ClientRequest =>

      val futureWorkerResponse1 = (context.actorOf(Props[PriceWorker]) ? request).mapTo[PriceWorkerResponse]
      val futureWorkerResponse2 = (context.actorOf(Props[PriceWorker]) ? request).mapTo[PriceWorkerResponse]

      val combinedWorkerResponse = futureWorkerResponse1.zipWith(futureWorkerResponse2)((q1, q2) => Seq(q1.price, q2.price).min)
        .fallbackTo(futureWorkerResponse1.map(_.price))
        .fallbackTo(futureWorkerResponse2.map(_.price))

      val server = sender()

      futureWorkerResponse1.onComplete {
        case Success(price) =>
          sender() ! PoisonPill.getInstance
        case Failure(e) => e.printStackTrace
      }

      futureWorkerResponse2.onComplete {
        case Success(price) =>
          sender() ! PoisonPill.getInstance
        case Failure(e) => e.printStackTrace
      }

      combinedWorkerResponse.onComplete {
        case Success(price) =>
          safePrintln(s"Got the callback, value = $price")
          server ! ServerWorkerResponse(request.productName, price)
        case Failure(e) => e.printStackTrace
      }
    case _ =>
      safePrintln("ServerWorker got unknown message")
  }

}
