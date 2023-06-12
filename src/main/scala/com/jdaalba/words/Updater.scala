package com.jdaalba.words

import org.neo4j.driver.GraphDatabase
import org.neo4j.driver.types.Entity

object Updater extends App {
  val query =
    """MATCH (c:Char)-[:POS]->(w:Word)
      |WITH c, count(w) as rels
      |WHERE rels > 1
      |RETURN c.`_id` as id, rels
      |order by c.`_id`""".stripMargin

  val driver = GraphDatabase.driver("bolt://localhost:7687")

  val session = driver.session()

  import scala.jdk.CollectionConverters.CollectionHasAsScala

  val res = session.executeRead(tx => tx.run(query).stream().toList)
    .asScala
    .map(r => (r.get("id", ""), r.get("rels", 0)))

  val total = res.foldLeft(0)((total, r) => total + r._2).toDouble

  implicit val weights: Map[Char, Double] = res.map(r => (r._1.charAt(0), r._2 / total)).toMap

  val returner =
    """match (w:Word)
      |return w
      |""".stripMargin

  val words = session.executeRead(tx => tx.run(returner).stream().toList)

  words.asScala
    .map(_.get("w").asEntity())
    .filterNot(n => {
      val value = n.get("_id").toString
      value.contains("Ü") | value.contains("Î") | value.contains(".")

    })
    .map(calcule)
    .map {
      case WeightedWord(id, word, score, uniqueLetters) =>
        println(s"word: $word")
        s"""match (w:Word)
           |where elementId(w) = "$id"
           |set w.score = $score, w.uniqueLetters = $uniqueLetters""".stripMargin
    } foreach(q => session.executeWrite(tx => tx.run(q)))


  println("ey")

  case class WeightedWord(id: String, word: String, score: Double, uniqueLetters: Int)

  def calcule(implicit weights: Map[Char, Double]): Entity => WeightedWord = e =>
    WeightedWord(
      e.elementId(),
      e.get("_id").toString,
      e.get("_id").toString.toCharArray.filterNot(_ == '"').map(weights).sum,
      e.get("_id").toString.toCharArray.filterNot(_ == '"').toSet.size
    )
}
