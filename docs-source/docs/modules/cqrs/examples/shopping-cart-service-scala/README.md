This tutorial contains a sample illustrating an CQRS design with [Akka Cluster Sharding](https://doc.akka.io/docs/akka/2.6/typed/cluster-sharding.html), [Akka Cluster Singleton](https://doc.akka.io/docs/akka/2.6/typed/cluster-singleton.html), [Akka Persistence](https://doc.akka.io/docs/akka/2.6/typed/persistence.html) and [Akka Persistence Query](https://doc.akka.io/docs/akka/2.6/persistence-query.html).

## Overview

This sample application implements a CQRS-ES design that will side-effect in the read model on selected events persisted to Cassandra by the write model. In this sample, the side-effect is logging a line. 
A more practical example would be to send a message to a Kafka topic or update a relational database.

## Write model

The write model is a shopping cart.

The implementation is based on a sharded actor: each `ShoppingCart` is an [Akka Cluster Sharding](https://doc.akka.io/docs/akka/2.6/typed/cluster-sharding.html) entity. The entity actor `ShoppingCart` is an [EventSourcedBehavior](https://doc.akka.io/docs/akka/2.6/typed/persistence.html).

Events from the shopping carts are tagged and consumed by the read model.

## Read model

The read model is implemented in such a way that 'load' is sharded over a number of processors.
This is implemented using [Akka Projections](https://doc.akka.io/docs/akka-projection/current) which is then running on top of
 [Sharded Daemon Process](https://doc.akka.io/docs/akka/current/typed/cluster-sharded-daemon-process.html).


## Running the sample code

1. Start a local Cassandra server on default port 9042 and a Kafka broker on port 9092. The included `docker-compose.yml` starts everything required for running locally.

```
docker-compose up -d
```

2. Start a node on port 2551:

```
sbt "runMain sample.shoppingcart.Main 2551"
```

3. (Optional) Start another node on port 2552:

```
sbt "runMain sample.shoppingcart.Main 2552"
```

4. (Optional) More can be started started by defining different ports:

```
sbt "runMain sample.shoppingcart.Main 2553"
sbt "runMain sample.shoppingcart.Main 2554"
```

Try it with [grpcurl](https://github.com/fullstorydev/grpcurl):

```
# add item to cart
grpcurl -d '{"cartId":"cart1", "itemId":"socks", "quantity":3}' -plaintext 127.0.0.1:8051 shoppingcart.ShoppingCartService.AddItem

# get cart
grpcurl -d '{"cartId":"cart1"}' -plaintext 127.0.0.1:8051 shoppingcart.ShoppingCartService.GetCart

# update quantity of item
grpcurl -d '{"cartId":"cart1", "itemId":"socks", "quantity":5}' -plaintext 127.0.0.1:8051 shoppingcart.ShoppingCartService.UpdateItem

# check out cart
grpcurl -d '{"cartId":"cart1"}' -plaintext 127.0.0.1:8051 shoppingcart.ShoppingCartService.Checkout

# get item popularity
grpcurl -d '{"itemId":"socks"}' -plaintext 127.0.0.1:8051 shoppingcart.ShoppingCartService.GetItemPopularity
```

or same `grpcurl` commands to port 8052.