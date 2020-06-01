package db

import akka.actor.Actor
import slick.jdbc.SQLiteProfile

class DbAccessActor(db: SQLiteProfile.backend.DatabaseDef) extends Actor{
  override def receive: Receive = ???
}
