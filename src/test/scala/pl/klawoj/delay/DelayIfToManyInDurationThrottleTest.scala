package pl.klawoj.delay

import java.time.Duration

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.contrib.DelayFlow
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.TestKit
import org.scalatest.WordSpecLike

import scala.concurrent.duration.DurationDouble

class DelayIfToManyInDurationThrottleTest extends TestKit(ActorSystem("test")) with WordSpecLike {
  private implicit val materializer: ActorMaterializer = ActorMaterializer()

  "DelayFlow with DelayIfToManyInDuration strategy" must {
    "push forward elements immediately until desired count is reached, then it should wait for the next time window to continue" in {
      val probe = Source.cycle(() => Seq(1, 2, 3).iterator).via(
        new DelayFlow[Int](() => new DelayIfToManyInDuration[Int](elements = 10, Duration.ofSeconds(3)))).
        take(30).
        runWith(TestSink.probe[Int])

      probe.request(31)
      probe.expectNextN(10)
      probe.expectNoMessage(2 second)
      probe.expectNextN(10)
      probe.expectNoMessage(2 second)
      probe.expectNextN(10)
      probe.expectComplete()
    }
  }

}
