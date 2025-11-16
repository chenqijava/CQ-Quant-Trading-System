package com.nyy.gmail.cloud.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.nyy.gmail.cloud.common.exception.CommonException;
import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.common.response.ResponseResult;
import com.nyy.gmail.cloud.common.response.ResultCode;
import com.nyy.gmail.cloud.entity.mongo.GroupTask;
import com.nyy.gmail.cloud.entity.mysql.DnsRecord;
import com.nyy.gmail.cloud.enums.TaskTypesEnums;
import com.nyy.gmail.cloud.model.dto.LinkCheckDetail;
import com.nyy.gmail.cloud.model.dto.Params;
import com.nyy.gmail.cloud.model.vo.DnsRecordVO;
import com.nyy.gmail.cloud.repository.mongo.GroupTaskRepository;
import com.nyy.gmail.cloud.repository.mongo.SubTaskRepository;
import com.nyy.gmail.cloud.repository.mysql.DnsRecordRepository;
import com.nyy.gmail.cloud.utils.DnsRecordUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Slf4j
@Service
public class DnsRecordService {
    @Autowired
    private DnsRecordRepository dnsRecordRepository;
    @Autowired
    private LinkCheckService linkCheckService;

    /**
     * 根据条件从数据库中获取所有的记录
     *
     * @param page 当前页面
     * @param perPage 每页大小
     * @param params 筛选条件
     */
    public Page<DnsRecordVO> getAllRecords(Integer page, Integer perPage, Map<String,String> params){
        String domain = params.get("domain");
        String workName = params.get("workName");
        // 创建分页请求对象
        Pageable pageable = PageRequest.of(page - 1, perPage);

        Page<Object[]> results = dnsRecordRepository.findAggregateData(workName, domain, pageable);
        Map<String, Integer> junkNumMap = new HashMap<>();
        Map<String, Integer> normalNumMap = new HashMap<>();
        List<String> groupTaskIds = results.stream()
                .filter(objects -> StringUtils.isNotBlank(((String) objects[5])))
                .flatMap(objects -> Arrays.stream(((String) objects[5]).split(","))).distinct().toList();
        for (String groupTaskId : groupTaskIds) {
            PageResult<LinkCheckDetail> pageResult = linkCheckService.detail(new HashMap<>(), groupTaskId, 100000, 1);
            if (pageResult != null) {
                Integer junkNum = 0;
                Integer normalNum = 0;
                for (LinkCheckDetail linkCheckDetail : pageResult.getData()) {
                    junkNum += linkCheckDetail.getJunkNum();
                    normalNum += linkCheckDetail.getNormalNum();
                }
                junkNumMap.putIfAbsent(groupTaskId, junkNum);
                normalNumMap.putIfAbsent(groupTaskId, normalNum);
            }
        }

        Page<DnsRecordVO> voPage = results.map(obj -> {
            DnsRecordVO vo = new DnsRecordVO();
            vo.setWorkName((String) obj[0]);
            vo.setCount(((Number) obj[1]).longValue());
            vo.setLatestCreateTime((Date) obj[2]);
            vo.setDomain((String) obj[3]);

            // 将逗号分隔的字符串转换为 List<String>
            String dnsListStr = (String) obj[4];
            if (dnsListStr != null && !dnsListStr.isEmpty()) {
                List<String> dnsList = Arrays.asList(dnsListStr.split(","));
                vo.setDnsList(dnsList);
            } else {
                vo.setDnsList(new ArrayList<>());
            }
            String groupTaskIdListStr = (String) obj[5];
            BigDecimal rate = BigDecimal.ZERO;
            if (groupTaskIdListStr != null && !groupTaskIdListStr.isEmpty()) {
                List<String> groupTaskIdList = Arrays.asList(groupTaskIdListStr.split(","));
                Integer junkNum = 0;
                Integer normalNum = 0;
                for (String groupTaskId : groupTaskIdList) {
                    junkNum += junkNumMap.getOrDefault(groupTaskId, 0);
                    normalNum += normalNumMap.getOrDefault(groupTaskId, 0);
                }
                if (junkNum + normalNum > 0) {
                    rate = BigDecimal.valueOf(junkNum).divide(BigDecimal.valueOf(junkNum + normalNum), 6, RoundingMode.HALF_UP);
                }
            }
            vo.setRate(rate);

            return vo;
        });
        return voPage;
    }


