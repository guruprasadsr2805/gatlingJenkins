package challenge

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.concurrent.duration._
import scala.util.Random

class FinalTest extends Simulation{

//  private def getProperty(propertyName: String, defaultValue: String) = {
//    Option(System.getenv(propertyName))
//      .orElse(Option(System.getProperty(propertyName)))
//      .getOrElse(defaultValue)
//  }
//
//  def userCount: Int = getProperty("USERS", "5").toInt
//  def rampDuration: Int = getProperty("RAMP_DURATION", "10").toInt
//  def testDuration: Int = getProperty("DURATION", "60").toInt
//
//  var idNumbers = (11 to 15).iterator
//  val now = LocalDate.now()
//  val pattern = DateTimeFormatter.ofPattern("yyyy-MM-dd")
//  val rnd = new Random()
//
//  def randomString(length: Int) = {
//    rnd.alphanumeric.filter(_.isLetter).take(length).mkString
//  }
//
//  def getRandomNumber() = {
//    rnd.between(100,200)
//  }
//  def getRandomDate(startDate: LocalDate, random: Random): String = {
//    startDate.minusDays(random.nextInt(30)).format(pattern)
//  }
//
//  val customFeeder = Iterator.continually(Map(
//    "gameId" -> idNumbers.next(),
//    "name" -> ("Game-" + randomString(5)),
//    "releaseDate" -> getRandomDate(now, rnd),
//    "reviewScore" -> rnd.nextInt(100),
//    "category" -> ("Category-" + randomString(6)),
//    "rating" -> ("Rating-" + randomString(4))
//  ))
//
//  // GET all video games
//  def getAllVideoGames() = {
//    exec(
//      http("Get all video games")
//        .get("videogames")
//        .check(status.is(200))
//    )
//  }
//
//  def postNewGame() = {
//    feed(customFeeder)
//      .exec(http("Post New Game")
//        .post("videogames")
//        .body(ElFileBody("bodies/NewGameTemplate.json")).asJson
//        .check(status.is(200))
//        .check(jsonPath("$.status").is("Record Added Successfully")))
//  }
//
//  def getSpecificgame() = {
//    exec(
//      http("Get the created video game")
//        .get("videogame/${gameId}")
//        .check(status.is(200))
//        .check(jsonPath("$.name").is("${name}"))
//    )
//  }
//
//  def deleteSpecificgame() = {
//    exec(
//      http("Delete the created video game")
//        .delete("videogame/${gameId}")
//        .check(status.is(200))
//        .check(jsonPath("$.status").is("Record Deleted Successfully")))
//  }
//
//  before {
//    println(s"Running test with ${userCount} users")
//    println(s"Ramping users over ${rampDuration} seconds")
//    println(s"Total test duration: ${testDuration} seconds")
//  }
//
//  val httpConf = http.baseUrl("http://localhost:8080/app/")
//    .header("Accept", "application/json")
//
//
//  val scn = scenario("Challenge script")
//    .forever(){
//      exec(getAllVideoGames())
//        .pause(2)
//      exec(postNewGame())
//        .pause(2)
//      exec(getSpecificgame())
//        .pause(2)
//      exec(deleteSpecificgame())
//    }
//
//  setUp(
//    scn.inject(
//      nothingFor(5 ),
//      rampUsers(userCount) during(rampDuration.seconds)
//    )
//  )
//    .protocols(httpConf)
//    .maxDuration(testDuration.seconds)
//
//  after{
//    println("Stress test completed")
//  }

  val httpConf = http
    .baseUrl("http://localhost:8080/app/")
    .header("Accept", "application/json")

  /*** Variables ***/
  // runtime variables
  def userCount: Int = getProperty("USERS", "3").toInt
  def rampDuration: Int = getProperty("RAMP_DURATION", "10").toInt
  def testDuration: Int = getProperty("DURATION", "60").toInt

  // other variables
  var idNumbers = (20 to 1000).iterator
  val rnd = new Random()
  val now = LocalDate.now()
  val pattern = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  /*** Helper Methods ***/
  private def getProperty(propertyName: String, defaultValue: String) = {
    Option(System.getenv(propertyName))
      .orElse(Option(System.getProperty(propertyName)))
      .getOrElse(defaultValue)
  }

  def randomString(length: Int) = {
    rnd.alphanumeric.filter(_.isLetter).take(length).mkString
  }

  def getRandomDate(startDate: LocalDate, random: Random): String = {
    startDate.minusDays(random.nextInt(30)).format(pattern)
  }

  /*** Custom Feeder ***/
  val customFeeder = Iterator.continually(Map(
    "gameId" -> idNumbers.next(),
    "name" -> ("Game-" + randomString(5)),
    "releaseDate" -> getRandomDate(now, rnd),
    "reviewScore" -> rnd.nextInt(100),
    "category" -> ("Category-" + randomString(6)),
    "rating" -> ("Rating-" + randomString(4))
  ))

  /*** Before ***/
  before {
    println(s"Running test with ${userCount} users")
    println(s"Ramping users over ${rampDuration} seconds")
    println(s"Total Test duration: ${testDuration} seconds")
  }

  /*** HTTP Calls ***/
  def getAllVideoGames() = {
    exec(
      http("Get All Video Games")
        .get("videogames")
        .check(status.is(200)))
  }

  def postNewGame() = {
    feed(customFeeder).
      exec(http("Post New Game")
        .post("videogames")
        .body(ElFileBody("bodies/NewGameTemplate.json")).asJson //template file goes in gating/resources/bodies
        .check(status.is(200)))
  }

  def getLastPostedGame() = {
    exec(http("Get Last Posted Game")
      .get("videogames/${gameId}")
      .check(jsonPath("$.name").is("${name}"))
      .check(status.is(200)))
  }

  def deleteLastPostedGame() = {
    exec(http("Delete Last Posted Game")
      .delete("videogames/${gameId}")
      .check(status.is(200)))
  }

  /*** Scenario Design ***/
  val scn = scenario("Video Game DB")
    .forever() {
      exec(getAllVideoGames())
        .pause(2)
        .exec(postNewGame())
        .pause(2)
        .exec(getLastPostedGame())
        .pause(2)
        .exec(deleteLastPostedGame())
    }

  /*** Setup Load Simulation ***/
  setUp(
    scn.inject(
      nothingFor(5.seconds),
      rampUsers(userCount) during (rampDuration.seconds))
  )
    .protocols(httpConf)
    .maxDuration(testDuration.seconds)

  /*** After ***/
  after {
    println("Stress test completed")
  }
}
