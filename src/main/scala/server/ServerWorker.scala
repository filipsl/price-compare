package server

import akka.actor.{Actor, PoisonPill, Props}
import akka.pattern.ask
import akka.util.Timeout
import app.Main.safePrintln
import db.DbAccessActor
import msg.{ClientRequest, PriceWorkerResponse, ServerWorkerResponse}
import slick.jdbc.SQLiteProfile

import scala.concurrent.duration._
import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import scala.language.postfixOps


class ServerWorker(session: SQLiteProfile.backend.SessionDef) extends Actor {
  implicit val timeout: Timeout = 300 millis

  override def receive: Receive = {
    case request: ClientRequest =>

      val futureWorkerResponse1 = (context.actorOf(Props[PriceWorker]) ? request).mapTo[PriceWorkerResponse]
      val futureWorkerResponse2 = (context.actorOf(Props[PriceWorker]) ? request).mapTo[PriceWorkerResponse]
      (context.actorOf(Props(classOf[DbAccessActor], session.database)) ! request)


      val combinedWorkerResponse = futureWorkerResponse1.zipWith(futureWorkerResponse2)((response1, response2) => Seq(response1.price, response2.price).min)
        .fallbackTo(futureWorkerResponse1.map(_.price))
        .fallbackTo(futureWorkerResponse2.map(_.price))
        .fallbackTo(Future[Double] {
          -1.0
        })

      val server = sender()

      futureWorkerResponse1.onComplete {
        case Success(price) =>
          sender() ! PoisonPill.getInstance
        case Failure(e) =>
          sender() ! PoisonPill.getInstance
          safePrintln(s"No response from the first price worker within 300ms: ${request.productName}")
      }

      futureWorkerResponse2.onComplete {
        case Success(price) =>
          sender() ! PoisonPill.getInstance
        case Failure(e) =>
          sender() ! PoisonPill.getInstance
          safePrintln(s"No response from the second price worker within 300ms: ${request.productName}")
      }

      combinedWorkerResponse.onComplete {
        case Success(price) =>
          session.close()
          server ! ServerWorkerResponse(request.productName, price)
        case Failure(e) =>
          session.close()
          safePrintln(s"Error occurred within ServerWorker ${self.path.name}")
      }
    case _ =>
      safePrintln("ServerWorker got unknown message")
  }

}
