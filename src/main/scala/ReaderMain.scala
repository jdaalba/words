import org.neo4j.driver.{GraphDatabase, Session}

import java.nio.file.{Files, Paths}

object ReaderMain extends App {

  //match (w:Word),
  //    // orange
  //    (co1:Char)-[po1:POS]->(w),
  //    (co2:Char)-[po2:POS]->(w)
  //// green
  //match (c:Char {_id:"A"})-[p:POS {in: 4}]->(w)
  //// grey
  //match (cg1:Char {_id: "B"})
  //match (cg2:Char {_id: "F"})
  //match (cg3:Char {_id: "H"})
  //match (cg4:Char {_id: "L"})
  //match (cg5:Char {_id: "Q"})
  //match (cg6:Char {_id: "U"})
  //match (cg7:Char {_id: "Z"})
  //match (cg8:Char {_id: "Y"})
  //match (cg9:Char {_id: "O"})
  //match (cg10:Char {_id: "G"})
  //match (cg11:Char {_id: "R"})
  //where true
  //and not (cg1)-[]->(w)
  //and not (cg2)-[]->(w)
  //and not (cg3)-[]->(w)
  //and not (cg4)-[]->(w)
  //and not (cg5)-[]->(w)
  //and not (cg6)-[]->(w)
  //and not (cg7)-[]->(w)
  //and not (cg8)-[]->(w)
  //and not (cg9)-[]->(w)
  //and not (cg10)-[]->(w)
  //and not (cg11)-[]->(w)
  //and (not co1._id = "I" and po1.in = 3)
  //and (not co2._id = "I" and po2.in = 5)
  //return w._id as name

  val words = Files.lines(Paths.get("src/main/resources/words.txt"))
    .filter(_.length == 5)
    .map(_ replace("á", "a"))
    .map(_ replace("é", "e"))
    .map(_ replace("í", "i"))
    .map(_ replace("ó", "o"))
    .map(_ replace("ú", "u"))
    .map(_.toUpperCase)
    .toList
  println("holi")

  val driver = GraphDatabase.driver("bolt://localhost:7687")

  val session = driver.session()

  words forEach (w => {
    println(s"inserting $w")
    insert(w)(session)
  })

  def insert(word: String)(session: => Session): Unit = {
    session.executeWrite(_ run s"""CREATE (w:Word {_id: "$word"})""")
    LazyList.from(0).zip(word.toCharArray) map {
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
