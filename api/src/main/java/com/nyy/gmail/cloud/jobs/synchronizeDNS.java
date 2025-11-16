package com.nyy.gmail.cloud.jobs;

import com.nyy.gmail.cloud.entity.mysql.DnsRecord;
import com.nyy.gmail.cloud.repository.mysql.DnsRecordRepository;
import com.nyy.gmail.cloud.utils.DnsRecordUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class synchronizeDNS {
    @Autowired
    DnsRecordRepository dnsRecordRepository;

    @Value("${application.taskType}")
    private String taskType;

    /**
     * 同步dns域名映射记录
     */
    @Async("other")
    @Scheduled(cron = "0 0 * * * ?")
    public void SynchronizeDNS(){

        if (taskType.equals("googleai")) {
            return;
        }

        // 获取所有的dns域名映射
        String zoneId = DnsRecordUtils.getZoneId();
        String dnsResult = DnsRecordUtils.listDnsRecord(zoneId,1,5000000);
        List<Map<String,String>> cloudRecords = DnsRecordUtils.getDnsRecord(dnsResult);
        List<DnsRecord> dbRecords = dnsRecordRepository.findAll();

        // 创建云记录的name到记录的映射，便于快速查找
        Map<String, Map<String, String>> cloudRecordMap = new HashMap<>();
        for (Map<String, String> record : cloudRecords) {
            cloudRecordMap.put(record.get("name"), record);
        }
        // 创建数据库记录的dns到记录的映射，便于快速查找
        Map<String, DnsRecord> dbRecordMap = new HashMap<>();
        for (DnsRecord record : dbRecords) {
            dbRecordMap.put(record.getDns(), record);
        }
        // 找出需要添加到数据库的记录（在云上存在但在数据库中不存在）
        List<DnsRecord> recordsToInsert = new ArrayList<>();
        for (Map<String, String> cloudRecord : cloudRecords) {
            String dnsName = cloudRecord.get("name");
            if (!dbRecordMap.containsKey(dnsName)) {
                // 数据库中不存在，需要添加
                DnsRecord dnsRecord = new DnsRecord();
                dnsRecord.setId(cloudRecord.get("id"));
                dnsRecord.setDns(dnsName);
                dnsRecord.setContent(cloudRecord.get("content"));
                // domain字段需要根据业务逻辑设置，这里暂时留空
                dnsRecord.setDomain("");
                recordsToInsert.add(dnsRecord);
            }
        }
        // 根据查找结果对数据库进行增加与删除
        if (!recordsToInsert.isEmpty()) {
            dnsRecordRepository.saveAll(recordsToInsert);
        }
        log.info("同步DNS记录完成，新增 {} 条记录", recordsToInsert.size());
    }

}
