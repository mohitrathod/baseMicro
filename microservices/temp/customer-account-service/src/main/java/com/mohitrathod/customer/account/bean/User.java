package com.mohitrathod.customer.account.bean;


import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

/**
 * Sample user class.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Mark Paluch
 */
@Data
@NoArgsConstructor
@Table(value = "users")
public class User {

    @PrimaryKey("user_id") private Long id;

    @Column("uname") private String username;
    @Column("fname") private String firstname;
    @Column("lname") private String lastname;

    public User(Long id) {
        this.setId(id);
    }
}
