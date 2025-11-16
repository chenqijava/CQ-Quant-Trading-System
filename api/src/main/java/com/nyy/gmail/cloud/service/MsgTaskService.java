package com.nyy.gmail.cloud.service;

import com.nyy.gmail.cloud.common.pagination.MongoPageHelper;
import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.common.response.ResponseResult;
import com.nyy.gmail.cloud.common.response.Result;
import com.nyy.gmail.cloud.common.response.ResultCode;
import com.nyy.gmail.cloud.entity.mongo.Account;
import com.nyy.gmail.cloud.entity.mongo.GroupTask;
import com.nyy.gmail.cloud.entity.mongo.SubTask;
import com.nyy.gmail.cloud.enums.AccountOnlineStatus;
import com.nyy.gmail.cloud.enums.GroupTaskUserActionEnums;
import com.nyy.gmail.cloud.enums.TaskTypesEnums;
import com.nyy.gmail.cloud.model.dto.task.MsgTaskDTO;
import com.nyy.gmail.cloud.model.vo.task.MsgTaskVO;
import com.nyy.gmail.cloud.model.vo.task.SubTaskVO;
import com.nyy.gmail.cloud.repository.mongo.AccountRepository;
import com.nyy.gmail.cloud.repository.mongo.GroupTaskRepository;
import com.nyy.gmail.cloud.repository.mongo.SubTaskRepository;
import com.nyy.gmail.cloud.utils.TaskUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MsgTaskService {

    @Autowired
    private SubTaskRepository subTaskRepository;

    @Autowired
    private TaskUtil taskUtil;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private GroupTaskRepository groupTaskRepository;

    @Autowired
    private MongoPageHelper mongoPageHelper;

    public PageResult<MsgTaskVO> queryMsgTaskByPage(Integer pageSize, Integer pageNo, Map<String, Object> filters) {

        Query query = new Query();
        query.addCriteria(Criteria.where("userID").is((String)filters.get("userId")));
        query.addCriteria(Criteria.where("type").is(TaskTypesEnums.SendMessage.getCode()));
        if (filters.get("status") != null) {
            query.addCriteria(Criteria.where("status").is((String)filters.get("status")));
        }
        if (filters.get("desc") != null) {
            query.addCriteria(Criteria.where("desc").regex("^"+filters.get("desc")));
        }

        PageResult<GroupTask> page = mongoPageHelper.pageQuery(query, GroupTask.class, pageSize,pageNo);
        List<MsgTaskVO> list = new ArrayList<>();
        page.getData().forEach(e->{
            MsgTaskVO vo = new MsgTaskVO();
            BeanUtils.copyProperties(e, vo);
            list.add(vo);
        });
        PageResult<MsgTaskVO> pageResult = new PageResult<>();
        pageResult.setTotal(page.getTotal());
        pageResult.setPageNum(pageNo);
        pageResult.setPageSize(pageSize);
        pageResult.setData(list);
        pageResult.setPages(page.getPages());
        return pageResult;
    }

    public Result batchDelete(List<String> ids, String userID) {
        if(CollectionUtils.isEmpty(ids)){
            return ResponseResult.failure(ResultCode.PARAMS_IS_BLANK);
        }
        List<GroupTask> groupTasks = groupTaskRepository.findAllById(ids).stream().filter(e -> e.getUserID().equals(userID)).toList();
        for (GroupTask groupTask : groupTasks) {
            groupTaskRepository.deleteById(groupTask.get_id());
        }
        return ResponseResult.success();
    }

    public Result finishTask(List<String> ids, String userID) {
        if(CollectionUtils.isEmpty(ids)){
            return ResponseResult.failure(ResultCode.PARAMS_IS_BLANK);
        }
        List<GroupTask> groupTasks = groupTaskRepository.findAllById(ids).stream().filter(e -> e.getUserID().equals(userID)).toList();
        groupTasks.forEach(e->{
            e.setStatus("success");
            e.setUserAction(GroupTaskUserActionEnums.ForceFinish.getCode());
        });
        groupTaskRepository.saveAll(groupTasks);
        return ResponseResult.success();
    }

    public PageResult<SubTaskVO> queryProcessDetail(Integer pageSize, Integer pageNo, String taskId, Map<String, Object> filters, String userID) {

        Query query = new Query();
        query.addCriteria(Criteria.where("groupTaskId").is(taskId));
        query.addCriteria(Criteria.where("userID").is(userID));
        if (filters.get("status") != null) {
            query.addCriteria(Criteria.where("status").is((String)filters.get("status")));
        }

        PageResult<SubTask> page = mongoPageHelper.pageQuery(query, SubTask.class, pageSize,pageNo);
        List<SubTaskVO> list = new ArrayList<>();
        page.getData().forEach(e->{
            SubTaskVO vo = new SubTaskVO();
            BeanUtils.copyProperties(e, vo);
            Account ac = accountRepository.findById(e.getAccid());
            if(ac!= null && StringUtils.hasText(ac.getPhone())){
                vo.setSendId(ac.get_id());
                vo.setSendPhone(ac.getPhone());
            }else {
                if(e.getParams().get("sendPhone") != null){
                    vo.setSendPhone(e.getParams().get("sendPhone").toString());
                }
            }
            list.add(vo);
        });
        PageResult<SubTaskVO> pageResult = new PageResult<>();
        pageResult.setTotal(page.getTotal());
        pageResult.setPageNum(pageNo);
        pageResult.setPageSize(pageSize);
        pageResult.setData(list);
        pageResult.setPages(page.getPages());
        return pageResult;
    }

    public Result  crateTask(MsgTaskDTO taskDTO) throws IOException {

        Map<String, Object> map = new HashMap<>(Map.of(
                "taskDesc", taskDTO.getTaskDesc(),
                "addMethod", taskDTO.getAddMethod(),
                "activeTabKey", taskDTO.getActiveTabKey(),
                "sendMethod", taskDTO.getSendMethod()));
//        if(!CollectionUtils.isEmpty(taskDTO.getIds())){
//            List<Account> list = accountRepository.findByIds(taskDTO.getIds()).stream().filter(e -> e.getUserID().equals(taskDTO.getUserId())).toList();
//            map.put("sendPhones", list.stream().map(Account::getPhone).collect(Collectors.toList()));
//        }
        if(!CollectionUtils.isEmpty(taskDTO.getAccountGroupIds())){
            List<String> ids = accountRepository.findByGroupIdList(taskDTO.getAccountGroupIds()).stream().filter(e->AccountOnlineStatus.ONLINE.getCode().equals(e.getOnlineStatus())).map(Account::get_id).collect(Collectors.toList());
            taskDTO.getIds().addAll(ids);
        }
        if(CollectionUtils.isEmpty(taskDTO.getIds())){
            return ResponseResult.failure(ResultCode.NO_ONLINE_ACCOUNT);
        }
        map.put("ids", taskDTO.getIds());
        if("2".equals(taskDTO.getAddMethod()) && StringUtils.hasText(taskDTO.getAddDataFilePath())){
            map.put("filepath", taskDTO.getAddDataFilePath());
        }else{
            map.put("addDatas", taskDTO.getAddData());
        }
        if(!CollectionUtils.isEmpty(taskDTO.getAccountGroupIds())){
            map.put("groupIds",taskDTO.getAccountGroupIds());
        }
        if(StringUtils.hasText(taskDTO.getImages())){
            map.put("imageFilePath", taskDTO.getImages());
        }
        if(StringUtils.hasText(taskDTO.getText())){
            map.put("content", taskDTO.getText());
        }

        taskUtil.createGroupTask(taskDTO.getIds(), TaskTypesEnums.SendMessage,map,
                taskDTO.getUserId(),
                "1",
                taskDTO.getSendDateTime());

        return ResponseResult.success();
    }
}
