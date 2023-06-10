package com.jdaalba.words

import org.scalatest.funsuite.FunSuite

class WriterTest extends FunSuite {

  test("build a query just with grays") {
    assertResult(
      Set(Exclusion('A'), Exclusion('B'), Exclusion('C'), Exclusion('D'), Exclusion('E'))
    )(
      Writer.buildQuery(List(
        List(Gray('A'), Gray('B'), Gray('C'), Gray('D'), Gray('E'))
      ))
    )
  }

  test("build a query with grays and a green") {
    assertResult(
      Set(Inclusion('A', 0), Exclusion('B'), Exclusion('C'), Exclusion('D'), Exclusion('E'))
    )(Writer.buildQuery(List(
      List(Green('A'), Gray('B'), Gray('C'), Gray('D'), Gray('E'))
    ))
    )
  }

  test("build a query with a green and a gray with the same letter") {
    assertResult(
      Set(Inclusion('A', 0), IndexedExclusion('A', 1), IndexedExclusion('A', 2), IndexedExclusion('A', 3),
        IndexedExclusion('A', 4), Exclusion('C'), Exclusion('D'), Exclusion('E'))
    )(
      Writer.buildQuery(List(
        List(Green('A'), Gray('A'), Gray('C'), Gray('D'), Gray('E'))
      ))
    )
  }

  test("build a query with an orange") {
    assertResult(
      Set(Exclusion('A'), IndexedExclusion('B', 1), IndexedInclusion('B', 0), IndexedInclusion('B', 2),
        IndexedInclusion('B', 3), IndexedInclusion('B', 4), Exclusion('C'), Exclusion('D'), Exclusion('E'))
    )(
      Writer.buildQuery(List(
        List(Gray('A'), Orange('B'), Gray('C'), Gray('D'), Gray('E'))
      ))
    )
  }

  test("build a query with a green and an orange") {
    assertResult(
      Set(Exclusion('A'), IndexedExclusion('B', 1), IndexedInclusion('B', 0), IndexedInclusion('B', 2),
        IndexedInclusion('B', 3), Exclusion('C'), Exclusion('D'), IndexedExclusion('B', 4))
    )(
      Writer.buildQuery(List(
        List(Gray('A'), Orange('B'), Gray('C'), Gray('D'), Orange('B'))
      ))
    )
  }

  test("build a query with an orange and a gray") {
    assertResult(
      Set(
        Exclusion('A'), Exclusion('C'), Exclusion('D'),
        IndexedExclusion('B', 1), IndexedExclusion('B', 4),
        IndexedInclusion('B', 0), IndexedInclusion('B', 2), IndexedInclusion('B', 3)
      )
    )(
      Writer.buildQuery(List(
        List(Gray('A'), Orange('B'), Gray('C'), Gray('D'), Gray('B'))
      ))
    )
  }

  test("build a query from a real sample with one row") {
    assertResult(
      Set(
        IndexedExclusion('I', 2),
        IndexedInclusion('I', 0), IndexedInclusion('I', 1), IndexedInclusion('I', 3), IndexedInclusion('I', 4),
        Exclusion('G'), Exclusion('U'), Exclusion('S'), Exclusion('O')
      )
    )(
      Writer.buildQuery(List(
        List(Gray('G'), Gray('U'), Orange('I'), Gray('S'), Gray('O'))
      ))
    )
  }

  test("build a query from a real sample with two rows") {
    assertResult(
      Set(
        IndexedExclusion('I', 2), IndexedExclusion('I', 4),
        IndexedInclusion('I', 0), IndexedInclusion('I', 1), IndexedInclusion('I', 3),
        Exclusion('G'), Exclusion('U'), Exclusion('S'), Exclusion('O'),
        Exclusion('A'), Exclusion('B'), Exclusion('D'),
      )
    )(
      Writer.buildQuery(List(
        List(Gray('G'), Gray('U'), Orange('I'), Gray('S'), Gray('O')),
        List(Gray('A'), Gray('B'), Gray('A'), Gray('D'), Orange('I'))
      ))
    )
  }

  test("build a query from a real sample with three rows") {
    assertResult(
      Set(
        IndexedExclusion('I', 2), IndexedExclusion('I', 4),
        Exclusion('G'), Exclusion('U'), Exclusion('S'), Exclusion('O'),
        Exclusion('A'), Exclusion('B'), Exclusion('D'),
        Exclusion('C'), Exclusion('R'),
        Inclusion('I', 1), Inclusion('E', 4),
      )
    )(
      Writer.buildQuery(List(
        List(Gray('G'), Gray('U'), Orange('I'), Gray('S'), Gray('O')),
        List(Gray('A'), Gray('B'), Gray('A'), Gray('D'), Orange('I')),
        List(Gray('C'), Green('I'), Gray('R'), Gray('C'), Green('E'))
      ))
    )
  }

  test("empty filter predicate") {
    assertResult(
      "where true"
    )(
      Writer.translate(Writer.empty)
    )
  }

  test("just with exclusions") {
    assertResult(
      "where true"
        + """ and not (:Char {`_id`: "A"})-[]->(w)"""
        + """ and not (:Char {`_id`: "B"})-[:POS {`in`:1}]->(w)"""
    )(
      Writer.translate(Set(Exclusion('A'), IndexedExclusion('B', 1)))
    )
  }

  test("just with exclusions and inclusions") {
    assertResult(
      "where true"
        + """ and not (:Char {`_id`: "A"})-[]->(w)"""
        + """ and not (:Char {`_id`: "B"})-[:POS {`in`:1}]->(w)"""
        + """ and (:Char {`_id`: "C"})-[:POS {`in`:2}]->(w)"""
    )(
      Writer.translate(Set(Exclusion('A'), IndexedExclusion('B', 1), Inclusion('C', 2)))
    )
  }

  test("just with indexed inclusions") {
    assertResult(
      "where true"
        + """ and ((:Char {`_id`: "C"})-[:POS {`in`:0}]->(w) or (:Char {`_id`: "C"})-[:POS {`in`:2}]->(w))"""
        + """ and ((:Char {`_id`: "D"})-[:POS {`in`:1}]->(w) or (:Char {`_id`: "D"})-[:POS {`in`:3}]->(w))"""
    )(
      Writer.translate(Set(
        IndexedInclusion('C', 0), IndexedInclusion('D', 1), IndexedInclusion('C', 2), IndexedInclusion('D', 3)
      ))
    )
  }

  test("all together") {
    assertResult(
      "where true"
        + """ and (:Char {`_id`: "C"})-[:POS {`in`:2}]->(w)"""
        + """ and not (:Char {`_id`: "B"})-[:POS {`in`:1}]->(w)"""
        + """ and not (:Char {`_id`: "A"})-[]->(w)"""
        + """ and ((:Char {`_id`: "D"})-[:POS {`in`:0}]->(w) or (:Char {`_id`: "D"})-[:POS {`in`:2}]->(w))"""
        + """ and ((:Char {`_id`: "E"})-[:POS {`in`:1}]->(w) or (:Char {`_id`: "E"})-[:POS {`in`:3}]->(w))"""
    )(
      Writer.translate(Set(
        Exclusion('A'), IndexedExclusion('B', 1), Inclusion('C', 2), IndexedInclusion('D', 0), IndexedInclusion('E', 1),
        IndexedInclusion('D', 2), IndexedInclusion('E', 3)
      ))
    )
  }
}
