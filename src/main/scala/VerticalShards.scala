package shard

import java.sql.DriverManager
import java.sql.Connection
import com.typesafe.config.ConfigFactory
import grizzled.slf4j.Logger

import shardSpike._

/**
 * Created by arm on 5/26/15.
 */

object externalizedConfigs {
  val conf = ConfigFactory.load()
  val dbDriver = conf.getString("db.driver")
  val dbDefaultConnStr = conf.getString("db.default_connection_string")
  val dbUname = conf.getString("db.username")
  val dbPasswd = conf.getString("db.password")
  val logger = Logger("com.verticalshardspike")

}

object VerticalShards {

  trait VerticalShardsManager {
    def read(primaryKeys: List[String]): List[Option[DomainShardRecord]]
    def write(domainRecord: DomainShardRecord): Unit
  }

  class VerticalShardsManagerImpl extends VerticalShardsManager {

    override def read(primaryKeys: List[String]): List[Option[DomainShardRecord]] = {

      if (!ShardMetadataTableCreated) {
        createShardMetadataTables
      }

      val domainRecord: List[Option[DomainShardRecord]] = for {
        userId <- primaryKeys
        userShardRecord = findUserShardRecord(userId)
        shardRecord = findShardRecordById(userShardRecord.get.shardId).get
        domainRecord = findDomainRecord(userId, shardRecord.connectionString)
      } yield domainRecord

      domainRecord
    }

    override def write(domainRecord: DomainShardRecord): Unit = {
      if (!ShardMetadataTableCreated) {
        createShardMetadataTables
      }

      val availableShardRecord = findAvailableShardRecord(shardSpike.findAvailableShardSqlStmt).get
      insertDomainRecord(domainRecord.userId, availableShardRecord.shardId, availableShardRecord.connectionString)
      updateShardRecordById(availableShardRecord.shardId, domainRecord.userId)

    }

    def insertDomainRecord(userId: String, shardId: String, connectionString: String): Boolean = {
      if (!domainRecordTableExist(connectionString))
        createDomainShardTable(sqlCreateDomainShardTableStmt, connectionString)

      executeSql(s"INSERT INTO DomainShard VALUES ($userId, $shardId)", connectionString)
    }

    def findDomainRecord(userId: String, connectionString: String): Option[DomainShardRecord] = {
      findDomainRecordById(s"SELECT * FROM DomainShard WHERE userId=$userId", connectionString)
    }

    def createIndexTable(sqlStmt: String): Boolean = {
      executeSql(sqlStmt, externalizedConfigs.dbDefaultConnStr)
    }

    def createDomainShardTable(sqlStmt: String, connectionString: String): Boolean = {
      executeSql(sqlStmt, externalizedConfigs.dbDefaultConnStr)
    }

    def domainRecordTableExist(connectionString: String): Boolean = {
      executeSql(sqlDomainShardTableExistsStmt, connectionString)
    }

    def createShardMetadataTables: Unit = {
      createIndexTable(sqlCreateIndexShardTableStmt)
      /* Assume IndexShard table will be populated with a list of available shards in advance via base db migration */

      createIndexTable(sqlCreateUserShardTableStmt)
      ShardMetadataTableCreated = true
    }


    def executeSql(sqlStmt: String, connectionString: String): Boolean = {

      var connection: Connection = null

      try {
        Class.forName(externalizedConfigs.dbDriver)
        connection = DriverManager.getConnection(connectionString, externalizedConfigs.dbUname, externalizedConfigs.dbPasswd)

        val statement = connection.createStatement()
        val resultSet = statement.execute(sqlStmt)
        connection.close()
        resultSet
      } catch {
        case _: Throwable  =>
          externalizedConfigs.logger.error(s"Error occurred while running SQL command!!!")
          connection.close()
          false
      }
    }

