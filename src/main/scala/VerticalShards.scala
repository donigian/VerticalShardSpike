import java.sql.DriverManager
import java.sql.Connection

/**
 * Created by arm on 5/26/15.
 */
object VerticalShards {

  /*
  + Create Index Shard (shard table)
  shardId – used as the unique identifier for a shard
  connectionString – a connection string used to connect to a shard
  status – used to signify a shard’s status as online, offline, or in active insert mode
  createdDate – the date the shard was added to the system, used for historical purposes

  + create Domain Shard(user_lookup table)
  userId – used to uniquely identify a user. Is the same userId used to identify a user within the user table located on each shard.
  shardId – used to uniquely identify the current shard that a user is located on


  Insert Scenario: A new user signs up.

  Connect to the Index Shard using an application configuration-level connection string.
  Query the shard table and retrieve the shard row that represents the current shard with a status of active insert mode.
  Disconnect from the Index Shard.
  Connect to the Domain Shard as specified by the previously retrieved shard row’s connectionString.
  Insert the user’s sign up information into the user table. Retrieving the userId as a result.
  Insert the user’s remaining creation information in to the user table’s related tables as necessary (i.e. user_profile, user_blog, etc).
  Disconnect from the Domain Shard.
  Connect to the Index Shard using an application configuration-level connection string.
  Insert the new user’s lookup information into the user_lookup table, using the shardId from the retrieved shard table and the userId from the Domain Shard’s user table, for the new location of the user’s information.
  Disconnect from the Index Shard.


  Update Scenario: A user changes their password.

  Connect to the Index Shard using an application configuration-level connection string.
  Query the user_lookup table, using user_id of the user, and retrieve the user_lookup row that contains the user’s lookup information.
  Query the shard table and retrieve the shard row that represents the user’s Domain Shard location.
  Update the retrieved user_lookup row, changing the password field to the user’s new password.
  Disconnect from the Index Shard.


  Delete Scenario: A user closes their account.

  Connect to the Index Shard using an application configuration-level connection string.
  Query the user_lookup table, using the userId of the user, and retrieve the user_lookup row that contains the user’s lookup information, saving it for later use.
  Query the shard table and retrieve the shard row that represents the user’s Domain Shard location.
  Delete the user’s user_lookup row, using the user’s username and password, or userId to find the user’s row.
  Disconnect from the Index Shard.
  Connect to the Domain Shard as specified by the previously retrieved shard row’s connectionString.
  Delete the user’s user row, found using the userId as retrieved earlier from the user_lookup table.
  Disconnect from the Domain Shard.


  Select Scenario: A system visitor views a user’s profile page.

  Connect to the Index Shard using an application configuration-level connection string.
  Query the user_lookup table, using the userId of the user, and retrieve the user_lookup row that contains the user’s lookup information.
  Query the shard table and retrieve the shard row that represents the user’s Domain Shard location.
  Disconnect from the Index Shard.
  Connect to the Domain Shard as specified by the previously retrieved shard row’s connectionString.
  Query the user_lookup table to retrieve the user’s basic information, using the previously retrieved userId.
  As necessary, query the user’s additional profile information and blog entries via the user_profile, user_blog, and user_blog_entry tables respectively.
  Disconnect from the Domain Shard.

   */




  def main(args: Array[String]) {
    // connect to the database named "mysql" on the localhost
    val driver = "org.postgresql.Driver"
    val url = "jdbc:postgresql://localhost/arm"
    val username = "arm"
    val password = ""

    // there's probably a better way to do this
    var connection:Connection = null

    try {
      // make the connection
      Class.forName(driver)
      connection = DriverManager.getConnection(url, username, password)

      // create the statement, and run the select query
      val statement = connection.createStatement()
      val resultSet = statement.execute("CREATE TABLE boohoo ( ID   INT NOT NULL, AGE  INT NOT NULL)")
//      while ( resultSet.next() ) {
//        val host = resultSet.getString("host")
//        val user = resultSet.getString("user")
//        println("host, user = " + host + ", " + user)
//      }
    } catch {
      case e => e.printStackTrace
    }
    connection.close()
  }
}

