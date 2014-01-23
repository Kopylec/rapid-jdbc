/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.kopylec.rapidjdbc;

import com.github.kopylec.rapidjdbc.annotation.Query;
import com.github.kopylec.rapidjdbc.annotation.Update;
import com.github.kopylec.rapidjdbc.exception.RapidJDBCException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An interceptor that manages SQL queries and updates. Should not be used in
 * clients code.
 *
 * @author Mariusz Kopylec
 * @since 1.0
 */
@Aspect
public class SQLAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(SQLAspect.class);

    @Around("target(repository) && execution(* *(..)) && @annotation(query) && @annotation(update)")
    public Object preventDuplicateSQL(Repository repository, ProceedingJoinPoint joinPoint, Query query, Update update) throws Throwable {
        throw new RapidJDBCException("Multiple SQLs on method " + joinPoint.getSignature().toShortString());
    }

    @Around("target(repository) && execution(* *(..)) && @annotation(query)")
    public Object executeSQLQuery(Repository repository, ProceedingJoinPoint joinPoint, Query query) throws Throwable {
        PreparedStatement statement = null;
        try {
            statement = prepareStatement(query.value(), joinPoint.getArgs());
            LOGGER.debug("Executing SQL query \"{}\" with parameters {}", query.value(), Arrays.toString(joinPoint.getArgs()));
            repository.setResultSet(statement.executeQuery());
            return joinPoint.proceed();
        } catch (RapidJDBCException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RapidJDBCException("Error executing query \"" + query.value() + "\" with parameters "
                    + Arrays.toString(joinPoint.getArgs()), ex);
        } finally {
            repository.closeResultSet();
            closeResources(statement);
        }
    }

    @Around("target(repository) && execution(* *(..)) && @annotation(update)")
    public Object executeSQLUpdate(Repository repository, ProceedingJoinPoint joinPoint, Update update) throws Throwable {
        PreparedStatement statement = null;
        try {
            statement = prepareStatement(update.value(), joinPoint.getArgs());
            Object result = joinPoint.proceed();
            LOGGER.debug("Executing SQL update \"{}\" with parameters {}", update.value(), Arrays.toString(joinPoint.getArgs()));
            statement.executeUpdate();
            return result;
        } catch (RapidJDBCException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RapidJDBCException("Error executing update \"" + update.value() + "\" with parameters "
                    + Arrays.toString(joinPoint.getArgs()), ex);
        } finally {
            closeResources(statement);
        }
    }

    private void closeResources(PreparedStatement statement) throws RapidJDBCException {
        if (statement != null) {
            LOGGER.trace("Closing prepared statement");
            try {
                statement.close();
            } catch (SQLException ex) {
                throw new RapidJDBCException("Error closing prepared statement", ex);
            }
        }
        if (!isTransactionActive()) {
            closeConnection();
        }
    }

    private PreparedStatement prepareStatement(String sql, Object... parameters) throws SQLException {
        Connection connection = getConnection();
        LOGGER.trace("Creating prepared statement from SQL \"{}\" and parameters {}", sql, Arrays.toString(parameters));
        PreparedStatement statement = connection.prepareStatement(sql);
        setStatementParameters(statement, parameters);

        return statement;
    }

    private void setStatementParameters(PreparedStatement statement, Object... parameters) throws SQLException {
        if (parameters != null) {
            for (int i = 0; i < parameters.length; i++) {
                Object parameter = parameters[i];
                if (parameter instanceof Date) {
                    statement.setTimestamp(i + 1, new Timestamp(((Date) parameter).getTime()));
                } else {
                    statement.setObject(i + 1, parameter);
                }
            }
        }
    }

    private Connection getConnection() throws RapidJDBCException {
        return JDBCContext.getInstance().getConnection();
    }

    private void closeConnection() throws RapidJDBCException {
        JDBCContext.getInstance().closeConnection();
    }

    private boolean isTransactionActive() throws RapidJDBCException {
        return JDBCContext.getInstance().isTransactionActive();
    }

    private SQLAspect() {
    }
}
