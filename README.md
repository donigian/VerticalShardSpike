Partitioning Spike!
===================
Some common terminology with regards to database scalability: 

The best way to deal with a problem is to avoid having to solve it. An ounce of prevention is worth of pound of cure. This spike is dedicated to times where `you must shard`! Before you shard, see if the following applies...
> -  Vertically scale your database server and see if the system is still having quality of service (QoS) issues during high load. Let's distinguish performance (speed of a single transaction) vs Scalability (QoS of entire system). This usually involves throwing $ by renting/purchasing better hardware for the database server.
> - Production Replica: If you're system is bound by read performance, you can try to setup a production read-only replica of your database then direct reads to that server. This will lessen the number of transactions on write server thus increasing performance.
> - Cache: Consider using memcached or other alternatives to cache database query responses to lessen the traffic on the servers.
 
What is sharding?
-------------
Sharding is a way of splitting and storing a single logical dataset in multiple databases. By distributing the data among multiple machines, a cluster of database systems can store larger dataset and handle additional requests.  As a result, a single instance of a database houses a subset of the system’s total dataset. Each shard typically shares the same schema, though the data on each shard is unique to that shard.

Sharding is necessary if a dataset is too large to be stored in a single database (exceeds max column limit) or to be scale with larger data and traffic. 

**There are 2 ways to partition:**

**Vertical Dataset Partitioning** – To partition a database table dataset vertically is to partition a dataset by column.

**Horizontal Dataset Partitioning** – To partition a database table dataset horizontally is to partition a dataset by row.
 
 Example of vertical partitioning
```
fetch_user_data(user_id) -> db[“USER”].fetch(user_id)
fetch_photo(photo_id) ->    db[“PHOTO”].fetch(photo_id)
```

Example of horizontal partitioning
```
fetch_user_data(user_id) -> user_db[user_id % 2].fetch(user_id)
```

The next step is to determine what the **shard** or **partition key** should be. A **partition key** allows you to retrieve and modify data efficiently by routing operations to the correct database. A **logical shard** is a collection of data sharing the same partition key.


Different Ways to Shard

:   **Algorithmic Sharding**
Example: In this scheme, you use part of the data itself to do the partitioning. 
Sharding function may be `hash(user_id) % NUM_DB.`

    **Pros**: Simple, fine grained can reduce hotspots.
    **Cons**: Hotspots, resharding data is a pain as Consistency & Availability are compromised.

:   **Domain Partitioning**  
In this scheme, all of the data related to a specific feature of a product are stored on the same machines. A different cluster of machines served each of Profiles, Messages, etc.
    **Pros**: Handles non-uniform data better than algorithmic sharding
    **Cons**: Join multiple data sets

:   **Directory Based Sharding**
This scheme maintains a lookup table somewhere in the cluster which keeps track of which data is stored on which shard.
    **Pros**: Handles non-uniform data better than algorithmic sharding, 
    **Cons**: SPOF, overhead to consult with directory each time you want to access data.

This spike implements **Directory Based Sharding** using the following constructs:


```
/* Purpose of ShardIndexRecord is to store/correlate shardId for a particular userId */
case class ShardIndexRecord(shardId: String, connectionString: String, status: Boolean, createdDate: Long)
```
 