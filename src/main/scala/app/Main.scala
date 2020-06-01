package app

import slick.jdbc.SQLiteProfile.api._
import akka.actor.{ActorRef, ActorSystem, Props}
import client.ClientActor
import com.typesafe.config.{ConfigFactory, ConfigValueFactory}

import scala.language.postfixOps
import server.ServerActor

import scala.collection.mutable.ArrayBuffer
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.pattern.ask
import akka.util.Timeout
import msg.{ClientRequest, ServerResponse}

import scala.language.postfixOps
import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success}


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
      executor = AsyncExecutor("request_count", numThreads = 16, queueSize = 1000)
    )

    val config = ConfigFactory.load()
      .withValue("akka.loglevel", ConfigValueFactory.fromAnyRef("OFF"))
      .withValue("akka.stdout-loglevel", ConfigValueFactory.fromAnyRef("OFF"))

    implicit val system = ActorSystem("priceCompareSystem", config)

    safePrintln("Started price-compare system")

    val server = system.actorOf(Props(classOf[ServerActor], db), "server")

    val route =
      concat(
        pathPrefix("price" / Segment) { name =>
          get {
            implicit val timeout: Timeout = 1000 millis
            val result = (server ? ClientRequest(name)).mapTo[ServerResponse]
            onComplete(result) {
              case Success(response) =>
                val productName = response.productName
                val price = response.price
                val counter = response.counter
                var responseStr = ""
                if (price > 0) {
                  if (counter >= 0) {
                    responseStr = f"RESPONSE: product: $productName%s, price: $price%f, request count: $counter%d"
                  } else {
                    responseStr = f"RESPONSE: product: $productName%s, price: $price%f"
                  }
                } else {
                  responseStr = f"RESPONSE: product: $productName%s, NO PRICE"
                }
                complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, s"<p>$responseStr</p>"))
              case Failure(e) => complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, s"<h1>Error occured</h1>"))
            }
          }
        },
        pathPrefix("review" / Segment) { name =>
          get {
            val review = getReview(name)
            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, s"<h1>Say hello2 to akka-http$name</h1>"))
          }
        })


    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)


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

