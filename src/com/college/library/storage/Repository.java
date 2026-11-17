package com.college.library.storage;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/** Generic storage contract; a JDBC implementation can replace the CSV one without touching the service layer. */
public interface Repository<T, ID> {
    List<T> findAll();
    Optional<T> findById(ID id);
    boolean existsById(ID id);
    void save(T entity) throws IOException;      // insert or update
    boolean deleteById(ID id) throws IOException;
}
