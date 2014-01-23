package com.github.kopylec.rapidjdbc;

import com.github.kopylec.rapidjdbc.exception.RapidJDBCException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class that <b>must be extended</b> by every class with data access
 * methods. Data access methods must be annotated either by
 * {@link com.github.kopylec.rapidjdbc.annotation.Query} or by
 * {@link com.github.kopylec.rapidjdbc.annotation.Update}
 *
 * @author Mariusz Kopylec
 * @since 1.0
 */
public abstract class Repository {

    private static final Logger LOGGER = LoggerFactory.getLogger(Repository.class);

    private final ThreadLocal<ResultSet> resultSet = new ThreadLocal<ResultSet>();
    private final ThreadLocal<String[]> resultSetColumns = new ThreadLocal<String[]>();

    public Repository() throws RapidJDBCException {
        LOGGER.trace("Creating repository of class {}", getClass().getName());

        if (getDataSource() == null) {
            throw new RapidJDBCException("Datasource has not been set");
        }
    }

    /**
     * Creates an entity from result set current row. Only fields annotated by
     * {@link com.github.kopylec.rapidjdbc.annotation.Column} will be set.
     *
     * @param <E>
     * @param entityClass type of entity
     * @return entity
     * @throws RapidJDBCException
     */
    protected final <E> E createEntity(Class<E> entityClass) throws RapidJDBCException {
        LOGGER.trace("Creating entity of class {}", entityClass.getName());

        E entity = initializeEntity(entityClass);
        Map<String, Field> entityFieldsByColumns = getEntityFieldsByColumns(entityClass);

        for (String column : getResultSetColumns()) {
            try {
                Field field = entityFieldsByColumns.get(column);
                if (field != null) {
                    field.set(entity, getObject(column));
                }
            } catch (Exception ex) {
                throw new RapidJDBCException("Error setting entity field.\n"
                        + "Entity class: " + entityClass.getName() + "\n"
                        + "Entity field name: " + entityFieldsByColumns.get(column) + "\n"
                        + "Result set column: " + column + "\n"
                        + "Result set column value: " + getObject(column), ex);
            }
        }

        return entity;
    }

    /**
     * Moves result set cursor to the next row.
     *
     * @return {@code true} if the new current row is valid; {@code false} if
     * there are no more rows
     * @throws RapidJDBCException
     */
    protected final boolean nextRow() throws RapidJDBCException {
        LOGGER.trace("Moving result set cursor to the next row");

        try {
            return getResultSet().next();
        } catch (SQLException ex) {
            throw new RapidJDBCException("Error moving cursor forward in result set", ex);
        }
    }

    /**
     * Gets the {@link java.sql.ResultSet} with a cursor set to row that is
     * currently being processed.
     *
     * @return a result set created from SQL query
     * @throws RapidJDBCException if no query has been executed or result set
     * has been closed; if a database error occurs
     */
    protected final ResultSet getResultSet() throws RapidJDBCException {
        ResultSet rs = resultSet.get();
        if (rs == null) {
            throw new RapidJDBCException("No query has been executed or result set has been closed");
        }
        return rs;
    }

    /**
     * Checks if the specified column value from the result set is null.
     *
     * @param column result set column name
     * @return {@code true} if value is null, otherwise {@code false}
     * @throws RapidJDBCException if the column name is not valid; if a database
     * access error occurs or this method is called on a closed result set
     */
    protected final boolean isNull(String column) throws RapidJDBCException {
        LOGGER.trace("Checking if result set column \"{}\" has null value", column);

        return getObject(column) == null;
    }

    /**
     * Checks if the specified column value from the result set is not null.
     *
     * @param column result set column name
     * @return {@code true} if value is not null, otherwise {@code false}
     * @throws RapidJDBCException if the column name is not valid; if a database
     * access error occurs or this method is called on a closed result set
     */
    protected final boolean isNotNull(String column) throws RapidJDBCException {
        LOGGER.trace("Checking if result set column \"{}\" has non-null value", column);

        return !isNull(column);
    }

    /**
     * Gets an integer value of the specified column from the result set.
     *
     * @param column result set column name
     * @return an integer value
     * @throws RapidJDBCException if the column name is not valid; if a database
     * access error occurs or this method is called on a closed result set
     */
    protected final Integer getInteger(String column) throws RapidJDBCException {
        LOGGER.trace("Getting result set column \"{}\" integer value", column);

        try {
            return getObject(column) == null ? null : getResultSet().getInt(column);
        } catch (SQLException ex) {
            throw createRapidJDBCExceptionForColumn(column, ex);
        }
    }

    /**
     * Gets a string value of the specified column from the result set.
     *
     * @param column result set column name
     * @return a string value
     * @throws RapidJDBCException if the column name is not valid; if a database
     * access error occurs or this method is called on a closed result set
     */
    protected final String getString(String column) throws RapidJDBCException {
        LOGGER.trace("Getting result set column \"{}\" string value", column);

        try {
            return getResultSet().getString(column);
        } catch (SQLException ex) {
            throw createRapidJDBCExceptionForColumn(column, ex);
        }
    }

    /**
     * Gets an object value of the specified column from the result set.
     *
     * @param column result set column name
     * @return an object value
     * @throws RapidJDBCException if the column name is not valid; if a database
     * access error occurs or this method is called on a closed result set
     */
    protected final Object getObject(String column) throws RapidJDBCException {
        LOGGER.trace("Getting result set column \"{}\" value", column);

        try {
            return getResultSet().getObject(column);
        } catch (SQLException ex) {
            throw createRapidJDBCExceptionForColumn(column, ex);
        }
    }

