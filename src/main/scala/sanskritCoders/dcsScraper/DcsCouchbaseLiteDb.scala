package sanskritCoders.dcsScraper

import _root_.java.io.File

import com.couchbase.lite.ManagerOptions
import dbSchema.dcs.{DcsBook, DcsObject, DcsSentence, DcsOldBook}
import dbUtils.jsonHelper
import sanskrit_coders.db.couchbaseLite.CouchbaseLiteDb

//import com.couchbase.lite.{Database, Manager, JavaContext, Document, UnsavedRevision, Query, ManagerOptions}
import com.couchbase.lite.{Database, JavaContext, Manager}
//import org.json4s.jackson.Serialization
import org.slf4j.LoggerFactory

// This version of the database uses Java (rather than Android) API.
class DcsCouchbaseLiteDB() {
  implicit def databaseToCouchbaseLiteDb(s: Database) = new CouchbaseLiteDb(s)
  val log = LoggerFactory.getLogger(getClass.getName)
  var booksDb: Database = null
  var sentencesDb: Database = null
  var dbManager: Manager = null

  def openDatabasesLaptop(readOnly: Boolean = false) = {
    val managerOptions = Manager.DEFAULT_OPTIONS
    managerOptions.setReadOnly(readOnly)
    dbManager = new Manager(new JavaContext("data") {
      override def getRootDirectory: File = {
        val rootDirectoryPath = "/home/vvasuki/dcs-scraper"
        new File(rootDirectoryPath)
      }
    }, managerOptions )
    dbManager.setStorageType("ForestDB")
    booksDb = dbManager.getDatabase("dcs_books")
    sentencesDb = dbManager.getDatabase("dcs_sentences")
  }

  def replicateAll() = {
    booksDb.replicate()
    sentencesDb.replicate()
  }


  def closeDatabases = {
    booksDb.close()
    sentencesDb.close()
  }

  def purgeAll = {
    booksDb.purgeDatabase()
    sentencesDb.purgeDatabase()
  }

  def updateBooksDb(dcsBook: DcsObject): Boolean = {
    val jsonMap = jsonHelper.getJsonMap(dcsBook)
    if (dcsBook.dcsId % 50 == 0) {
      log debug (jsonMap.toString())
    }
    //    sys.exit()
    booksDb.updateDocument(dcsBook.getKey, jsonMap)
    return true
  }

  def updateSentenceDb(dcsSentence: DcsSentence): Boolean = {
    val jsonMap = jsonHelper.getJsonMap(dcsSentence)
    if (dcsSentence.dcsId % 50 == 0) {
      log debug (jsonMap.toString())
    }
    //    sys.exit()
    sentencesDb.updateDocument(dcsSentence.getKey, jsonMap)
    return true
  }

  def getSentencesWithoutAnalysis(): Iterator[DcsSentence] = {
    val query = sentencesDb.createAllDocumentsQuery()
    return sentencesDb.listCaseClassObjects(query=query, explicitJsonClass=DcsSentence.getClass).map(_.asInstanceOf[DcsSentence])
      .filter(_.dcsAnalysisDecomposition == None)
  }

  def getOldBooks(): Iterator[DcsOldBook] = {
    val query = booksDb.createAllDocumentsQuery()
    // TODO: Fix the below.
    return booksDb.listCaseClassObjects(query=query, explicitJsonClass=DcsOldBook.getClass).map(_.asInstanceOf[DcsOldBook])
  }
}
