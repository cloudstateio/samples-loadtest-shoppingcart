package shoppingcart

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.app.Gatling
import io.gatling.core.config.GatlingPropertiesBuilder
import scala.concurrent.duration._
import io.grpc.ManagedChannelBuilder
import com.github.phisgr.gatling.grpc.Predef._
import com.example.shoppingcart.shoppingcart._
import scala.util.Try
import scala.util.Random

object RandomUsers {

  val randomUserFeeder = new Iterator[Map[String,String]] {

    private val RNG = new Random

    override def hasNext = true

    override def next: Map[String, String] = {
      val id = s"gatling-${RNG.nextLong.toString}"

      Map("userId" -> id)
    }
  }
}

class ShoppingCartSimulation1 extends Simulation {

  val host = scala.util.Properties.envOrElse("GATLING_HOST", "localhost")
  val portString = scala.util.Properties.envOrElse("GATLING_PORT", "9000")
  val port = Try(portString.toInt).getOrElse(9000)
  val numUsersString = scala.util.Properties.envOrElse("GATLING_NUM_USERS", "1")
  val numUsers = Try(numUsersString.toInt).getOrElse(10)
  val numRepsString = scala.util.Properties.envOrElse("GATLING_NUM_REPS", "10")
  val numReps = Try(numRepsString.toInt).getOrElse(10)
  val useSSLString = scala.util.Properties.envOrElse("GATLING_USE_SSL", "false")
  val useSSL = Try(useSSLString.toBoolean).getOrElse(false)
  val rampUpSecondsString = scala.util.Properties.envOrElse("GATLING_RAMP_UP_SECONDS", "30")
  val rampUpSeconds = Try(rampUpSecondsString.toInt).getOrElse(30)

  val grpcConf = if(useSSL)
                  grpc(ManagedChannelBuilder.forAddress(host, port))
                else
                  grpc(ManagedChannelBuilder.forAddress(host, port).usePlaintext())

  val userFeeder = RandomUsers.randomUserFeeder

  val itemFeeder = csv("items.csv").eager.random

  val scn = scenario("Adding items")
    .feed(userFeeder)
    .repeat(numReps) {
      feed(itemFeeder).
      exec(
        grpc("Add Item")
          .rpc(ShoppingCartGrpc.METHOD_ADD_ITEM)
          .payload(session =>
            for (
              userId <- session("userId").validate[String];
              productId <- session("product_id").validate[String];
              productName <- session("product_name").validate[String]
            ) yield AddLineItem(userId, productId = productId, name = productName, quantity = 1)))
      .exec(
        grpc("GetCart")
          .rpc(ShoppingCartGrpc.METHOD_GET_CART)
          .payload(session =>
            for (
              userId <- session("userId").validate[String]
            ) yield GetShoppingCart(userId))
          .extract(_.items.headOption.map(_.productId))(item => item.saveAs("addedItem"))
      )
      .exec(
        grpc("RemoveItem")
          .rpc(ShoppingCartGrpc.METHOD_REMOVE_ITEM)
          .payload(session =>
            for (
              userId <- session("userId").validate[String];
              productId <- session("addedItem").validate[String]
            ) yield RemoveLineItem(userId, productId))
      )
    }

  setUp(scn.inject(rampUsers(numUsers) during (30 seconds)).protocols(grpcConf))
    .assertions(
      global.responseTime.max.lt(2000),
      global.successfulRequests.percent.gte(100.0)
    )
}

object ShoppingCartRunner {
  def main(args: Array[String]) {

    // This sets the class for the simulation we want to run.
    val simClass = classOf[ShoppingCartSimulation1].getName
    val props = new GatlingPropertiesBuilder
    props.binariesDirectory("./target/scala-2.12/classes")
      .simulationClass(simClass)

    println(s"🛒🛒🛒 Testing Shopping cart 🛒🛒🛒")

    val response = Gatling.fromMap(props.build)

    if(response == 0)
      println("Test completed successfully ✅")
    else
      println("Tests completed unsuccessfully ❌")

    System.exit(response)
  }
}