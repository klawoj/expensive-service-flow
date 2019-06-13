package pl.klawoj.stages

import akka.NotUsed
import akka.stream.scaladsl.{Flow, GraphDSL}
import akka.stream.{FlowShape, Graph}

object ForceFrequency {

  def apply[T](frequencies: Seq[Int], typeIdxMapping: T => Int): Flow[T, T, NotUsed] = {
    Flow.fromGraph(graph(frequencies, typeIdxMapping))
  }

  private def graph[T](frequencies: Seq[Int], typeIdxMapping: T => Int): Graph[FlowShape[T, T], NotUsed] = {
    GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      def containsRequiredElements: Seq[T] => Boolean = seq => {
        def hasEnoughElementsOfType(count: Int, typeIdx: Int): Boolean = seq.count(el => typeIdxMapping(el) == typeIdx) >= count

        frequencies.zipWithIndex.forall { case (f, idx) => hasEnoughElementsOfType(f, idx) }
      }

      def requiredElements: Seq[T] => Seq[T] = seq => {
        def requiredElementsOfType(count: Int, typeIdx: Int): Seq[T] = seq.filter(el => typeIdxMapping(el) == typeIdx).take(count)

        frequencies.zipWithIndex.flatMap { case (f, idx) => requiredElementsOfType(f, idx) }
      }

      val accumulateEnoughElementsToHaveRequiredOnes = b.add(AccumulateUntil[T](containsRequiredElements))
      val keepOnlyRequiredElements = b.add(Flow[Seq[T]].map(requiredElements))
      val flatten = b.add(Flow[Seq[T]].mapConcat(el => el.toList))

      accumulateEnoughElementsToHaveRequiredOnes ~> keepOnlyRequiredElements ~> flatten

      FlowShape(accumulateEnoughElementsToHaveRequiredOnes.in, flatten.out)
    }
  }
}
