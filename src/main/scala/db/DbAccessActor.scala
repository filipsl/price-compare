package db

import akka.actor.Actor
import app.Main.safePrintln
import msg.{ClientRequest, DbAccessActorResponse}
import slick.jdbc.SQLiteProfile
import slick.jdbc.SQLiteProfile.api._

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.language.postfixOps
import scala.util.{Failure, Success}


class DbAccessActor(session: SQLiteProfile.backend.SessionDef) extends Actor {
  val db = session.database

  override def receive: Receive = {
    case request: ClientRequest =>
      val action = sql"select ID, NAME, COUNTER from count_table WHERE NAME ==${request.productName}".as[(Int, String, Int)]
      val result = db.run(action.transactionally).map(q => q(0) _3)
      val worker = sender()

      result.onComplete {
        case Success(counter) =>
          if (counter.getClass.getName.equals("int")) {
            worker ! DbAccessActorResponse(request.productName, counter)
          } else {
            worker ! DbAccessActorResponse(request.productName, 0)
          }
        case Failure(e) =>
          safePrintln("Could not get data from database")
          safePrintln(0.toString)
          worker ! DbAccessActorResponse(request.productName, 0)
      }

      val actionUpdate = sql"INSERT INTO count_table (NAME, COUNTER) VALUES(${request.productName}, 1) ON CONFLICT(NAME) DO UPDATE SET COUNTER=COUNTER + 1".asUpdate
      val resultUpdate = db.run(actionUpdate.transactionally)
      resultUpdate.onComplete {
        case Success(v) =>
          session.close()
        case Failure(e) =>
          safePrintln("Could not update data in database")
          session.close()
      }

    case _ => safePrintln("DbAccessActor got wrong message")
  }
}
