package de.hypoport.mongo.template;

import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoException;
import com.mongodb.MongoTimeoutException;
import com.mongodb.WriteConcernException;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.CollectionCallback;
import org.springframework.data.mongodb.core.DbCallback;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoExceptionTranslator;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.util.Assert;

import java.util.logging.Level;
import java.util.logging.Logger;

public class MongoTemplateWithRetry extends MongoTemplate {

  private static final Logger LOG = Logger.getLogger(MongoTemplateWithRetry.class.getName());

  public static long MAX_RETRY_TIME_IN_MILLIS = 1000 * 60 * 1; // 1 Minute
  public static long RETRY_SLEEP_TIME_PER_INTERVAL = 1000;

  static {
    java.util.logging.Logger.getLogger("com.mongodb").setLevel(java.util.logging.Level.SEVERE);
  }

  private boolean retryEnabled = true;

  public MongoTemplateWithRetry(Mongo mongo, String databaseName) {
    super(mongo, databaseName);
  }

  public MongoTemplateWithRetry(MongoDbFactory dbFactory, MongoConverter mongoConverter) {
    super(dbFactory, mongoConverter);
  }


  public void setRetryEnabled(boolean retryEnabled) {
    this.retryEnabled = retryEnabled;
  }

  public void logMongoWarnings(boolean show) {
    java.util.logging.Logger.getLogger("com.mongodb").setLevel(show ? Level.WARNING : java.util.logging.Level.SEVERE);
  }

  // Overridden retryable database actions
  @Override
  public <T> T execute(final String collectionName, final CollectionCallback<T> callback) {
    Assert.notNull(callback);
    return retry().databaseAction(() -> MongoTemplateWithRetry.super.execute(collectionName, callback));
  }

  @Override
  public <T> T execute(final DbCallback<T> action) {
    Assert.notNull(action);
    return retry().databaseAction(() -> MongoTemplateWithRetry.super.execute(action));
  }

  @Override
  protected <T> T doFindAndModify(String collectionName, DBObject query, DBObject fields, DBObject sort, Class<T> entityClass, Update update, FindAndModifyOptions options) {
    return retry().databaseAction(() -> MongoTemplateWithRetry.super.doFindAndModify(collectionName, query, fields, sort, entityClass, update, options));
  }

  @Override
  protected <T> T doFindAndRemove(String collectionName, DBObject query, DBObject fields, DBObject sort, Class<T> entityClass) {
    return retry().databaseAction(() -> MongoTemplateWithRetry.super.doFindAndRemove(collectionName, query, fields, sort, entityClass));
  }

  @Override
  protected <T> T doFindOne(String collectionName, DBObject query, DBObject fields, Class<T> entityClass) {
    return retry().databaseAction(() -> MongoTemplateWithRetry.super.doFindOne(collectionName, query, fields, entityClass));
  }

  public Retry retry() {
    return new Retry();
  }

  private class Retry {

    private <T> T databaseAction(DatabaseAction<T> action) {
      DateTime requestStartTime = new DateTime();
      RuntimeException lastException;
      do {
        try {
          return action.perform();
        }
        catch (RetrieableDataAccessException retrieable) {
          lastException = retrieable;
          LOG.info(retrieable.getMessage());
          sleep();
        }
        catch (RuntimeException th) {
          lastException = th;
          throw th;
        }
      } while (shouldRetry(requestStartTime));

      LOG.throwing(MongoTemplateWithRetry.class.getSimpleName(), "databaseAction", lastException);
      throw lastException;
    }

    private  void sleep() {
      try { Thread.sleep(RETRY_SLEEP_TIME_PER_INTERVAL); } catch (InterruptedException e) { /* go on */ }
    }

    private boolean shouldRetry(DateTime requestStartTime) {
      return retryEnabled && requestStartTime.plus(MAX_RETRY_TIME_IN_MILLIS).isAfterNow();
    }
  }

  public static interface DatabaseAction<T> {
    T perform();
  }

  public static PersistenceExceptionTranslator getExceptionTranslator() {
    MongoExceptionTranslator translator = new MongoExceptionTranslator();

    return new PersistenceExceptionTranslator() {
      @Override
      public DataAccessException translateExceptionIfPossible(RuntimeException e) {
        try {
          throw e;
        }

        // Handle master/slave failover dependent exceptions
        catch (MongoException.Network ex) {
          return new RetrieableDataAccessException("Network issue: " + ex.getMessage(), ex);
        }

        catch (WriteConcernException ex) {
          String err = ex.getCommandResult().get("err").toString();
          if ("not master".equals(err)) {
            return new RetrieableDataAccessException("No master in replica set found", ex);
          }
        }

        catch (MongoTimeoutException ex) {
          if (StringUtils.startsWith(ex.getMessage(), "Timed out while waiting for a server")) {
            return new RetrieableDataAccessException("Timed out while waiting for a master server", ex);
          }
        }

        catch (MongoException ex) {
          if (StringUtils.startsWith(ex.getMessage(), "No replica set members available")) {
            return new RetrieableDataAccessException("No replica set members available", ex);
          }
        }

        // Other exceptions will be translated and thrown
        return translator.translateExceptionIfPossible(e);
      }
    };
  }

  static class RetrieableDataAccessException extends DataAccessException {

    public RetrieableDataAccessException(String msg) {
      super(msg);
    }

    public RetrieableDataAccessException(String msg, Throwable cause) {
      super(msg, cause);
    }
  }
}

