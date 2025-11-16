package com.nyy.gmail.cloud.service;

import com.nyy.gmail.cloud.common.Session;
import com.nyy.gmail.cloud.common.exception.CommonException;
import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.common.response.ResultCode;
import com.nyy.gmail.cloud.entity.mongo.*;
import com.nyy.gmail.cloud.enums.MailTemplateStatusEnums;
import com.nyy.gmail.cloud.enums.MailTemplateTypeEnums;
import com.nyy.gmail.cloud.model.dto.*;
import com.nyy.gmail.cloud.repository.mongo.*;
import com.nyy.gmail.cloud.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.nio.file.Path;
import java.util.*;

import static com.nyy.gmail.cloud.common.response.ResultCode.NO_AUTHORITY;

@Slf4j
@Service
public class MailTemplateService {
    @Autowired
    private MailTemplateRepository mailTemplateRepository;
    @Autowired
    private UserService userService;

    public PageResult<MailTemplate> findByPagination(MailTemplateListDTO mailTemplateListDTO, int pageSize, int page) {
        return mailTemplateRepository.findByPagination(mailTemplateListDTO, pageSize, page);
    }

    public List<MailTemplate> find(MailTemplateListDTO mailTemplateListDTO) {
        return mailTemplateRepository.find(mailTemplateListDTO);
    }

    public void importTemplate(ImportMailTemplateReqDto reqDto) {
        if (reqDto.getType().equals("1")) {
            if (StringUtils.isEmpty(reqDto.getFilepath())) {
                throw new CommonException(ResultCode.PARAMS_IS_INVALID);
            }
            Path resPath = FileUtils.resPath;
            Path path = resPath.resolve(reqDto.getFilepath()).toAbsolutePath().normalize();
            List<MailTemplate> toSave = new ArrayList<>();
            if (path.getFileName().toString().endsWith(".csv")) {
                List<String[]> lines = FileUtils.readCsv(path);
                for (String[] line : lines) {
                    if (line.length != 3) {
                        throw new CommonException(ResultCode.PARAMS_IS_INVALID);
                    } else {
                        MailTemplate mailTemplate = new MailTemplate();
                        mailTemplate.setGroupID(reqDto.getGroupId());
                        mailTemplate.setName(line[0]);
                        mailTemplate.setTitle(line[1]);
                        mailTemplate.setContent(line[2]);
                        mailTemplate.setType(reqDto.getTemplateType());
                        if (reqDto.getTemplateType().equals(MailTemplateTypeEnums.USER.getCode())) {
                            mailTemplate.setUserID(reqDto.getUserId());
                        }
                        toSave.add(mailTemplate);
                    }
                }
            } else if (path.getFileName().toString().endsWith(".txt")) {
                    Pair<List<String>, Long> file = FileUtils.readFileLines(path, 0, 100000);
                List<String> lines = Arrays.stream(String.join("<br/>\n", file.getLeft()).split("==NEWONE==")).filter(StringUtils::isNotBlank).toList();
                for (String line : lines) {
                    String[] split = Arrays.stream(line.split("<br/>\n")).filter(StringUtils::isNotBlank).toArray(String[]::new);
                    if (split.length < 3) {
                        throw new CommonException(ResultCode.PARAMS_IS_INVALID);
                    } else {
                        MailTemplate mailTemplate = new MailTemplate();
                        mailTemplate.setGroupID(reqDto.getGroupId());
                        mailTemplate.setName(split[0]);
                        mailTemplate.setTitle(split[1]);
                        StringBuilder content = new StringBuilder();
                        for (int i = 2; i < split.length; i++) {
                            content.append(split[i]);
                            if (i != split.length - 1) {
                                content.append("\n");
                            }
                        }
                        mailTemplate.setContent(content.toString());
                        mailTemplate.setType(reqDto.getTemplateType());
                        if (reqDto.getTemplateType().equals(MailTemplateTypeEnums.USER.getCode())) {
                            mailTemplate.setUserID(reqDto.getUserId());
                        }
                        toSave.add(mailTemplate);
                    }
                }
            } else {
                throw new CommonException(ResultCode.PARAMS_IS_INVALID);
            }
            if (!CollectionUtils.isEmpty(toSave)) {
                mailTemplateRepository.saveBatch(toSave);
            }
        } else {
            MailTemplate mailTemplate = new MailTemplate();
            mailTemplate.setName(reqDto.getName());
            mailTemplate.setContent(reqDto.getContent());
            mailTemplate.setTitle(reqDto.getTitle());
            mailTemplate.setGroupID(reqDto.getGroupId());
            mailTemplate.setType(reqDto.getTemplateType());
            if (reqDto.getTemplateType().equals(MailTemplateTypeEnums.USER.getCode())) {
                mailTemplate.setUserID(reqDto.getUserId());
            }
            mailTemplateRepository.save(mailTemplate);
        }
    }

