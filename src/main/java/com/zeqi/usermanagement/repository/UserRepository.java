package com.zeqi.usermanagement.repository;

import com.zeqi.usermanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    User findByUserName(String userName);

    boolean existsByUserName(String userName);

}
