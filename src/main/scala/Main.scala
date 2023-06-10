import org.neo4j.driver.{GraphDatabase, Session}

object Main {

  def main(args: Array[String]): Unit = {
    val driver = GraphDatabase.driver("bolt://localhost:7687")

    val session = driver.session()

    session.executeRead(tx => tx.run("match (w:Word) return w limit 1"))

   // Seq("ALFIL") foreach (insert(_)(session))

  }

  def insert(word: String)(session: => Session): Unit = {
    session.executeWrite(_ run s"""CREATE (w:Word {_id: "$word"})""")
    LazyList.from(1).zip(word.toCharArray) map {
      case (i, c) => s"""
                        |MATCH
                        |  (c:Char),
                        |  (w:Word)
                        |WHERE c._id = '$c' AND w._id = '$word'
                        |CREATE (c)-[:POS {in: $i}]->(w)
                        |""".stripMargin
    } foreach (q => session executeWrite (_ run q))
  }
}