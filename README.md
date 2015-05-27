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

**Algorithmic Sharding**
Example: In this scheme, you use part of the data itself to do the partitioning. 
Sharding function may be `hash(user_id) % NUM_DB.`

    **Pros**: Simple, fine grained can reduce hotspots.
    
    **Cons**: Hotspots, resharding data is a pain as Consistency & Availability are compromised.

**Domain Partitioning**  
In this scheme, all of the data related to a specific feature of a product are stored on the same machines. A different cluster of machines served each of Profiles, Messages, etc.
    
    **Pros**: Handles non-uniform data better than algorithmic sharding
    
    **Cons**: Join multiple data sets

**Directory Based Sharding**
This scheme maintains a lookup table somewhere in the cluster which keeps track of which data is stored on which shard.

**Pros**: Handles non-uniform data better than algorithmic sharding, 

**Cons**: SPOF, overhead to consult with directory each time you want to access data.

This spike implements **Directory Based Sharding** using the following tables/entities:

```
/* Purpose of ShardIndexRecord is to store/correlate connectionString for a particular shardId. This allows one to find database server connection specific to a given shard */
case class ShardIndexRecord(shardId: String, connectionString: String, status: Boolean, createdDate: Long)
```

  **shardId** – used as globally unique id for a shard
  **connectionString** – a connection string used to connect to a shard
  **status** – used to signify a shard’s status as available or not available
  **createdDate** – the date the shard was added to the system, used for historical purposes

```
/* Purpose of UserShardRecord is to store/correlate shardId for a particular userId */
case class UserShardRecord(userId: String, shardId: String)
```
**userId** – used to uniquely identify a user. Is the same userId used to identify a user within the user table located on each shard.

**shardId** –used to uniquely identify the current shard that a user is located on

```
/* Purpose of DomainShardRecord is to store/correlate Domain Partition for a particular userId */
case class DomainShardRecord(userId: String, password: String, userName: String)
```

Let's walk through the following 4 common SQL command types (CRUD) operations in our sharded system. These command types are the SELECT, INSERT, UPDATE, and DELETE.



**Insert Scenario: A new user signs up.**
```
  Connect to the ShardIndexRecord using an externalized application configuration-level connection string.
  Query ShardIndexRecord and retrieve the next available shard row.
  Disconnect from the ShardIndexRecord.
  Connect to the DomainShardRecord as specified by the previously retrieved shard row’s connectionString.
  Insert the user’s info into the user table.  
  Disconnect from the DomainShardRecord.
  Connect to ShardIndexRecord using an application configuration-level connection string.
  Insert the new user’s lookup information into the UserShardRecord table, using the shardId from the retrieved shard table and the userId from the Domain Shard’s user table, for the new location of the user’s information.
  Disconnect from the ShardIndexRecord.
```

**Update Scenario: A user changes their password.**

  Connect to the ShardIndexRecord using an application configuration-level connection string.
  Query the UserShardRecord table, using user_id of the user, and retrieve the UserShardRecord row that contains the user’s lookup information.
  Query the ShardIndexRecord table and retrieve the shard row that represents the user’s DomainShardRecord location.
  Update the retrieved UserShardRecord row, updating necessary fields.
  Disconnect from the ShardIndexRecord.


**Delete Scenario: A user closes their account.**

  Connect to the ShardIndexRecord using an application configuration-level connection string.
  Query the UserShardRecord table, using the userId of the user, and retrieve the UserShardRecord row that contains the user’s lookup information.
  Query the shard table and retrieve the shard row that represents the user’s UserShardRecord location.
  Delete the user’s UserShardRecord row userId to find the user’s row.
  Disconnect from the ShardIndexRecord.
  Connect to the DomainShardRecord as specified by the previously retrieved shard row’s connectionString.
  Delete the user’s user row, found using the userId as retrieved earlier from the UserShardRecord table.
  Disconnect from the DomainShardRecord.


**Select Scenario: A system visitor views a user’s profile page.**

  Connect to the ShardIndexRecord using an application configuration-level connection string.
  Query the UserShardRecord table, using the userId of the user, and retrieve the UserShardRecord row that contains the user’s lookup information.
  Query the shard table and retrieve the shard row that represents the user’s Domain Shard location.
  Disconnect from the ShardIndexRecord.
  Connect to the Domain Shard as specified by the previously retrieved shard row’s connectionString.
  Query the UserShardRecord table to retrieve the user’s basic information, using the previously retrieved userId.
  As necessary, query the user’s additional profile information via DomainShardRecord.
  Disconnect from the DomainShardRecord.

  