    public void saveTemplate(MailTemplate reqDto) {
        MailTemplate old = mailTemplateRepository.findById(reqDto.get_id());
        MailTemplate mailTemplate = new MailTemplate();
        mailTemplate.set_id(reqDto.get_id());
        mailTemplate.setName(reqDto.getName());
        mailTemplate.setContent(reqDto.getContent());
        mailTemplate.setTitle(reqDto.getTitle());
        mailTemplate.setGroupID(reqDto.getGroupID());
        if (old != null && (
                !old.getName().equals(reqDto.getName())
                        || !old.getTitle().equals(reqDto.getTitle())
                        || !old.getContent().equals(reqDto.getContent())
                )) {
            mailTemplate.setStatus(MailTemplateStatusEnums.NORMAL.getCode());
        }
        mailTemplate.setType(reqDto.getType());
        if (reqDto.getType().equals(MailTemplateTypeEnums.USER.getCode())) {
            mailTemplate.setUserID(reqDto.getUserID());
        }
        mailTemplateRepository.update(mailTemplate);
    }

    public void updateGroup(List<String> ids, String groupId) {
        String userID = Session.currentSession().getUserID();
        List<MailTemplate> olds = mailTemplateRepository.findByIdsIn(ids);
        if (!CollectionUtils.isEmpty(olds) && olds.stream().anyMatch(mailTemplate ->
                mailTemplate.getType().equals(MailTemplateTypeEnums.USER.getCode()) &&
                        !mailTemplate.getUserID().equals(userID))) {
            throw new CommonException(NO_AUTHORITY, "无权限");
        }
        if (!CollectionUtils.isEmpty(olds) && olds.stream().anyMatch(mailTemplate ->
                mailTemplate.getType().equals(MailTemplateTypeEnums.SYSTEM.getCode()) &&
                        !userService.isAdmin(userID))) {
            throw new CommonException(NO_AUTHORITY, "无权限");
        }
        for (String id : ids) {
            MailTemplate mailTemplate = new MailTemplate();
            mailTemplate.set_id(id);
            mailTemplate.setGroupID(groupId);
            mailTemplateRepository.update(mailTemplate);
        }
    }

    public void updateStatus(List<String> ids, Integer status) {
        String userID = Session.currentSession().getUserID();
        List<MailTemplate> olds = mailTemplateRepository.findByIdsIn(ids);
        if (!CollectionUtils.isEmpty(olds) && olds.stream().anyMatch(mailTemplate ->
                mailTemplate.getType().equals(MailTemplateTypeEnums.USER.getCode()) &&
                !mailTemplate.getUserID().equals(userID))) {
            throw new CommonException(NO_AUTHORITY, "无权限");
        }
        if (!CollectionUtils.isEmpty(olds) && olds.stream().anyMatch(mailTemplate ->
                mailTemplate.getType().equals(MailTemplateTypeEnums.SYSTEM.getCode()) &&
                        !userService.isAdmin(userID))) {
            throw new CommonException(NO_AUTHORITY, "无权限");
        }
        for (String id : ids) {
            MailTemplate mailTemplate = new MailTemplate();
            mailTemplate.set_id(id);
            mailTemplate.setStatus(status);
            mailTemplateRepository.update(mailTemplate);
        }
    }

    public void deleteManyByIds(List<String> ids) {
        String userID = Session.currentSession().getUserID();
        List<MailTemplate> olds = mailTemplateRepository.findByIdsIn(ids);
        if (!CollectionUtils.isEmpty(olds) && olds.stream().anyMatch(mailTemplate ->
                mailTemplate.getType().equals(MailTemplateTypeEnums.USER.getCode()) &&
                        !mailTemplate.getUserID().equals(userID))) {
            throw new CommonException(NO_AUTHORITY, "无权限");
        }
        if (!CollectionUtils.isEmpty(olds) && olds.stream().anyMatch(mailTemplate ->
                mailTemplate.getType().equals(MailTemplateTypeEnums.SYSTEM.getCode()) &&
                        !userService.isAdmin(userID))) {
            throw new CommonException(NO_AUTHORITY, "无权限");
        }
        mailTemplateRepository.deleteManyByIds(ids);
    }
}