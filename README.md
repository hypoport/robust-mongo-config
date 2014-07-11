robust-mongo-config
=============

Beispiel-Konfiguration MongoDB und Spring-Data um Ausfälle im Mongo-Cluster besser zu kompensieren.


## Starten eines Lokalen Mongo-Clusters

Starten von drei Mongo-Knoten

````
mongod --port 27001 --dbpath /srv/mongodb/rs0-1 --replSet rs0 --smallfiles --oplogSize 128
````

````
mongod --port 27002 --dbpath /srv/mongodb/rs0-2 --replSet rs0 --smallfiles --oplogSize 128
````

````
mongod --port 27003 --dbpath /srv/mongodb/rs0-3 --replSet rs0 --smallfiles --oplogSize 128
````

Initialisieren:

Siehe: http://docs.mongodb.org/manual/tutorial/deploy-replica-set/


## Test ausführen

src/test/java/de/hypoport/repository/JustARepositoryTest.java

Wenn der test läuft, kann der PRIMARY Mongo Server gekillt werden und die Daten werden weitergeschrieben.
Eine INFO-Meldung wird gelogged.