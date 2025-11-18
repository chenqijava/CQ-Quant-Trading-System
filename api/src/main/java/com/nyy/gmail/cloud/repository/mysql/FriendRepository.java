package com.nyy.gmail.cloud.repository.mysql;

import com.nyy.gmail.cloud.entity.mysql.Friend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface FriendRepository extends JpaRepository<Friend, String>, JpaSpecificationExecutor<Friend> {

    List<Friend> findByAccIdEquals(String accId);

    List<Friend> findByPhoneEquals(String phone);

    List<Friend> findByPhoneEqualsAndAccIdEquals(String phone, String phone1);

    List<Friend> findByUidEqualsAndAccIdEquals(String uid, String uid1);
}
