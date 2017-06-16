package sanskritCoders.dcsScraper

import dbSchema.dcs.DcsBook
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import org.slf4j.LoggerFactory


object bookList {
  val log = LoggerFactory.getLogger(getClass.getName)
  var books = List[DcsBook]()

  def scrape = {
    val browser = JsoupBrowser()
    val doc = browser.get("http://kjc-sv013.kjc.uni-heidelberg.de/dcs/index.php?contents=texte")
    import net.ruippeixotog.scalascraper.dsl.DSL._
    import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
    import net.ruippeixotog.scalascraper.dsl.DSL.Parse._
    val bookTags = (doc >> elementList("select")) (0).children
    books = bookTags.map(bookTag => new DcsBook(title = bookTag.text, dcsId = bookTag.attr("value").toInt)).toList
    log.debug(bookTags.mkString("\n"))
  }
}

object dcsScraper {
  val log = LoggerFactory.getLogger(getClass.getName)

  def main(args: Array[String]): Unit = {
    bookList.scrape
  }
}
