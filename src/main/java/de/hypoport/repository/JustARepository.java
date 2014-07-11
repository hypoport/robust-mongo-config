package de.hypoport.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JustARepository extends MongoRepository<SomeEntity, String> {


}



