package pl.klawoj.stages

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.TestKit
import org.scalatest.WordSpecLike

class ForceFrequencyTest extends TestKit(ActorSystem("test")) with WordSpecLike {
  private implicit val materializer: ActorMaterializer = ActorMaterializer()

  "ForceFrequency" must {
    "ignore some of the tasks to achieve desired frequency for each of the priorities " in {
      Source.cycle(() => Seq(1, 2, 3).iterator).via(ForceFrequency(Seq(5, 3, 2), x => if (x <= 3) x - 1 else 2)).
        take(10).
        runWith(TestSink.probe[Int]).
        request(11).expectNextUnordered(1, 1, 1, 1, 1, 2, 2, 2, 3, 3).
        expectComplete()

    }
  }

}
