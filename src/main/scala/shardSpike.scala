package shard

/**
 * Created by arm on 5/26/15.
 */
object shardSpike {

  case class ShardIndexRecord(shardId: String, connectionString: String, status: Boolean, createdDate: Long)
  case class UserShardRecord(userId: String, shardId: String)
  case class DomainShardRecord(userId: String, password: String, userName: String)

  val sqlCreateIndexShardTableStmt =
    """
          CREATE TABLE IndexShard(
            shard_id INT PRIMARY KEY,
            connection_string TEXT UNIQUE NOT NULL,
            status boolean NOT NULL,
            created_date TIMESTAMP NOT NULL
          );
    """.stripMargin

  val sqlCreateUserShardTableStmt =
    """
          CREATE TABLE UserShard(
            user_id INT PRIMARY KEY,
            shard_id REFERENCES IndexShard
          );
    """.stripMargin

  val sqlCreateDomainShardTableStmt =
    """
          CREATE TABLE DomainShard(
            user_id INT REFERENCES UserShard,
            password TEXT UNIQUE NOT NULL,
            user_name TEXT UNIQUE NOT NULL
          );
    """.stripMargin

  val sqlDomainShardTableExistsStmt =
    """
      SELECT EXISTS (
        SELECT 1
      FROM   information_schema.tables
      AND    table_name = 'DomainShard'
        );
    """.stripMargin

  val findAvailableShardSqlStmt =
    """
       SELECT * FROM IndexShard WHERE status=TRUE LIMIT 1;
    """.stripMargin

  var ShardMetadataTableCreated = false

}
