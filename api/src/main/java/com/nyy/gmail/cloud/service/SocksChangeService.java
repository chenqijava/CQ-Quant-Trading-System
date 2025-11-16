package com.nyy.gmail.cloud.service;

import com.nyy.gmail.cloud.entity.mongo.SocksChange;
import com.nyy.gmail.cloud.repository.mongo.SocksChangeRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SocksChangeService {
    @Resource
    private SocksChangeRepository socksChangeRepository;

    private Map<String, Map<String, List<String>>> userId2SocksChange = new HashMap<>();

    @PostConstruct
    public void init() {
        this.reload();
    }

    public void reload() {
        List<SocksChange> socksChanges = socksChangeRepository.findAll();
        userId2SocksChange = socksChanges.stream()
                .collect(Collectors.groupingBy(
                        p -> Objects.requireNonNullElse(p.getUserID(), "admin"),
                        Collectors.toMap(SocksChange::getSource, SocksChange::getTarget)
                ));
        log.info("load params:{}", userId2SocksChange);
    }

    public List<String> getSocksChange(String area, String userID) {
        final String k = area.toUpperCase();
        return Optional.ofNullable(userId2SocksChange.get(userID))
                .map(innerMap -> innerMap.get(k))
                .orElse(null);
    }
}
