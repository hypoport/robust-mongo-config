package de.hypoport.mongo.template;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;
import com.mongodb.MongoException;
import com.mongodb.MongoTimeoutException;
import com.mongodb.WriteConcernException;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.data.mongodb.core.CollectionCallback;
import org.springframework.data.mongodb.core.DbCallback;
import org.springframework.data.mongodb.core.MongoExceptionTranslator;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.util.Assert;

import java.util.logging.Level;
import java.util.logging.Logger;

public class MongoTemplateWithRetry extends MongoTemplate {
  private static final Logger LOG = Logger.getLogger(MongoTemplateWithRetry.class.getName());

  static {
    java.util.logging.Logger.getLogger("com.mongodb").setLevel(java.util.logging.Level.SEVERE);
  }

  private final MongoExceptionTranslator exceptionTranslator = new MongoExceptionTranslator();
  private boolean retryEnabled = true;

  public MongoTemplateWithRetry(Mongo mongo, String databaseName) {
    super(mongo, databaseName);
  }

  public void setRetryEnabled(boolean retryEnabled) {
    this.retryEnabled = retryEnabled;
  }

  public void logMongoWarnings(boolean show) {
    java.util.logging.Logger.getLogger("com.mongodb").setLevel(show ? Level.WARNING : java.util.logging.Level.SEVERE);
  }

  @Override
  public <T> T execute(final String collectionName, final CollectionCallback<T> callback) {
    Assert.notNull(callback);

    try {
      final DBCollection collection = getAndPrepareCollection(getDb(), collectionName);
      if (retryEnabled) {
        return Retry.withRetry(() -> callback.doInCollection(collection));
      }
      return callback.doInCollection(collection);
    }
    catch (RuntimeException e) {
      throw potentiallyConvertRuntimeException(e);
    }
  }

  @Override
  public <T> T execute(final DbCallback<T> action) {
    Assert.notNull(action);

    try {
      final DB db = this.getDb();
      if (retryEnabled) {
        return Retry.withRetry(() -> action.doInDB(db));
      }
      return action.doInDB(db);
    }
    catch (RuntimeException e) {
      throw potentiallyConvertRuntimeException(e);
    }
  }

  private DBCollection getAndPrepareCollection(DB db, String collectionName) {
    try {
      DBCollection collection = db.getCollection(collectionName);
      prepareCollection(collection);
      return collection;
    }
    catch (RuntimeException e) {
      throw potentiallyConvertRuntimeException(e);
    }
  }

  private RuntimeException potentiallyConvertRuntimeException(RuntimeException ex) {
    RuntimeException resolved = this.exceptionTranslator.translateExceptionIfPossible(ex);
    return resolved == null ? ex : resolved;
  }

  private static class Retry {

    public static long MAX_RETRY_TIME_IN_MILLIS = 1000 * 60 * 1; // 1 Minute
    public static int RETRY_SLEEP_TIME_PER_INTERVAL = 1000;

    private static <T> T withRetry(DatabaseAction<T> databaseAction) {
      DateTime requestStartTime = new DateTime();
      MongoException lastException;
      do {
        try {
          return databaseAction.perform();
        }
        catch (MongoException.Network ex) {
          lastException = ex;
          handleFailoverError("Network issue: " + ex.getMessage());
        }
        catch (WriteConcernException ex) {
          String err = ex.getCommandResult().get("err").toString();
          if ("not master".equals(err)) {
            lastException = ex;
            handleFailoverError("No master in replica set found");
          }
          else {
            throw ex;
          }
        }
        catch (MongoTimeoutException ex) {
          if (StringUtils.startsWith(ex.getMessage(), "Timed out while waiting for a server")) {
            lastException = ex;
            handleFailoverError("Timed out while waiting for a master server");
          }
          else {
            throw ex;
          }
        }
        catch (MongoException ex) {
          if (StringUtils.startsWith(ex.getMessage(), "No replica set members available")) {
            lastException = ex;
            handleFailoverError("No replica set members available");
          }
          else {
            throw ex;
          }
        }
      } while (shouldRetry(requestStartTime));

      throw lastException;
    }

    private static void handleFailoverError(String errorMessage) {
      LOG.info(errorMessage);
      sleep();
    }

    private static void sleep() {
      try { Thread.sleep(RETRY_SLEEP_TIME_PER_INTERVAL); } catch (InterruptedException e) { /* go on */ }
    }

    static boolean shouldRetry(DateTime requestStartTime) {
      return requestStartTime.plus(MAX_RETRY_TIME_IN_MILLIS).isAfterNow();
    }

    public static interface DatabaseAction<T> {
      T perform();
    }
  }
}

