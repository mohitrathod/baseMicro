package com.mohitrathod.customer.account.repository;



import java.util.List;

import com.mohitrathod.customer.account.bean.User;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.repository.CrudRepository;

/**
 * Simple repository interface for {@link User} instances. The interface is used to declare so called query methods,
 * methods to retrieve single entities or collections of them.
 *
 * @author Thomas Darimont
 */
public interface BasicUserRepository extends CrudRepository<User, Long> {

    /**
     * Sample method annotated with {@link Query}. This method executes the CQL from the {@link Query} value.
     *
     * @param id
     * @return
     */
    @Query("SELECT * from users where user_id in(?0)")
    User findUserByIdIn(long id);

    /**
     * Derived query method. This query corresponds with {@code SELECT * FROM users WHERE uname = ?0}.
     * {@link User#username} is not part of the primary so it requires a secondary index.
     *
     * @param username
     * @return
     */
    User findUserByUsername(String username);

    /**
     * Derived query method using SASI (SSTable Attached Secondary Index) features through the {@code LIKE} keyword. This
     * query corresponds with {@code SELECT * FROM users WHERE lname LIKE '?0'}. {@link User#lastname} is not part of the
     * primary key so it requires a secondary index.
     *
     * @param lastnamePrefix
     * @return
     */
    List<User> findUsersByLastnameStartsWith(String lastnamePrefix);
}