package com.github.kopylec.rapidjdbc.annotation;

import com.github.kopylec.rapidjdbc.TransactionLevel;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated should run inside JDBC transaction. Nested
 * transactional methods will run inside single, largest scoped transaction.
 *
 * @author Mariusz Kopylec
 * @since 1.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Transaction {

    /**
     * @return JDBC transaction isolation level, if not provided the default
     * level will be used
     */
    TransactionLevel isolation() default TransactionLevel.DEFAULT;
}
