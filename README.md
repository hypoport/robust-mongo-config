robust-mongo-config
=============

Beispielkonfiguration des Java Mongo Treibers mit Spring-Data-MongoDB um Master Slave Switches eines Mongo Replication Sets zu kompensieren.


## Starten eines Lokalen Mongo-Clusters

Starten und Initialisieren von drei Mongo-Knoten:

```
mkdir -p /tmp/mongodb/rs0-1 /tmp/mongodb/rs0-2 /tmp/mongodb/rs0-3

mongod --port 27001 --replSet rs0 --smallfiles --oplogSize 128 --dbpath /tmp/mongodb/rs0-1
mongod --port 27002 --replSet rs0 --smallfiles --oplogSize 128 --dbpath /tmp/mongodb/rs0-2
mongod --port 27003 --replSet rs0 --smallfiles --oplogSize 128 --dbpath /tmp/mongodb/rs0-3

cat rs0.conf | mongo 127.0.0.1:27001
```

Die ReplicaSet Konfiguration ist in der Datei [rs0.conf](./rs0.conf) hinterlegt

Siehe auch http://docs.mongodb.org/manual/tutorial/deploy-replica-set/

Überprüfen der Konfiguration:

```
mongo 127.0.0.1:27001
rs.conf()
rs.status()
```

## Test ausführen

src/test/java/de/hypoport/repository/JustARepositoryTest.java

Wenn der test läuft, kann der PRIMARY Mongo Server gekillt werden. Die Daten unterbrechungsfrei weitergeschrieben. Eine INFO-Meldung wird gelogged.