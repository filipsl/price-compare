package server

import akka.actor.{Actor, Props}
import msg.{ClientRequest, ServerResponse, ServerWorkerResponse}
import akka.pattern.ask
import akka.util.Timeout
import app.Main.safePrintln

import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success}


class ServerActor extends Actor {
  implicit val timeout: Timeout = 1000 millis

  override def receive: Receive = {
    case request: ClientRequest =>
      safePrintln("Server got product name: ".concat(request.productName))
      val futureServerWorkerResponse = (context.actorOf(Props[ServerWorker]) ? request).mapTo[ServerWorkerResponse]
      val client = sender()
      futureServerWorkerResponse.onComplete {
        case Success(workerResponse) =>
          safePrintln("SERVER GOT RESPONSE FROM WORKER")
          client ! ServerResponse(workerResponse.productName, workerResponse.price)
        case Failure(e) => safePrintln("SERVER DID NOT GET RESPONSE FROM WORKER")
      }

    case _ => safePrintln("Server got unknown message")
  }
}
