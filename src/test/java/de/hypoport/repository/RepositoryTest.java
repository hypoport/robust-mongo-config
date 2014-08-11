package de.hypoport.repository;

import de.hypoport.config.PropertiesConfiguration;
import de.hypoport.mongo.config.MongoConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import static java.lang.Thread.sleep;
import static org.fest.assertions.Assertions.assertThat;

@ContextConfiguration(classes = {PropertiesConfiguration.class, MongoConfiguration.class})
public class RepositoryTest extends AbstractTestNGSpringContextTests {

  @Inject
  @Named(value = "justARepository")
  JustARepository repository;

  @BeforeClass
  @AfterClass
  public void cleanup() throws Exception {
    repository.deleteAll();
  }

  /**
   * Manually kill the mongo primary in you replication set.
   */
  @Test
  public void long_running_insertion_test() throws Exception {
    for (int i = 1; i <= 10000; i++) {
      SomeEntity entity = new SomeEntity();
      entity.setNumber(i);
      entity.setName("Datensatz " + i);
      entity.setContent("Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet.");
      SomeEntity saved = repository.save(entity);

      assertThat(saved.getId()).isNotNull();
      assertThat(repository.count()).isEqualTo(i);

      sleep(new Random().nextInt(500));
    }
  }

  @Test
  public void long_run_findOne_Test() throws Exception {
    SomeEntity entity = new SomeEntity();
    entity.setNumber(4711);
    entity.setName("Datensatz " + 4711);
    entity.setContent("Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet.");
    String id = repository.save(entity).getId();

    for (int i = 1; i <= 10000; i++) {
      SomeEntity found = repository.findOne(id);

      assertThat(found.getId()).isEqualTo(id);
      assertThat(found.getName()).isEqualTo("Datensatz " + 4711);
      sleep(new Random().nextInt(500));
    }
  }
}