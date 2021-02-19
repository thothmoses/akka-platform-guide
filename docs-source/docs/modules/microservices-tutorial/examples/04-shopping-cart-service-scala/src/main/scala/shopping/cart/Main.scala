package shopping.cart

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorSystem
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import org.slf4j.LoggerFactory
import scala.util.control.NonFatal
import shopping.cart.repository.ItemPopularityRepositoryImpl
import shopping.cart.repository.ScalikeJdbcSetup

object Main {

  val logger = LoggerFactory.getLogger("shopping.cart.Main")

  def main(args: Array[String]): Unit = {
    val system =
      ActorSystem[Nothing](Behaviors.empty, "ShoppingCartService")
    try {
      init(system)
    } catch {
      case NonFatal(e) =>
        logger.error("Terminating due to initialization failure.", e)
        system.terminate()
    }
  }

  def init(system: ActorSystem[_]): Unit = {
    // tag::ItemPopularityProjection[]
    ScalikeJdbcSetup.init(system) // <1>
    // end::ItemPopularityProjection[]

    AkkaManagement(system).start()
    ClusterBootstrap(system).start()

    ShoppingCart.init(system)

    // tag::ItemPopularityProjection[]
    val itemPopularityRepository = new ItemPopularityRepositoryImpl() // <2>
    ItemPopularityProjection.init(system, itemPopularityRepository) // <3>
    // end::ItemPopularityProjection[]

    val grpcInterface =
      system.settings.config.getString("shopping-cart-service.grpc.interface")
    val grpcPort =
      system.settings.config.getInt("shopping-cart-service.grpc.port")
    val grpcService =
      new ShoppingCartServiceImpl(system, itemPopularityRepository)
    ShoppingCartServer.start(grpcInterface, grpcPort, system, grpcService)
  }

}
