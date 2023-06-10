package com.jdaalba.words

import scala.language.postfixOps

object Writer {

  type Row = List[Result]
  type Board = List[Row]
  type Filter = Set[Condition]

  def play(board: Board): String = s"match (w:Word) ${translate(buildQuery(board))} return w"

  def translate(filter: Filter): String = (
    "where true" :: (
      filter.toList.filterNot(_.isInstanceOf[IndexedInclusion]).map(translate)
        ++ filter.toList.filter(_.isInstanceOf[IndexedInclusion])
        .map {
          case i: IndexedInclusion => i
        }
        .sortBy(_.index)
        .groupMapReduce(_.char)(List(_))(_ ++ _)
        .toList
        .map("and (" + _._2.map(translate).mkString(" or ") + ")")
      )
    ) mkString " "

  def empty: Filter = Set()

  def buildQuery: PartialFunction[Board, Filter] = {
    case Nil => empty
    case r :: rs => combine(mapRow(r), buildQuery(rs))
  }

  private def combine: PartialFunction[(Filter, Filter), Filter] = {
    case (nf, f) if nf isEmpty => f
    case (nf, f) if f isEmpty => nf
    case (nf, f) => nf head match {
      case e: Exclusion => combine(nf tail, f + e)
      case IndexedExclusion(c, i) => combine(nf tail, (f + IndexedExclusion(c, i)) - IndexedInclusion(c, i))
      case _ => combine(nf tail, f)
    }
  }

  private def mapRow(row: Row): Filter = {
    val greens: List[Inclusion] = row.zipWithIndex
      .filter(_._1.isInstanceOf[Green])
      .map {
        case (Green(c), i) => Inclusion(c, i)
      }
    val frees: List[Int] = for {
      i <- row.indices.toList
      if !greens.map(_.index).contains(i)
    } yield i

    val oranges: List[Condition] = row.zipWithIndex
      .filter(_._1.isInstanceOf[Orange])
      .flatMap {
        case (Orange(c), i) =>
          IndexedExclusion(c, i) :: row.zipWithIndex
            .filter {
              case (Green(_), _) | (Orange(`c`), _) | (Gray(`c`), _) => false
              case _ => true
            }
            .map(_._2)
            .map(IndexedInclusion(c, _))
      }

    val grays = row.zipWithIndex
      .filter(_._1.isInstanceOf[Gray])
      .flatMap {
        case (Gray(c), i) if row contains Orange(c) => IndexedExclusion(c, i) :: Nil
        case (Gray(c), _) if row contains Green(c) => frees map (IndexedExclusion(c, _))
        case (Gray(c), _) => Exclusion(c) :: Nil
      }
    (greens ++ oranges ++ grays).toSet
  }

  private def translate: PartialFunction[Condition, String] = {
    case Exclusion(c) => s"""and not (:Char {`_id`: "$c"})-[]->(w)"""
    case Inclusion(c, i) => s"""and (:Char {`_id`: "$c"})-[:POS {`in`:$i}]->(w)"""
    case IndexedExclusion(c, i) => s"""and not (:Char {`_id`: "$c"})-[:POS {`in`:$i}]->(w)"""
    case IndexedInclusion(c, i) => s"""(:Char {`_id`: "$c"})-[:POS {`in`:$i}]->(w)"""
  }
}

sealed trait Condition {
  def char: Char
}

sealed trait IndexedCondition extends Condition {
  def index: Int
}

case class Inclusion(char: Char, index: Int) extends IndexedCondition

case class IndexedInclusion(char: Char, index: Int) extends IndexedCondition

case class Exclusion(char: Char) extends Condition

case class IndexedExclusion(char: Char, index: Int) extends IndexedCondition

sealed trait Result {
  def char: Char
}

case class Gray(char: Char) extends Result

case class Green(char: Char) extends Result

case class Orange(char: Char) extends Result