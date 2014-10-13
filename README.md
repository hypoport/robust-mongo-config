robust-mongo-config
=============

Example configuration of the Java MongoDB driver (<http://docs.mongodb.org/ecosystem/drivers/java/>)
with Spring Data MongoDB (<http://projects.spring.io/spring-data-mongodb/>)
to make the application more robust when a master slave switchover occures.
The MongoTemplateWithRetry retries the most common database operations for three
times. MongoDB specific exections will be catched and logged as warn. Only after the
retry period the exeption will be thown as translated Spring database exception.

## Starting a local MongoDB-Clusters for testing

Starting and initializing of three Mongo-Nodes:

```
mkdir -p /tmp/mongodb/rs0-1 /tmp/mongodb/rs0-2 /tmp/mongodb/rs0-3

mongod --port 27001 --replSet rs0 --smallfiles --oplogSize 128 --dbpath /tmp/mongodb/rs0-1
mongod --port 27002 --replSet rs0 --smallfiles --oplogSize 128 --dbpath /tmp/mongodb/rs0-2
mongod --port 27003 --replSet rs0 --smallfiles --oplogSize 128 --dbpath /tmp/mongodb/rs0-3

cat rs0.conf | mongo 127.0.0.1:27001
```

The ReplicaSet Configuration is saved in the file [rs0.conf](./rs0.conf)

Have a look at <http://docs.mongodb.org/manual/tutorial/deploy-replica-set/>

Check configuration:

```
mongo 127.0.0.1:27001
rs.conf()
rs.status()
```

## run tests

src/test/java/de/hypoport/repository/JustARepositoryTest.java

When the test is running kill the primary Mongo node. The data should be written without
break in case of unsteady replica set state.
An information message should be logged.
