package com.nyy.gmail.cloud.service;

import com.nyy.gmail.cloud.repository.mongo.VpsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VpsService {


    @Autowired
    private VpsRepository vpsRepository;

}