    /**
     * Gets a date value of the specified column from the result set.
     *
     * @param column result set column name
     * @return a date value
     * @throws RapidJDBCException if the column name is not valid; if a database
     * access error occurs or this method is called on a closed result set
     */
    protected final Date getDate(String column) throws RapidJDBCException {
        LOGGER.trace("Getting result set column \"{}\" date value", column);

        try {
            return getResultSet().getDate(column);
        } catch (SQLException ex) {
            throw createRapidJDBCExceptionForColumn(column, ex);
        }
    }

    /**
     * Gets a double value of the specified column from the result set.
     *
     * @param column result set column name
     * @return a double value
     * @throws RapidJDBCException if the column name is not valid; if a database
     * access error occurs or this method is called on a closed result set
     */
    protected final Double getDouble(String column) throws RapidJDBCException {
        LOGGER.trace("Getting result set column \"{}\" double value", column);

        try {
            return getObject(column) == null ? null : getResultSet().getDouble(column);
        } catch (SQLException ex) {
            throw createRapidJDBCExceptionForColumn(column, ex);
        }
    }

    /**
     * Gets a boolean value of the specified column from the result set.
     *
     * @param column result set column name
     * @return a boolean value
     * @throws RapidJDBCException if the column name is not valid; if a database
     * access error occurs or this method is called on a closed result set
     */
    protected final Boolean getBoolean(String column) throws RapidJDBCException {
        LOGGER.trace("Getting result set column \"{}\" boolean value", column);

        try {
            return getObject(column) == null ? null : getResultSet().getBoolean(column);
        } catch (SQLException ex) {
            throw createRapidJDBCExceptionForColumn(column, ex);
        }
    }

    /**
     * Gets a big decimal value of the specified column from the result set.
     *
     * @param column result set column name
     * @return a big decimal value
     * @throws RapidJDBCException if the column name is not valid; if a database
     * access error occurs or this method is called on a closed result set
     */
    protected final BigDecimal getBigDecimal(String column) throws RapidJDBCException {
        LOGGER.trace("Getting result set column \"{}\" big decimal value", column);

        try {
            return getResultSet().getBigDecimal(column);
        } catch (SQLException ex) {
            throw createRapidJDBCExceptionForColumn(column, ex);
        }
    }

    /**
     * Gets a long value of the specified column from the result set.
     *
     * @param column result set column name
     * @return a long value
     * @throws RapidJDBCException if the column name is not valid; if a database
     * access error occurs or this method is called on a closed result set
     */
    protected final Long getLong(String column) throws RapidJDBCException {
        LOGGER.trace("Getting result set column \"{}\" long value", column);

        try {
            return getObject(column) == null ? null : getResultSet().getLong(column);
        } catch (SQLException ex) {
            throw createRapidJDBCExceptionForColumn(column, ex);
        }
    }

    /**
     * Gets a timestamp value of the specified column from the result set.
     *
     * @param column result set column name
     * @return a timestamp value
     * @throws RapidJDBCException if the column name is not valid; if a database
     * access error occurs or this method is called on a closed result set
     */
    protected final Timestamp getTimestamp(String column) throws RapidJDBCException {
        LOGGER.trace("Getting result set column \"{}\" timestamp value", column);

        try {
            return getResultSet().getTimestamp(column);
        } catch (SQLException ex) {
            throw createRapidJDBCExceptionForColumn(column, ex);
        }
    }

    void setResultSet(ResultSet resultSet) {
        LOGGER.trace("Preparing result set");

        this.resultSet.set(resultSet);
    }

    void closeResultSet() throws SQLException {
        if (resultSetColumns.get() != null) {
            LOGGER.trace("Clearing cached result set column names");
            resultSetColumns.remove();
        }
        ResultSet rs = resultSet.get();
        if (rs != null) {
            LOGGER.trace("Closing result set");
            rs.close();
            resultSet.remove();
        }
    }

    private String[] getResultSetColumns() throws RapidJDBCException {
        LOGGER.trace("Getting result set column names from cache");

        String[] columns = resultSetColumns.get();
        if (columns != null) {
            return columns;
        }

        LOGGER.trace("No cached result set column names found");
        LOGGER.trace("Initializing result set column names cache from meta data");

        try {
            ResultSetMetaData metaData = getResultSet().getMetaData();
            int columnCount = metaData.getColumnCount();
            columns = new String[columnCount];

            for (int i = 0; i < columnCount; i++) {
                columns[i] = metaData.getColumnName(i + 1);
            }
            LOGGER.trace("Caching result set columns {}", Arrays.toString(columns));
            resultSetColumns.set(columns);

            return columns;
        } catch (SQLException ex) {
            throw new RapidJDBCException("Error getting column names from result set", ex);
        }
    }

    private RapidJDBCException createRapidJDBCExceptionForColumn(String column, SQLException ex) {
        return new RapidJDBCException("Error getting value from column \"" + column + "\"", ex);
    }

    private DataSource getDataSource() {
        return JDBCContext.getInstance().getDataSource();
    }

    private Map<String, Field> getEntityFieldsByColumns(Class entityClass) {
        return JDBCContext.getInstance().getEntityFieldsByColumns(entityClass);
    }

    private <E> E initializeEntity(Class<E> entityClass) throws RapidJDBCException {
        return JDBCContext.getInstance().initializeEntity(entityClass);
    }
}