    /**
     * 提取相关的参数
     * @param result
     * @return
     */
    public List<Map<String,String>> getDnsDomainMap(String result){
        // 解析记录列表和总数
        List<Map<String, String>> dnsDomainList = new ArrayList<>();
        JSONObject jsonObject = JSON.parseObject(result);
        JSONObject resultObj = jsonObject.getJSONObject("result");
        JSONArray resultArry = resultObj.getJSONArray("posts"); // 根据实际返回结构调整

        // 遍历所有记录
        for (int i = 0; i < resultArry.size(); i++) {
            JSONObject record = resultArry.getJSONObject(i);
            Map<String, String> dnsDomainItem = new HashMap<>();
            dnsDomainItem.put("name", record.getString("name"));
            dnsDomainItem.put("id", record.getString("id"));
            dnsDomainItem.put("content", record.getString("content"));
            dnsDomainList.add(dnsDomainItem);
        }

        return dnsDomainList;
    }

    /**
     * 删除方法
     * @param ids - 要删除的id
     */
    public boolean deleteDnsDomain(List<String> ids){
        try {
            String zoneId = DnsRecordUtils.getZoneId();
            // 根据ids删除dns别名
            String result = DnsRecordUtils.deleteDnsRecords(zoneId,ids);
            // 根据返回的result，获取内容的name
            JSONObject jsonObject = JSON.parseObject(result);
            JSONObject resultObj = jsonObject.getJSONObject("result");
            JSONArray resultArry = resultObj.getJSONArray("deletes");
            // 遍历记录获取name
            List<String> names = new ArrayList<>();
            for (int i = 0 ; i < resultArry.size(); i++){
                JSONObject record = resultArry.getJSONObject(i);
                names.add(record.getString("name"));
            }
            // 根据获取到的name，删除数据库中的内容
            dnsRecordRepository.deleteByDnsIn(names);

            return true;
        } catch (Exception e) {
            log.info("{}",e.getMessage());
            return false;
        }

    }

    public List<DnsRecord> getBy(List<String> workNames,List<String> domains) {
        List<DnsRecord> records = new ArrayList<>();
        if (workNames != null && !workNames.isEmpty()) {
            // 使用Specification查询所有workName在给定列表中的记录
            Specification<DnsRecord> specification = (root, query, criteriaBuilder) -> {
                jakarta.persistence.criteria.Predicate predicate = root.get("workName").in(workNames);
                jakarta.persistence.criteria.Predicate predicate1 = root.get("domain").in(domains);
                return criteriaBuilder.and(predicate, predicate1);
            };

            // 查询数据库
            records = dnsRecordRepository.findAll(specification);
            return records;

        }
        return records;
    }


    /**
     * 根据workNames获取所有对应的ids
     * @param workNames 工作名称列表
     * @param domains 目标路径
     * @return 对应的ID列表
     */
    public List<String> getIdsBy(List<String> workNames,List<String> domains) {
        List<String> ids = new ArrayList<>();
        if (workNames != null && !workNames.isEmpty()) {
            // 使用Specification查询所有workName在给定列表中的记录
            Specification<DnsRecord> specification = (root, query, criteriaBuilder) -> {
                jakarta.persistence.criteria.Predicate predicate = root.get("workName").in(workNames);
                jakarta.persistence.criteria.Predicate predicate1 = root.get("domain").in(domains);
                return criteriaBuilder.and(predicate, predicate1);
            };

            // 查询数据库
            List<DnsRecord> records = dnsRecordRepository.findAll(specification);

            // 提取所有记录的ID
            for (DnsRecord record : records) {
                ids.add(record.getId());
            }
        }
        return ids;
    }

}
