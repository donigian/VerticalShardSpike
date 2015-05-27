package scalatest

import org.scalatest.{GivenWhenThen, FeatureSpec, FunSuite}
import shard._
import shard.shardSpike.DomainRecord

/**
 * Created by arm on 5/26/15.
 */

class VerticalShardsTest extends FeatureSpec with GivenWhenThen  {

  feature("Read user data located in a shard somewhere") {

    scenario("View user record after metadata tables have been created") {
      Given("an array of userId primary keys")
      When("perform read for each userId")
      Then("locate shardId by userId & return User Data (DomainShard) by userId")
      assert(DomainRecord("","","") != None)
    }
  }

  feature("Write user data to a shard") {

    scenario("Create new user record, initially when no shard metadata tables exist") {
      Given("A User record")
      When("When write is called")
      Then("Create IndexShard, UserShard & DomainShard tables, insert DomainShard, update UserShard/IndexShard respectively")
    }

    scenario("Create new user record after metadata tables have been created") {
      Given("A User record")
      When("When write is called")
      Then("Insert DomainShard, update UserShard/IndexShard respectively")
    }

    /* API wouldn't be complete without support for Update */
    scenario("Update existing user record after metadata tables have been created") {
      Given("A User record")
      When("When write is called")
      Then("Update DomainShard, update UserShard/IndexShard respectively")
    }

    /* API wouldn't be complete without support for Delete */
    scenario("Delete existing user record after metadata tables have been created") {
      Given("A User record")
      When("When write is called")
      Then("Delete DomainShard, update UserShard/IndexShard respectively")
    }
  }

}