    def findShardRecordById(shardId: String): Option[ShardIndexRecord] = {
      try {
        Class.forName(externalizedConfigs.dbDriver)
        val connection = DriverManager.getConnection(externalizedConfigs.dbDefaultConnStr, externalizedConfigs.dbUname, externalizedConfigs.dbPasswd)

        val statement = connection.prepareStatement(s"SELECT * FROM IndexShard WHERE shard_id=$shardId AND status=TRUE")
        val resultSet = statement.executeQuery
        if (resultSet.next()) {
          connection.close()
          Some(ShardIndexRecord(resultSet.getString("shard_id"), resultSet.getString("connection_string"), resultSet.getBoolean("status"), resultSet.getLong("created_date")))
        }
        None
      } catch {
        case _: Throwable  =>
          externalizedConfigs.logger.warn(s"No Shard Index Record Found!!!")
          None
      }
    }

    def findAvailableShardRecord(sqlStmt: String): Option[ShardIndexRecord] = {

      try {
        // make the connection
        Class.forName(externalizedConfigs.dbDriver)
        val connection = DriverManager.getConnection(externalizedConfigs.dbDefaultConnStr, externalizedConfigs.dbUname, externalizedConfigs.dbPasswd)

        // create the statement, and run specified sql
        val statement = connection.prepareStatement(findAvailableShardSqlStmt)
        val resultSet = statement.executeQuery
        if (resultSet.next()) {
          connection.close()
          Some(ShardIndexRecord(resultSet.getString("shard_id"), resultSet.getString("connection_string"), resultSet.getBoolean("status"), resultSet.getLong("created_date")))
        }
        None
      } catch {
        case _: Throwable  =>
          externalizedConfigs.logger.warn(s"No Shard Index Record Found!!!")
          None
      }
    }

    def findUserShardRecord(userId: String): Option[UserShardRecord] = {

      try {
        // make the connection
        Class.forName(externalizedConfigs.dbDriver)
        val connection = DriverManager.getConnection(externalizedConfigs.dbDefaultConnStr, externalizedConfigs.dbUname, externalizedConfigs.dbPasswd)

        // create the statement, and run specified sql
        val statement = connection.prepareStatement(s"SELECT * FROM UserShard WHERE user_id=$userId")
        val resultSet = statement.executeQuery
        if (resultSet.next()) {
          connection.close()
          Some(UserShardRecord(resultSet.getString("user_id"), resultSet.getString("shard_id")))
        }
        None
      } catch {
        case _: Throwable  =>
          externalizedConfigs.logger.warn(s"No Shard Index Record Found!!!")
          None
      }
    }

    def findDomainRecordById(sqlStmt: String, connectionString: String): Option[DomainShardRecord] = {

      try {
        Class.forName(externalizedConfigs.dbDriver)
        val connection = DriverManager.getConnection(connectionString, externalizedConfigs.dbUname, externalizedConfigs.dbPasswd)

        val statement = connection.prepareStatement(sqlStmt)
        val resultSet = statement.executeQuery
        while (resultSet.next()) {
          connection.close()
          Some(DomainShardRecord(resultSet.getString("user_id"), resultSet.getString("password"), resultSet.getString("userName")))
        }
        None
      } catch {
        case _: Throwable  =>
          externalizedConfigs.logger.warn(s"No Domain Record Found!!!")
          None
      }
    }


    def updateShardRecordById(shardId: String, userId: String): Boolean = {
      try {
        Class.forName(externalizedConfigs.dbDriver)
        val connection = DriverManager.getConnection(externalizedConfigs.dbDefaultConnStr, externalizedConfigs.dbUname, externalizedConfigs.dbPasswd)

        val statement = connection.createStatement
        val resultSet = statement.executeUpdate(s"INSERT INTO IndexShard VALUES ($userId, $shardId)")
        if (resultSet == 1)
          true
        else
          false
      } catch {
        case _: Throwable  =>
          externalizedConfigs.logger.warn(s"No Domain Record Found!!!")
          false
      }
    }

    def main(args: Array[String]): Unit = {

    }

  }

}

