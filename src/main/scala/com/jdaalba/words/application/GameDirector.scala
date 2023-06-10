package com.jdaalba.words.application

import com.jdaalba.words.{Gray, Green, Orange, Writer}
import com.jdaalba.words.Writer.Board
import com.jdaalba.words.config.SeleniumConfig
import com.jdaalba.words.io.Game
import org.neo4j.driver.GraphDatabase

import java.util.logging.Logger
import scala.annotation.tailrec
import scala.jdk.CollectionConverters.CollectionHasAsScala

object GameDirector extends App {

  private val log: Logger = Logger.getLogger("GameDirector")

  private val config = new SeleniumConfig()
  private val game = new Game(config.getDriver, "https://lapalabradeldia.com")
  private val driver = GraphDatabase.driver("bolt://localhost:7687")
  private val session = driver.session()

  try {
    for (i <- 1 to 10) {
      game.setAddress(s"https://lapalabradeldia.com/archivo/normal/$i")
      run(List()) match {
        case Some(r) => log.info(s"Result for $i is $r")
        case None => log.warning(s"No known response for $i")
      }
    }

  } catch {
    case e: Throwable => log.severe(s"Exception: $e")
  } finally {
    config.close()
  }

  @tailrec
  def run(board: Board): Option[String] = {
    val res = execute(next(board))
    if (stillRunning(res)) {
      run(res)
    } else {
      if (isComplete(res)) {
        Some(res.last.map(_.char).mkString)
      } else {
        None
      }
    }
  }

  private def stillRunning(board: Board): Boolean = board.size < 6 & !isComplete(board)


  private def isComplete(board: Board): Boolean = board.last.forall(_.isInstanceOf[Green])

  private def execute(word: String): Board = game.apply(word).asScala
    .map(m => m.stream()
      .map{
        case (c, "rgb(117, 117, 117)") => Gray(c)
        case (c, "rgb(228, 168, 29)") => Orange(c)
        case (c, "rgb(67, 160, 71)") => Green(c)
      }
      .toList
      .asScala
      .toList
    ).toList

  def next(board: Board): String = {
    if (board.isEmpty) {
      "QUESO"
    } else {

      val queryRes: String = session.executeRead(tx => {
        val query = Writer.play(board)
        log.info(s"@@@ executing $query")
        val r = tx.run(query)
        r.next().fields().stream().map(p => p.value().asMap().get("_id")).findAny().get()
      }).toString
      log.info(s"""Next: "$queryRes"""")
      queryRes
    }
  }
}
