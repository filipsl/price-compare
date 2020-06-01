package client

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, HttpResponse}
import akka.http.scaladsl.server.Directives.{complete, concat, get, onComplete, pathPrefix}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import msg.{ClientRequest, ServerResponse}
import akka.pattern.ask
import app.Main.safePrintln

import scala.language.postfixOps
import scala.util.{Failure, Success}
import scala.concurrent.duration._
import scala.concurrent._
import ExecutionContext.Implicits.global

class HttpClientActor(systemOfActors: ActorSystem, server: ActorRef) {
  implicit val system: ActorSystem = systemOfActors
  implicit val materializer: ActorMaterializer = ActorMaterializer()


  def runHttpClient(): Unit = {
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
            val responseFuture: Future[HttpResponse] = Http.get(system).singleRequest(HttpRequest(uri = f"https://www.opineo.pl/?szukaj=$name&s=2"))
            val duration = 5000 millis
            val result = Await.result(responseFuture, duration)
            var strr = "test"
            result.entity.toStrict(10, materializer).thenApply(entity => entity.getData.utf8String).thenAccept(body =>
              safePrintln(body.toString)
            )
            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, s"<h1>Product: $name</h1>  <p>Review: test</p>"))
          }
        })

    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)
  }
}
