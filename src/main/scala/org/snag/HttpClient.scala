package org.snag

import akka.actor.ActorSystem
import akka.util.Timeout
import spray.client.pipelining._
import scala.concurrent.duration._

trait HttpClient {
  implicit protected def actorSystem: ActorSystem
  implicit private val timeout = Timeout(1 minute)

  implicit private lazy val ec = actorSystem.dispatcher

  private lazy val basePipeline = {
    import com.pragmasoft.reactive.throttling.threshold._
    import com.pragmasoft.reactive.throttling.http.client.HttpClientThrottling._
    sendReceive(throttleFrequency(1 perSecond))
  }

  private val verbose = false
  protected lazy val http =
    if ( verbose )
      logRequest(println(_)) ~> basePipeline ~> logResponse(println(_))
    else
      basePipeline
}
