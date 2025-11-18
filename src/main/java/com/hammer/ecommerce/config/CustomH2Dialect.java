package com.hammer.ecommerce.config;

import org.hibernate.dialect.H2Dialect;

public class CustomH2Dialect extends H2Dialect {

    public CustomH2Dialect() {
        super();
    }

    @Override
    public boolean supportsInsertReturning() {
        return false;
    }

    @Override
    public boolean supportsInsertReturningGeneratedKeys() {
        return false;
    }
}