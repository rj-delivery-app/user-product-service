package com.example.userproduct.utils;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.sql.ResultSet;
import java.sql.Statement;

@Slf4j
public class IdGenerator implements IdentifierGenerator {
    @Override
    public Object generate(SharedSessionContractImplementor session, Object o) {
        String prefix = "ORD-"; // Custom aggregate prefix

        log.debug(prefix, o);
        // Fetch next sequence value using JDBC transaction context
        return session.doReturningWork(connection -> {
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("SELECT nextval('order_id_seq')")) {

                if (resultSet.next()) {
                    long id = resultSet.getLong(1);
                    return prefix + String.format("%05d", id); // Results in: ORD-00001
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to generate custom ID sequential aggregation", e);
            }
            return null;
        });
    }
}
