package server

import akka.actor.{Actor, PoisonPill, Props}
import msg.{ClientRequest, ServerResponse, ServerWorkerResponse}
import akka.pattern.ask
import akka.util.Timeout
import app.Main.safePrintln
import slick.jdbc.SQLiteProfile

import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import scala.language.postfixOps


class ServerActor(db: SQLiteProfile.backend.DatabaseDef) extends Actor {
  implicit val timeout: Timeout = 1000 millis

  override def receive: Receive = {
    case request: ClientRequest =>
      val clientName = sender().path.name
      safePrintln(s"SERVER: request from $clientName for ${request.productName}")
      val session = db.createSession()
      val futureServerWorkerResponse = (context.actorOf(Props(classOf[ServerWorker], session)) ? request).mapTo[ServerWorkerResponse]
      val client = sender()
      futureServerWorkerResponse.onComplete {
        case Success(workerResponse) =>
          client ! ServerResponse(workerResponse.productName, workerResponse.price)
          sender() ! PoisonPill.getInstance
        case Failure(e) =>
          sender() ! PoisonPill.getInstance
          safePrintln(s"SERVER DID NOT GET RESPONSE FROM WORKER: request from $clientName for ${request.productName}")
      }

    case _ => safePrintln("SERVER: Got unknown message")
  }
}
