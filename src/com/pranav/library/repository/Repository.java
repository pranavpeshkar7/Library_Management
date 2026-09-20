package com.pranav.library.repository;

import java.util.List;
import java.util.Optional;

/**
 * Hand-rolled generic repository contract — deliberately the same shape as
 * Spring Data's JpaRepository<T, ID>, but with no framework behind it:
 * every method here is implemented by hand with plain JDBC.
 */
public interface Repository<T, ID> {
    T save(T entity) throws Exception;
    Optional<T> findById(ID id) throws Exception;
    List<T> findAll() throws Exception;
    void deleteById(ID id) throws Exception;
}
