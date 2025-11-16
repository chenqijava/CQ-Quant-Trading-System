package com.nyy.gmail.cloud.service;

import com.nyy.gmail.cloud.entity.mongo.IpStatistics;
import com.nyy.gmail.cloud.repository.mongo.IpStatisticsRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * ip 统计
 *
 * @Author: wjx
 * @Date: 2023/7/7 11:07
 */
@Service
public class IpStatisticsService {

    @Autowired
    private IpStatisticsRepository ipStatisticsRepository;

    public void record(String userID, String ip) {
        IpStatistics ipStatistics = ipStatisticsRepository.findOne(userID, ip);
        if (ipStatistics == null) {
            IpStatistics _ipStatistics = new IpStatistics();
            _ipStatistics.setUserID(userID);
            _ipStatistics.setIp(ip);
            ipStatisticsRepository.saveIpStatistics(_ipStatistics);
        } else {
            ipStatistics.setLoginNumber(ipStatistics.getLoginNumber() + 1);
            ipStatistics.setLastLoginTime(new Date());
            ipStatisticsRepository.updateIpStatistics(ipStatistics);
        }
    }
}
