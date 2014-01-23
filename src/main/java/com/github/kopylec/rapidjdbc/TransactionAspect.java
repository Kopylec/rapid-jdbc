package com.github.kopylec.rapidjdbc;

import com.github.kopylec.rapidjdbc.annotation.Transaction;
import com.github.kopylec.rapidjdbc.exception.RapidJDBCException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * An interceptor that manages JDBC transactions. Should not be used in clients
 * code.
 *
 * @author Mariusz Kopylec
 * @since 1.0
 */
@Aspect
public final class TransactionAspect {

    @Around("execution(* *(..)) && @annotation(transaction)")
    public Object wrapWithTransaction(ProceedingJoinPoint joinPoint, Transaction transaction) throws Throwable {
        try {
            if (!isTransactionActive()) {
                beginTransaction(transaction);
                initTransactionOccurences();
            } else {
                addTransactionOccurence();
            }
            Object result = joinPoint.proceed();
            if (hasTransactionBeEnded()) {
                commit();
            }
            return result;
        } catch (Exception ex) {
            if (hasTransactionBeEnded()) {
                rollback();
            }
            throw ex;
        } finally {
            if (hasTransactionBeEnded()) {
                clearTransactionOccurences();
                closeConnection();
            } else {
                removeTransactionOccurence();
            }
        }
    }

    private void initTransactionOccurences() {
        JDBCContext.getInstance().initTransactionOccurences();
    }

    private void clearTransactionOccurences() {
        JDBCContext.getInstance().clearTransactionOccurences();
    }

    private void removeTransactionOccurence() {
        JDBCContext.getInstance().removeTransactionOccurence();
    }

    private boolean hasTransactionBeEnded() {
        return JDBCContext.getInstance().hasTransactionBeEnded();
    }

    private void addTransactionOccurence() {
        JDBCContext.getInstance().addTransactionOccurence();
    }

    private void beginTransaction(Transaction transaction) throws RapidJDBCException {
        JDBCContext.getInstance().beginTransaction(transaction.isolation());
    }

    private void commit() throws RapidJDBCException {
        JDBCContext.getInstance().commitTransaction();
    }

    private void rollback() throws RapidJDBCException {
        JDBCContext.getInstance().rollbackTransaction();
    }

    private void closeConnection() throws RapidJDBCException {
        JDBCContext.getInstance().closeConnection();
    }

    private boolean isTransactionActive() throws RapidJDBCException {
        return JDBCContext.getInstance().isTransactionActive();
    }

    private TransactionAspect() {
    }
}
