package pl.klawoj.stages

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.TestKit
import org.scalatest.WordSpecLike

class AccumulateUntilTest extends TestKit(ActorSystem("test")) with WordSpecLike {

  private implicit val materializer: ActorMaterializer = ActorMaterializer()

  "AccumulateUntil" must {
    "respect the condition when grouping and properly complete the stream" in {
      Source(1 to 100).via(AccumulateUntil(_.sum > 20)).take(4).runWith(TestSink.probe[Seq[Int]]).
        request(5).expectNext(
        Seq(1, 2, 3, 4, 5, 6),
        Seq(7, 8, 9),
        Seq(10, 11),
        Seq(12, 13)
      ).expectComplete()
    }
  }

}
