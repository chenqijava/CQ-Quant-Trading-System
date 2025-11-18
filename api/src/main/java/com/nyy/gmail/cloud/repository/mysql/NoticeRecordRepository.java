package com.nyy.gmail.cloud.repository.mysql;

import com.nyy.gmail.cloud.entity.mysql.NoticeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface NoticeRecordRepository extends JpaRepository<NoticeRecord, String>, JpaSpecificationExecutor<NoticeRecord> {

    List<NoticeRecord> findByStatusEquals(String status);

    List<NoticeRecord> findByStatusEqualsAndTimeIntervalIn(String status, List<String> timerInterval);
}
