package com.github.kopylec.rapidjdbc;

import java.sql.Connection;

/**
 * A transaction isolation level which represents {@code java.sql.Connection}
 * transaction constants.
 *
 * @author Mariusz Kopylec
 * @since 1.0
 * @see Connection
 */
public enum TransactionLevel {

    /**
     * Database default transaction isolation level.
     */
    DEFAULT(-1),
    /**
     * {@link Connection#TRANSACTION_NONE}
     */
    NONE(Connection.TRANSACTION_NONE),
    /**
     * {@link Connection#TRANSACTION_READ_COMMITTED}
     */
    READ_COMMITTED(Connection.TRANSACTION_READ_COMMITTED),
    /**
     * {@link Connection#TRANSACTION_READ_UNCOMMITTED}
     */
    READ_UNCOMMITTED(Connection.TRANSACTION_READ_UNCOMMITTED),
    /**
     * {@link Connection#TRANSACTION_REPEATABLE_READ}
     */
    REPEATABLE_READ(Connection.TRANSACTION_REPEATABLE_READ),
    /**
     * {@link Connection#TRANSACTION_SERIALIZABLE}
     */
    SERIALIZABLE(Connection.TRANSACTION_SERIALIZABLE);
    private final int level;

    private TransactionLevel(int level) {
        this.level = level;
    }

    int getLevel() {
        return level;
    }
}
