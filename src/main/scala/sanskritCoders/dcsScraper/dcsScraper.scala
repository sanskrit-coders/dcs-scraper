package sanskritCoders.dcsScraper

import dbSchema.dcs.{DcsBook, DcsChapter, DcsSentence, DcsWord}
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import org.slf4j.LoggerFactory

import scala.collection.immutable
import scala.util.matching.Regex

object dcsScraper {

  import net.ruippeixotog.scalascraper.dsl.DSL._
  import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
  import net.ruippeixotog.scalascraper.dsl.DSL.Parse._

  val log = LoggerFactory.getLogger(getClass.getName)
  val browser = JsoupBrowser()


  def scrapeBookList: Seq[DcsBook] = {
    val doc = browser.get("http://kjc-sv013.kjc.uni-heidelberg.de/dcs/index.php?contents=texte")
    val bookTags = (doc >> elementList("select")) (0).children
    val books = bookTags.map(bookTag => new DcsBook(title = bookTag.text, dcsId = bookTag.attr("value").toInt)).toList
    return books
  }

  def scrapeChapterList(book: DcsBook) = {
    val doc = browser.post(s"http://kjc-sv013.kjc.uni-heidelberg.de/dcs/ajax-php/ajax-text-handler-wrapper.php",
      form = Map(
        "mode" -> "printchapters",
        "textid" -> book.dcsId.toString
      ))
    val chapterTags = doc >> elementList("option")
    val chapters = chapterTags.map(tag => new DcsChapter(dcsId = tag.attr("value").toInt, dcsName = Some(tag.text)))
    book.chapters = Some(chapters)
  }

  def scrapeChapter(chapter: DcsChapter) = {
    val doc = browser.post(s"http://kjc-sv013.kjc.uni-heidelberg.de/dcs/ajax-php/ajax-text-handler-wrapper.php",
      form = Map(
        "mode" -> "printsentences",
        "chapterid" -> chapter.dcsId.toString
      ))
    val sentenceTags = doc >> elementList(".sentence_div")
    val sentenceIds = sentenceTags.map(tag => tag.attr("sentence_id").toInt)
    chapter.sentenceIds = Some(sentenceIds)
  }

  def scrapeSentences(chapter: DcsChapter): Seq[DcsSentence] = {
    val doc = browser.post(s"http://kjc-sv013.kjc.uni-heidelberg.de/dcs/ajax-php/ajax-text-handler-wrapper.php",
      form = Map(
        "mode" -> "printsentences",
        "chapterid" -> chapter.dcsId.toString
      ))
    val sentenceTags = doc >> elementList(".sentence_div")
    val sentences = sentenceTags.map(tag => new DcsSentence(text = tag.text, dcsId = tag.attr("sentence_id").toInt))
    return sentences
  }

  //  <a href="index.php?contents=lemma&IDWord=32153"target="_blank">ambhodhi</a> [n.s.m.]&nbsp;&nbsp;
  // <a href="index.php?contents=lemma&IDWord=102755"target="_blank">sthala</a> [comp.]-<a href="index.php?contents=lemma&IDWord=203679"target="_blank">tā</a> [ac.s.f.]&nbsp;&nbsp;
  // <a href="index.php?contents=lemma&IDWord=102755"target="_blank">sthala</a> [n.s.n.]&nbsp;&nbsp;
  // <a href="index.php?contents=lemma&IDWord=88119"target="_blank">jaladhi</a> [comp.]-<a href="index.php?contents=lemma&IDWord=203679"target="_blank">tā</a> [ac.s.f.]&nbsp;&nbsp;
  // <a href="index.php?contents=lemma&IDWord=42911"target="_blank">dhūli</a> [g./o.s.m.]&nbsp;&nbsp;
  // <a href="index.php?contents=lemma&IDWord=81126"target="_blank">lava</a> [n.s.n.]&nbsp;&nbsp;
  // <a href="index.php?contents=lemma&IDWord=127714"target="_blank">śaila</a> [comp.]-<a href="index.php?contents=lemma&IDWord=203679"target="_blank">tā</a> [ac.s.f.]
  def scrapeAnalysis(sentence: DcsSentence) = {
    val doc = browser.post(s"http://kjc-sv013.kjc.uni-heidelberg.de/dcs/ajax-php/ajax-text-handler-wrapper.php",
      form = Map(
        "mode" -> "printonesentence",
        "sentenceid" -> sentence.dcsId.toString
      ))
    val wordGroupHtmls = doc.toHtml.split("&nbsp;&nbsp;").map(_.split("-"))
    val analysis = wordGroupHtmls.map(wordGroupHtml => {
      wordGroupHtml.map(x => {
        val y = browser.parseString(x) >> element("a")
        val grammarHint = new Regex("\\[(.+)\\]").findFirstIn(x)
        val word = new DcsWord(root = y.text,
          dcsId = y.attr("href").replaceAll(".*&IDWord=", "").toInt,
          dcsGrammarHint = grammarHint)
        word
      }).toSeq
    }).toSeq
    sentence.dcsAnalysisDecomposition = Some(analysis)
//    sentence.dcsAnalysis = Some(doc.toString)
//    log.debug(sentence.toString)
  }


  def main(args: Array[String]): Unit = {
    var books = scrapeBookList
    val dcsDb = new DictCouchbaseLiteDB
    dcsDb.openDatabasesLaptop()
    dcsDb.replicateAll()
    var numSentenceFailures = 0
    books.foreach(book => {
      log.info(s"Starting on book ${book.title}")
      scrapeChapterList(book)
      book.chapters.get.foreach(chapter => {
        //        scrapeChapter(chapter)
        log.info(s"Processing book ${book.title} - ${chapter.dcsName}")
        val sentences = scrapeSentences(chapter)
        sentences.foreach(s => {
          try {
            scrapeAnalysis(s)
          } catch {
            case e: Exception => {
              log.error(s"Alas! can't analyze $s. Error: ${e.toString}")
              numSentenceFailures = numSentenceFailures + 1
            }
          }
          dcsDb.updateSentenceDb(s)
        })
      })
      //      log.info(s"Fetched book ${book.title}")
      //      dcsDb.updateBooksDb(book)
    })
    log.error(s"Failed analysis on $numSentenceFailures sentences.")
    dcsDb.closeDatabases
  }
}