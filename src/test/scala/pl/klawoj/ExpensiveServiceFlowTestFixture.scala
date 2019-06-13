package pl.klawoj

import java.time.{Duration, Instant}

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.scaladsl.Source
import pl.klawoj.HttpErrorCodes.TOO_MANY_REQUESTS

import scala.collection.immutable

trait ExpensiveServiceFlowTestFixture {

  val system: ActorSystem

  val log = Logging(system, this)

  val priorityFromId: Int => Int = id => id % 3 + 1

  val alwaysSuccessfulService: Task => Ok = t => Ok(TaskResult(t.definition, t.id))

  def assertAcksMatchesResults(acked: immutable.Seq[Ack], results: immutable.Seq[TaskResult]) = {
    assert(acked.zip(results).forall { case (ack, result) => ack.id == result.task.id })
  }

  def assertFrequenciesForPriorities(acked: immutable.Seq[Ack]) = {
    assert(acked.map(ack => priorityFromId(ack.id.toInt)).sorted == Seq(1, 1, 1, 1, 1, 2, 2, 2, 3, 3))
  }

  def taskInfiniteSourceWithIdBasedAndUniformlyDistributedPriorites: Source[Task, NotUsed] = {
    Source.fromIterator(() => Iterator.from(0)).map(id => Task(id.toString, priorityFromId(id)))
  }

  def rateLimitedService(minIntervalBetweenMessages: Duration): Task => ExpensiveServiceResponse = new (Task => ExpensiveServiceResponse) {

    private var lastRequest: Option[Instant] = None
    private def millisSinceLast: Long = lastRequest.map(Duration.between(_, Instant.now())).map(_.toMillis).getOrElse(0)

    override def apply(t: Task): ExpensiveServiceResponse = {
      val instant = Instant.now()

      def requestTooSoon(interval: Duration) = lastRequest.exists(_.isAfter(instant.minus(interval)))

      if (requestTooSoon(minIntervalBetweenMessages)) {
        log.info(s"Task TOO SOON! $t received $millisSinceLast millis after last one")

        lastRequest = Some(instant)

        Error(TOO_MANY_REQUESTS)
      } else {
        log.info(s"Task OK: $t received $millisSinceLast millis after last")

        lastRequest = Some(instant)

        Ok(TaskResult(t.definition, t.id))
      }
    }
  }
}
