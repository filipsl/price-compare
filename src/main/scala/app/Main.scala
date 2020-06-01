package app

import slick.jdbc.SQLiteProfile.api._
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.util.Timeout
import client.ClientActor
import com.typesafe.config.{ConfigFactory, ConfigValueFactory}

import scala.language.postfixOps
import server.ServerActor

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Await
import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.duration._


object Main {

  var clients: ArrayBuffer[ActorRef] = ArrayBuffer[ActorRef]()

  def safePrintln(msg: String): Unit = {
    this.synchronized {
      println(msg)
    }
  }

  def runSimulation(): Unit = {
    val products = List("laptop", "tv", "dryer", "monitor", "keyboard", "mouse", "lamp", "phone", "charger", "case")
    for (i <- 0 to 9) clients(i) ! products(i)
  }

  def runSingleRequest(): Unit = {
    try {
      val clientId = scala.io.StdIn.readInt()
      val product = scala.io.StdIn.readLine()

      if (clientId >= 0 && clientId <= 9) {
        clients(clientId) ! product
      } else {
        safePrintln("Id must be in range 0 - 9")
      }
    } catch {
      case e: java.lang.NumberFormatException => safePrintln("Incorrect value")
    }
  }

  def main(args: Array[String]) {


    val db = Database.forURL("jdbc:sqlite:src/main/scala/db/request_count.db3",
      driver = "org.sqlite.JDBC",
      executor = AsyncExecutor("request_count", numThreads = 16, queueSize = 1000))

    //    val timeout: Duration = 300 millis
//    implicit val timeout: Timeout = 300 millis
//    val q = sql"select ID from count_table ct".as[Int]
//    val f1 = db.run(q)

//    Await.result(
//      db.run(q).map { res =>
//        // res is a Vector[String]
//        println(res)
//      }, timeout)

    val config = ConfigFactory.load()
      .withValue("akka.loglevel", ConfigValueFactory.fromAnyRef("OFF"))
      .withValue("akka.stdout-loglevel", ConfigValueFactory.fromAnyRef("OFF"))

    val system = ActorSystem("priceCompareSystem", config)
    safePrintln("Started price-compare system")

    val server = system.actorOf(Props(classOf[ServerActor], db), "server")
    for (i <- 0 to 9) clients += system.actorOf(Props(classOf[ClientActor], server), f"client$i")

    var runLoop = true
    while (runLoop) {
      val line = scala.io.StdIn.readLine()
      line match {
        case "run" => runSimulation()
        case "c" => runSingleRequest()
        case "quit" => system.terminate(); db.close(); runLoop = false;
        case _ => safePrintln("Unknown command. Available commands => run, c, quit")
      }
    }
  }
}

