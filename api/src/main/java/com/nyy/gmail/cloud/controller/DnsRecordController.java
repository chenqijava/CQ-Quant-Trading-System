package com.nyy.gmail.cloud.controller;

import com.aliyun.openservices.shade.org.apache.commons.lang3.StringUtils;
import com.nyy.gmail.cloud.common.Session;
import com.nyy.gmail.cloud.common.annotation.NoLogin;
import com.nyy.gmail.cloud.common.response.ResponseResult;
import com.nyy.gmail.cloud.common.response.Result;
import com.nyy.gmail.cloud.common.response.ResultCode;
import com.nyy.gmail.cloud.entity.mongo.GroupTask;
import com.nyy.gmail.cloud.entity.mysql.DnsRecord;
import com.nyy.gmail.cloud.model.dto.LinkCheckReqDto;
import com.nyy.gmail.cloud.model.vo.DnsRecordVO;
import com.nyy.gmail.cloud.repository.mysql.DnsRecordRepository;
import com.nyy.gmail.cloud.service.DnsRecordService;
import com.nyy.gmail.cloud.service.LinkCheckService;
import com.nyy.gmail.cloud.service.ParamsService;
import com.nyy.gmail.cloud.utils.DnsRecordUtils;
import com.nyy.gmail.cloud.utils.GoogleDocUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * DNS记录管理控制器
 */
@RestController
@RequestMapping("/api")
@Slf4j
public class DnsRecordController {
    @Autowired
    private DnsRecordRepository dnsRecordRepository;
    @Autowired
    private DnsRecordService dnsRecordService;

    @Autowired
    private LinkCheckService linkCheckService;
    @Autowired
    private ParamsService paramsService;
    /**
     * URL转二级域名请求参数
     */
    @Data
    public static class UrlToDomainRequest {
        /**
         * 记录名称
         */
        private String name;

        /**
         * 记录内容
         */
        private String content;

        /**
         * DNS记录类型 (A, CNAME, TXT等)
         */
        private String type = "CNAME";

        /**
         * TTL值 (默认1表示自动)
         */
        private Integer ttl = 1;

        /**
         * 记录备注/注释
         */
        private String comment;

        /**
         * 是否启用代理 (Cloudflare proxy)
         */
        private Boolean proxied = false;
    }

    /**
     * 从URL中提取域名部分
     *
     * @param url 原始URL
     * @return 提取的域名
     */
    private String extractDomainFromUrl(String url) {
        // 移除协议部分(http://, https://)
        String cleanUrl = url.replaceFirst("^https?://", "");

//        // 只保留域名部分，移除路径和参数
//        int pathIndex = cleanUrl.indexOf('/');
//        if (pathIndex > 0) {
//            cleanUrl = cleanUrl.substring(0, pathIndex);
//        }
//
//        // 移除端口号
//        int portIndex = cleanUrl.indexOf(':');
//        if (portIndex > 0) {
//            cleanUrl = cleanUrl.substring(0, portIndex);
//        }
//
//        // 确保末尾没有斜杠
//        if (cleanUrl.endsWith("/")) {
//            cleanUrl = cleanUrl.substring(0, cleanUrl.length() - 1);
//        }

        return cleanUrl;
    }

