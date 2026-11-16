package com.sentinel.database;

import java.util.List;
import java.util.Optional;

/**
 * Generic CRUD (Create, Read, Update, Delete) Repository interface.
 * Demonstrates Abstraction and Java Generics for database operations.
 *
 * @param <T>  The entity type
 * @param <ID> The primary key identifier type
 */
public interface CrudRepository<T, ID> {

    /**
     * Create: Inserts a new record into the database.
     *
     * @param entity the entity to persist
     * @return the persisted entity
     * @throws DatabaseException if SQL or constraint error occurs
     */
    T save(T entity) throws DatabaseException;

    /**
     * Read: Retrieves an entity by its primary key identifier.
     *
     * @param id the primary key
     * @return Optional containing the entity if found, empty otherwise
     * @throws DatabaseException if SQL error occurs
     */
    Optional<T> findById(ID id) throws DatabaseException;

    /**
     * Read: Retrieves all entities in the table.
     *
     * @return List of all entities
     * @throws DatabaseException if SQL error occurs
     */
    List<T> findAll() throws DatabaseException;

    /**
     * Update: Modifies an existing entity in the database.
     *
     * @param entity the entity with updated fields
     * @return true if updated, false if record not found
     * @throws DatabaseException if SQL error occurs
     */
    boolean update(T entity) throws DatabaseException;

    /**
     * Delete: Removes a record by its identifier.
     *
     * @param id the primary key
     * @return true if removed, false if not found
     * @throws DatabaseException if SQL error occurs
     */
    boolean deleteById(ID id) throws DatabaseException;

    /**
     * Total count of records in the table.
     *
     * @return number of records
     * @throws DatabaseException if SQL error occurs
     */
    long count() throws DatabaseException;
}
