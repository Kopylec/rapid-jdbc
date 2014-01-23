package com.github.kopylec.rapidjdbc;

import com.github.kopylec.rapidjdbc.annotation.Column;
import com.github.kopylec.rapidjdbc.annotation.Transaction;
import com.github.kopylec.rapidjdbc.exception.RapidJDBCException;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A thread safe singleton context which handles references to resources needed
 * by RapidJDBC to work with underlaying database. Before starting any data
 * access operations you <b>must</b> set a data source and optionally entity
 * classes using the context.
 *
 * @author Mariusz Kopylec
 * @since 1.0
 */
public final class JDBCContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(JDBCContext.class);

    private DataSource dataSource;
    private final ThreadLocal<Connection> connection = new ThreadLocal<Connection>();
    private final ThreadLocal<Integer> numberOfTransactionOccurences = new ThreadLocal<Integer>();
    private final Map<Class, Map<String, Field>> entityFieldsByClass = new HashMap<Class, Map<String, Field>>();

    <E> E initializeEntity(Class<E> entityClass) throws RapidJDBCException {
        LOGGER.trace("Initializing entity of class {}", entityClass.getName());

        if (!entityFieldsByClass.containsKey(entityClass)) {
            throw new RapidJDBCException("Class (" + entityClass.getName() + ") has not been added to contexts entity classes");
        }
        try {
            return entityClass.newInstance();
        } catch (IllegalAccessException ex) {
            throw new RapidJDBCException("Error initializing entity of class " + entityClass.getName(), ex);
        } catch (InstantiationException ex) {
            throw new RapidJDBCException("Error initializing entity of class " + entityClass.getName(), ex);
        }
    }

    Connection getConnection() throws RapidJDBCException {
        LOGGER.debug("Getting connection to database");

        if (connection.get() == null) {
            LOGGER.debug("No active database connection found");
            createConnection();
        }
        return connection.get();
    }

    void closeConnection() throws RapidJDBCException {
        if (connection.get() != null) {
            LOGGER.debug("Closing active database connection");
            try {
                connection.get().close();
                connection.remove();
            } catch (SQLException ex) {
                throw new RapidJDBCException("Error closing database connection", ex);
            }
        }
    }

    void beginTransaction(TransactionLevel level) throws RapidJDBCException {
        try {
            createConnection();
            LOGGER.debug("Beginning JDBC transaction with {} isolation level", level);
            connection.get().setAutoCommit(false);
            if (level != TransactionLevel.DEFAULT) {
                connection.get().setTransactionIsolation(level.getLevel());
            }
        } catch (SQLException ex) {
            throw new RapidJDBCException("Error beginning JDBC transaction", ex);
        }
    }

    void initTransactionOccurences() {
        LOGGER.trace("{} occurence found, initializing transaction occurences counter", Transaction.class.getName());
        numberOfTransactionOccurences.set(1);
    }

    void addTransactionOccurence() {
        LOGGER.trace("{} occurence found, incrementing transaction occurences counter", Transaction.class.getName());
        numberOfTransactionOccurences.set(numberOfTransactionOccurences.get() + 1);
    }

    void removeTransactionOccurence() {
        LOGGER.trace("Decrementing transaction occurences counter", Transaction.class.getName());
        numberOfTransactionOccurences.set(numberOfTransactionOccurences.get() - 1);
    }

    void clearTransactionOccurences() {
        LOGGER.trace("Clearing transaction occurences counter", Transaction.class.getName());
        numberOfTransactionOccurences.remove();
    }

    boolean hasTransactionBeEnded() {
        return numberOfTransactionOccurences.get() == 1;
    }

    void commitTransaction() throws RapidJDBCException {
        if (connection.get() != null) {
            LOGGER.debug("Committing JDBC transaction");
            try {
                connection.get().commit();
            } catch (SQLException ex) {
                throw new RapidJDBCException("Error committing JDBC transaction", ex);
            } finally {
                enableAutoCommit();
            }
        }
    }

    void rollbackTransaction() throws RapidJDBCException {
        if (connection.get() != null) {
            LOGGER.debug("Rollbacking JDBC transaction");
            try {
                connection.get().rollback();
            } catch (SQLException ex) {
                throw new RapidJDBCException("Error rollbacking JDBC transaction", ex);
            } finally {
                enableAutoCommit();
            }
        }
    }

    Map<String, Field> getEntityFieldsByColumns(Class entityClass) {
        LOGGER.trace("Getting entity fields annotated with {} from cache. Entity class: {}", Column.class.getName(), entityClass.getName());

        return entityFieldsByClass.get(entityClass);
    }

    DataSource getDataSource() {
        LOGGER.trace("Getting data source from context");

        return dataSource;
    }

    /**
     * @param dataSource data source used for communication with database
     * @throws RapidJDBCException
     */
    public final void setDataSource(DataSource dataSource) throws RapidJDBCException {
        LOGGER.debug("Setting data source");

        if (dataSource == null) {
            throw new RapidJDBCException("Data source is null");
        }
        if (this.dataSource != null) {
            throw new RapidJDBCException("Data source already set");
        }
        this.dataSource = dataSource;
    }

    boolean isTransactionActive() throws RapidJDBCException {
        if (connection.get() != null) {
            try {
                LOGGER.trace("Checking if JDBC transaction is active");
                return !connection.get().getAutoCommit();
            } catch (SQLException ex) {
                throw new RapidJDBCException("Error checking if JDBC transaction is active", ex);
            }
        }
        return false;
    }

    /**
     * Sets entity types. Entity is any class with fields annotated with
     * {@link com.github.kopylec.rapidjdbc.annotation.Column}.
     *
     * @param classes types of entities
     * @throws RapidJDBCException
     */
    public final void setEntityClasses(Class... classes) throws RapidJDBCException {
        LOGGER.debug("Setting entity classes: {}", Arrays.toString(classes));

        if (classes == null) {
            throw new RapidJDBCException("Entity classes are null");
        }
        if (!entityFieldsByClass.isEmpty()) {
            throw new RapidJDBCException("Entity classes already set");
        }
        for (Class clazz : classes) {
            if (clazz != null) {
                addEntityFieldsByClass(clazz);
            }
        }
    }

    private void addEntityFieldsByClass(Class entityClass) {
        LOGGER.trace("Caching entity fields annotated with {}. Entity class: {}", Column.class.getName(), entityClass.getName());

        Map<String, Field> fieldsByColumns = new HashMap<String, Field>();
        Field[] fields = entityClass.getDeclaredFields();

        for (Field field : fields) {
            Column column = field.getAnnotation(Column.class);
            if (column != null) {
                LOGGER.trace("Annotation {} with value \"{}\" found on field \"{}\"", Column.class.getName(), column.value(), field.getName());
                field.setAccessible(true);
                fieldsByColumns.put(column.value(), field);
            }
        }
        entityFieldsByClass.put(entityClass, fieldsByColumns);
    }

    private void createConnection() throws RapidJDBCException {
        LOGGER.debug("Creating connection from data source");

        try {
            connection.set(dataSource.getConnection());
        } catch (SQLException ex) {
            throw new RapidJDBCException("Error creating connection to database", ex);
        }
    }

    private void enableAutoCommit() throws RapidJDBCException {
        if (connection.get() != null) {
            LOGGER.trace("Enabling auto commit on database connection");
            try {
                connection.get().setAutoCommit(true);
            } catch (SQLException ex) {
                throw new RapidJDBCException("Error enabling auto commit", ex);
            }
        }
    }

    private JDBCContext() {
        LOGGER.debug("Creating context");
    }

    public static JDBCContext getInstance() {
        return JDBCContextHolder.INSTANCE;
    }

    private static class JDBCContextHolder {

        private static final JDBCContext INSTANCE = new JDBCContext();
    }
}
