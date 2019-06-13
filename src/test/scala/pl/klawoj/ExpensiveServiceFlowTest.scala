package pl.klawoj

import java.time.Duration

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.TestKit
import org.scalatest.{GivenWhenThen, WordSpecLike}
import pl.klawoj.ExpensiveServiceFlow.RequestsContract

import scala.concurrent.duration.DurationLong

class ExpensiveServiceFlowTest extends TestKit(ActorSystem("test"))
  with WordSpecLike
  with GivenWhenThen
  with ExpensiveServiceFlowTestFixture {
  private implicit val materializer: ActorMaterializer = ActorMaterializer()


  "ExpensiveServiceFlow" must {
    "ack after using the service and provide task results, keeping frequency for priority in mind  " in {

      val tasks = taskInfiniteSourceWithIdBasedAndUniformlyDistributedPriorites

      val service = alwaysSuccessfulService

      val (ackProbe, resultsProbe) = new ExpensiveServiceFlow(
        tasks,
        TestSink.probe[Ack],
        TestSink.probe[TaskResult],
        service).graph.run()

      ackProbe.request(10)
      resultsProbe.request(10)

      val acked = ackProbe.expectNextN(10)
      val results = resultsProbe.expectNextN(10)

      assertAcksMatchesResults(acked, results)

      assertFrequenciesForPriorities(acked)

    }

    "slow down when required, but still complete all the requests" in {
      val tasks = Source.fromIterator(() => Iterator.from(0)).take(13).map(id => Task(id.toString, priorityFromId(id)))

      val service: Task => ExpensiveServiceResponse = rateLimitedService(Duration.ofSeconds(1))

      val (ackProbe, resultsProbe) = new ExpensiveServiceFlow(
        tasks,
        TestSink.probe[Ack],
        TestSink.probe[TaskResult],
        service).graph.run()

      resultsProbe.request(10)
      ackProbe.request(10)

      assertFrequenciesForPriorities(Stream.fill(10)(ackProbe.expectNext(20 seconds)))
    }


    "wait for the next time period when reaching the contract, but still complete all the requests" in {

      val contract = RequestsContract(10, Duration.ofSeconds(3))

      val tasks = Source.fromIterator(() => Iterator.from(0)).map(id => Task(id.toString, priorityFromId(id)))

      val service: Task => ExpensiveServiceResponse = alwaysSuccessfulService

      val (ackProbe, resultsProbe) = new ExpensiveServiceFlow(
        tasks,
        TestSink.probe[Ack],
        TestSink.probe[TaskResult],
        service,
        contract
      ).graph.run()

      ackProbe.request(31)
      resultsProbe.request(31)

      assertFrequenciesForPriorities(ackProbe.expectNextN(10))
      ackProbe.expectNoMessage(2 second)
      assertFrequenciesForPriorities(ackProbe.expectNextN(10))
      ackProbe.expectNoMessage(2 second)
      assertFrequenciesForPriorities(ackProbe.expectNextN(10))
    }
  }

}