    /**
     * 随机生成一个域名
     * @return 生成的域名
     */
    private String buildUrl() {
        // 根据域名配置对应结束部分
        String severnumber = paramsService.getParams("account.severnumber", null, null).toString();
        String s = "abcdefghijklmnopqrstuvwxyz1234567890";
        int sLen = s.length();
        Random random = new Random();
        int len = random.nextInt(5) + 1; // 随机长度 1~5
        StringBuilder result = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            int index = random.nextInt(sLen);
            result.append(s.charAt(index));
        }
        String res = result.toString();
        // 添加时间戳并压缩为36进制，防止不同批次间重复
        String shortTimestamp = Long.toString(System.currentTimeMillis(), 36);
        res = res.concat(shortTimestamp);
        res = res.concat(severnumber);
        res = res.concat(".inboxtad.com");
        log.info("{}", severnumber);
        return res;
    }

    /**
     * 根据传入的数据，为目标配置域名
     *
     * @param requests
     * @return json报文
     */
    public String convertUrlToDomain(List<UrlToDomainRequest> requests) {
        try {
            List<Map<String,Object>> record = new ArrayList<>();
            for (UrlToDomainRequest request:requests){
                Map<String, Object> recordItem = new HashMap<>();
                recordItem.put("type", request.getType() != null ? request.getType() : "CNAME");
                recordItem.put("name", request.getName());
                recordItem.put("content", request.getContent());
                recordItem.put("ttl", request.getTtl() != null ? request.getTtl() : 1);
                recordItem.put("proxied", true);
                if (request.getComment() != null) {
                    recordItem.put("comment", request.getComment());
                }
                record.add(recordItem);
            }
            // 调用工具类创建DNS记录
            String result = DnsRecordUtils.createDnsRecords(DnsRecordUtils.getZoneId(), record);

            return result;
        } catch (Exception e) {
            log.error("URL转换为二级域名失败", e);
            return e.getMessage();
        }
    }

    /**
     * 请求MySQL中已有的URL转域名
     *
     * @return
     */
    @GetMapping({"/dns/getURLtoDomain/{page}/{perPage}"})
    public Result getUrlToDomain(@PathVariable Integer page, @PathVariable Integer perPage, @RequestParam Map<String,String> params){
        // 执行分页查询
        Page<DnsRecordVO> dnsRecordPage = dnsRecordService.getAllRecords(page, perPage, params);

        // 封装返回结果
        Map<String, Object> response = new HashMap<>();
        response.put("records", dnsRecordPage.getContent());
        response.put("totalCount", dnsRecordPage.getTotalElements());
        return ResponseResult.success(response);
    }

    /**
     * 插入URL转换为二级域名接口
     *
     * @param requestData URL转换请求参数
     * @param num 每个url需要生成映射的个数
     * @return 创建结果
     */
    @PostMapping("/dns/addUrlToDomain/{num}")
    public Result create(@RequestBody Map<String, Object> requestData, @PathVariable Integer num) {
        String target = (String) requestData.get("target");
        String workName = (String) requestData.get("workName");
        List<String> paths = (List<String>) requestData.get("paths");
        List<DnsRecord> dnsRecordsToSave = new ArrayList<>();
        if (GoogleDocUtils.BASE_DOMAIN.equals(target)) {
            //特殊处理google doc，自动生成google doc文档插入目标链接
            for (int i = 0; i < paths.size(); i++) {
                String docId = buildUrl();
                for (int j = 0; j < num; j++) {
                    String finalPath = null;
                    try {
                        finalPath = GoogleDocUtils.addDoc(docId, paths.get(i));
                    } catch (Exception e) {
                        log.error("Save Google Doc ERROR:" + e.getMessage(), e);
                    }
                    if (StringUtils.isNotBlank(finalPath)) {
                        DnsRecord dnsRecord = new DnsRecord();
                        dnsRecord.setWorkName(workName);
                        dnsRecord.setId(docId);
                        dnsRecord.setDns(finalPath);
                        dnsRecord.setContent(paths.get(i));
                        dnsRecord.setDomain(extractDomainFromUrl(paths.get(i)));
                        dnsRecordsToSave.add(dnsRecord);
                    }
                }
            }
        } else {
            // 将paths中的路径只保留域名部分，重新存储到paths中
            for (int i = 0; i < paths.size(); i++) {
                String originalUrl = paths.get(i);
                String domain = extractDomainFromUrl(originalUrl);
                paths.set(i, domain.trim());
            }

            // paths个数*num，获取需要的映射数
            Integer number = num * paths.toArray().length;
            // 确保同批次里没有相同的
            Set<String> urls = new HashSet<>();
            while (urls.size() < number) {
                urls.add(buildUrl());
            }
            // 对映射的内容调用converUrlToDomain
            List<UrlToDomainRequest> submit = new ArrayList<>();
            // 存储Dns映射
            List<String> urlList = new ArrayList<>(urls);
            for (int i = 0; i < number; i++) {
                // 配置域名的服务器部分
                UrlToDomainRequest urlToDomainRequest = new UrlToDomainRequest();
                urlToDomainRequest.setName(urlList.get(i));
                urlToDomainRequest.setContent("whatsappchat-cname.chatnow123.xyz");
                submit.add(urlToDomainRequest);
            }

            // 在完成dns映射的配置后，根据返回的结果对数据库进行插入
            try {
                // 调用convertUrlToDomain方法配置DNS域名
                String dnsResult = convertUrlToDomain(submit);
                // 从返回的结果中获取（id，name，content），并根据name拼接对应的domain
                List<Map<String, String>> dnsList = dnsRecordService.getDnsDomainMap(dnsResult);
                for (int i = 0; i < paths.size(); i++) {
                    for (int j = 0; j < num; j++) {
                        // 计算索引位置
                        int index = i * num + j;
                        if (index < dnsList.size()) {
                            Map<String, String> dnsInfo = dnsList.get(index);

                            // 创建DnsRecord对象
                            DnsRecord dnsRecord = new DnsRecord();
                            dnsRecord.setWorkName(workName);
                            dnsRecord.setId(dnsInfo.get("id"));
                            dnsRecord.setDns(dnsInfo.get("name"));
                            dnsRecord.setContent(dnsInfo.get("content"));
                            dnsRecord.setDomain(paths.get(i));

                            // 添加到待保存列表
                            dnsRecordsToSave.add(dnsRecord);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("保存DNS记录失败", e);
                return ResponseResult.failure(ResultCode.ERROR, "保存DNS记录失败: " + e.getMessage());
            }
        }
        // 根据循环中获得的结果，对数据库进行插入
        // 批量保存到数据库
        if (!dnsRecordsToSave.isEmpty()) {
            for (DnsRecord dnsRecord : dnsRecordsToSave) {
                LinkCheckReqDto reqDTO = new LinkCheckReqDto();
                reqDTO.setDesc(dnsRecord.getWorkName());
                reqDTO.setAddMethod("1");
                reqDTO.setAddData(dnsRecord.getDns());
                reqDTO.setSendNum(10);
                GroupTask groupTask = linkCheckService.save(reqDTO, Session.currentSession().userID);
                dnsRecord.setGroupTaskId(groupTask.get_id());
            }
            dnsRecordRepository.saveAll(dnsRecordsToSave);
        }


        // 如果数据库插入成功，返回true
        return ResponseResult.success();
    }

    /**
     * 批量删除DNS域名映射
     *
     * @param workNames - 需要删除的DNS域名
     * @return 删除结果
     */
    @DeleteMapping("/dns/deleteUrlToDomain")
    public Result deleteUrlToDomain(@RequestParam List<String> workNames, @RequestParam List<String> domains){
        try {
            log.info("{},{}",workNames,domains);
            // 根据workNames获取ids
            List<DnsRecord> records = dnsRecordService.getBy(workNames ,domains);
            List<String> ids = records.stream().filter(dnsRecord -> !dnsRecord.getDns().startsWith(GoogleDocUtils.getBaseUrl()))
                            .map(DnsRecord::getId).toList();
            List<String> googleDocIds = records.stream().filter(dnsRecord -> dnsRecord.getDns().startsWith(GoogleDocUtils.getBaseUrl()))
                    .map(DnsRecord::get_id).toList();
            log.info("{}",ids);
            boolean result = true;
            if(CollectionUtils.isNotEmpty(ids)) {
                result = dnsRecordService.deleteDnsDomain(ids);
            }
            log.info("{}",result);
            if(CollectionUtils.isNotEmpty(googleDocIds)){
                dnsRecordRepository.deleteAllById(googleDocIds);
            }
            // 解析结果并返回统一格式
            if (result){
                return ResponseResult.success("删除成功", result);
            } else {
                return ResponseResult.failure(ResultCode.ERROR, "删除失败");
            }
        } catch (Exception e) {
            log.error("删除DNS记录失败", e);
            return ResponseResult.failure(ResultCode.ERROR, "删除失败: " + e.getMessage());
        }
    }

    /**
     * 中转页面获取跳转的路径
     * @param come
     * @return
     */
    @NoLogin
    @GetMapping("/open/v1beta/getJumpUrl")
    public Result getJumpUrl(@RequestParam String come){
        String domain = dnsRecordRepository.findDomainByDns(come);
        if (domain != null) {
            return ResponseResult.success(domain);
        } else {
            return ResponseResult.failure(ResultCode.DATA_NOT_EXISTED, "未找到对应的域名映射");
        }
    }
}
