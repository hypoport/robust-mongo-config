package de.hypoport.mongo.config;

import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.ReadPreference;
import de.hypoport.mongo.template.MongoTemplateWithRetry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import javax.inject.Inject;
import java.net.UnknownHostException;

import static com.mongodb.MongoClientOptions.builder;
import static com.mongodb.WriteConcern.ACKNOWLEDGED;
import static java.util.concurrent.TimeUnit.SECONDS;

@Configuration
@EnableMongoRepositories(basePackages = "de.hypoport.repository")
public class MongoConfiguration extends AbstractMongoConfiguration {

  @Value("${mongo.uri}")
  String mongoUri;

  @Inject
  Mongo mongo;

  @Override
  protected String getDatabaseName() {
    return "mongostress";
  }

  @Override
  @Bean
  public Mongo mongo() throws UnknownHostException {
    return new MongoClient(new MongoClientURI(mongoUri, builder()
        .connectTimeout((int) SECONDS.toMillis(5))
        .socketTimeout((int) SECONDS.toMillis(10))
        .connectionsPerHost(100)
        .threadsAllowedToBlockForConnectionMultiplier(50)
        .readPreference(ReadPreference.primaryPreferred())
        .writeConcern(ACKNOWLEDGED)
    ));
  }

  @Override
  @Bean
  public MongoTemplate mongoTemplate() throws Exception {
    MongoTemplateWithRetry mongoTemplateWithRetry = new MongoTemplateWithRetry(mongo, getDatabaseName());
    mongoTemplateWithRetry.setRetryEnabled(true);
    mongoTemplateWithRetry.logMongoWarnings(false);
    return mongoTemplateWithRetry;
  }
}

