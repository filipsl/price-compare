package db

import akka.actor.Actor
import app.Main.safePrintln
import msg.ClientRequest
import slick.jdbc.SQLiteProfile
import slick.jdbc.SQLiteProfile.api._

import scala.concurrent._
import ExecutionContext.Implicits.global


class DbAccessActor(db: SQLiteProfile.backend.DatabaseDef) extends Actor{
  override def receive: Receive = {
    case request: ClientRequest =>
      val action = sql"select ID, NAME, COUNTER from count_table WHERE NAME ==${request.productName}".as[(Int,String,Int)]
//      val result = db.withSession {
//        implicit session =>
//          query.list // <- takes session implicitly
//      }
      db.run(action.transactionally).map(q => println(q(1)))
      safePrintln("SUCCESSSSSSSSSS")
    case _ => safePrintln("DbAccessActor got wrong message")
  }
}
