package com.nyy.gmail.cloud.tasks.impl;

import com.nyy.gmail.cloud.common.exception.CommonException;
import com.nyy.gmail.cloud.common.response.ResultCode;
import com.nyy.gmail.cloud.entity.mongo.Account;
import com.nyy.gmail.cloud.entity.mongo.GroupTask;
import com.nyy.gmail.cloud.entity.mongo.SubTask;
import com.nyy.gmail.cloud.entity.mongo.User;
import com.nyy.gmail.cloud.enums.*;
import com.nyy.gmail.cloud.framework.mq.SubTaskMQProducer;
import com.nyy.gmail.cloud.gateway.GatewayClient;
import com.nyy.gmail.cloud.gateway.dto.SendEmailResponse;
import com.nyy.gmail.cloud.mq.entity.TaskMessage;
import com.nyy.gmail.cloud.repository.mongo.*;
import com.nyy.gmail.cloud.service.AccountPlatformService;
import com.nyy.gmail.cloud.tasks.AbstractTask;
import com.nyy.gmail.cloud.tasks.BaseTask;
import com.nyy.gmail.cloud.utils.*;
import com.nyy.gmail.cloud.utils.tgnet.SessionParser;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipOutputStream;

@Slf4j
@Component("TgNetToSession")
public class TgNetToSessionTaskImpl extends AbstractTask implements BaseTask {

    @Autowired
    private SubTaskRepository subTaskRepository;

    @Autowired
    private GroupTaskRepository groupTaskRepository;

    @Autowired
    private TaskUtil taskUtil;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountPlatformService accountPlatformService;

    @Autowired
    private GatewayClient gatewayClient;

    @Override
    protected SubTaskRepository getSubTaskRepository() {
        return subTaskRepository;
    }

    @Override
    protected TaskUtil getTaskUtil() {
        return taskUtil;
    }

    @Autowired
    private SubTaskMQProducer subTaskMQProducer;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BalanceDetailRepository balanceDetailRepository;

    @Override
    public boolean publishTask(GroupTask groupTask) {
        // 导入任务
        String userID = groupTask.getUserID();
        Map params = groupTask.getParams();
        String dir = params.get("tgNetDir").toString();
        groupTask.setUpdateTime(new Date());

        switch (GroupTaskStatusEnums.fromCode(groupTask.getStatus())) {
            case GroupTaskStatusEnums.waitPublish -> {
                groupTask.setStatus(GroupTaskStatusEnums.processing.getCode());
            }
            case GroupTaskStatusEnums.init, GroupTaskStatusEnums.processing -> {
                // 保存子任务
                long taskCount = subTaskRepository.countByGroupTaskIdEquals(groupTask.get_id());

                List<SubTask> processingTasks = new ArrayList<>();
                if (taskCount <= 0) {
                    File currentDir = new File(dir);

                    File[] dirs = currentDir.listFiles(File::isDirectory);
                    if (dirs != null) {
                        for (File d : dirs) {
                            SubTask subTask = taskUtil.newTaskModel(groupTask, Map.of(), new HashMap(Map.of(
                                    "tgnet", Path.of(d.getAbsolutePath(), "tgnet.dat").toString(), "userconfing", Path.of(d.getAbsolutePath(), "userconfing.xml").toString())), new Date());
                            processingTasks.add(subTask);
                            taskCount++;
                        }

                        subTaskRepository.batchInsert(processingTasks);
                        groupTaskRepository.save(groupTask);
                    }
                    return true;
                } else if (groupTask.getStatus().equals(GroupTaskStatusEnums.processing.getCode())) {
                    groupTask.setTotal(taskCount);
                    groupTask.setPublishTotalCount(taskCount);
                    groupTask.setStatus(GroupTaskStatusEnums.init.getCode());
                    groupTaskRepository.save(groupTask);
                }
            }
            case null -> {
            }
            default -> {
            }
        }

        switch (GroupTaskUserActionEnums.fromCode(groupTask.getUserAction())) {
            case null -> {
            }
            case ForceFinish -> {
                taskUtil.finishForceTaskByPublish(groupTask);
            }
        }
        if (StringUtil.isNotEmpty(groupTask.getUserAction())) {
            groupTask.setUserAction("");
            groupTaskRepository.save(groupTask);
            return true;
        }

        List<String> ids = groupTask.getIds();
        int onceMaxCount = 500;
        Map<String, Account> accountMap = new HashMap<>();
        List<TaskMessage> mqMsg = new ArrayList<>();

        List<SubTask> subTaskList = subTaskRepository.findByUserIDEqualsAndGroupTaskIdEqualsAndStatusIn(userID, groupTask.get_id(), List.of(SubTaskStatusEnums.processing.getCode()), 1, onceMaxCount);
        int count = 0;
        for (SubTask subTask : subTaskList) {
            // 随机
            String accid = ids.get(count % ids.size());
            count++;
            Account account = accountMap.get(accid);
            if (account == null) {
                account = accountRepository.findById(accid);
            }
            if (account == null || !account.getOnlineStatus().equals(AccountOnlineStatus.ONLINE.getCode())) {
                continue;
            } else {
                accountMap.put(accid, account);
            }

            taskUtil.distributeAccount(account, subTask);

            mqMsg.add(taskUtil.domain2mqTask(subTask));
        }

        if (!mqMsg.isEmpty()) {
            subTaskMQProducer.sendMessage(mqMsg);
        }

        try {
            if (subTaskList.isEmpty() && groupTask.getSuccess() + groupTask.getFailed() >= groupTask.getTotal()) {
                long success = subTaskRepository.countByGroupTaskIdEqualsAndStatusEquals(groupTask.get_id(), SubTaskStatusEnums.success.getCode());
                long failed = subTaskRepository.countByGroupTaskIdEqualsAndStatusEquals(groupTask.get_id(), SubTaskStatusEnums.failed.getCode());
                if (groupTask.getSuccess() != success) {
                    groupTask.setSuccess(success);
                }
                if (groupTask.getFailed() != failed) {
                    groupTask.setFailed(failed);
                }
                if (groupTask.getSuccess() + groupTask.getFailed() >= groupTask.getTotal()) {
                    groupTask.setPublishStatus("success");
                    groupTask.setFinishTime(new Date());
                    if (!groupTask.getStatus().equals(GroupTaskStatusEnums.success.getCode())) {
                        groupTask.setStatus(GroupTaskStatusEnums.success.getCode());
                    }

                    // 退款
                    if (groupTask.getFailed() > 0) {
                        try {
                            RLock lock = redissonClient.getLock(userID + "-charge");
                            if (lock.tryLock(30, TimeUnit.SECONDS)) {
                                try {
                                    User user = userRepository.findOneByUserID(userID);
                                    if (user != null) {
                                        user.setBalance(user.getBalance().add(BigDecimal.valueOf(groupTask.getFailed())));
                                        userRepository.updateUser(user);

                                        balanceDetailRepository.addUserBill("TGNET转换失败退款",
                                                userID, BigDecimal.valueOf(groupTask.getFailed()), user.getBalance(), user.getName(), BillExpenseTypeEnums.IN, BillCateTypeEnums.REFUND);
                                    }
                                } finally {
                                    lock.unlock();
                                }
                            }
                        } catch (Exception e) {
                            log.error("groupTaskId: " + groupTask.get_id() + " 退款失败：" + e.getMessage());
                        }
                    }

                    // 把目录删除
                    FileUtils.deleteFile(Path.of(dir));

                    List<SubTask> subTasks = subTaskRepository.findByGroupTaskId(groupTask.get_id());
                    // 找出所有任务写到文件，保存result
                    List<String> lines = subTasks.stream().filter(e -> e.getStatus().equals(SubTaskStatusEnums.success.getCode()) && !StringUtils.isEmpty(e.getResult().getOrDefault("session", "").toString())).map(e -> e.getResult().getOrDefault("session", "").toString()).toList();
                    // 导出文件保存
                    Path resPath = FileUtils.resPath;
                    Path filepath = Path.of("tgNetToSession", "pySession" + UUIDUtils.get32UUId().substring(26) + "_" + DateUtil.formatByDate(new Date(), DateUtil.FORMAT.YYYYMMDDHHMMSS) + "_" + lines.size() + ".txt");
                    Path filepath2 = Path.of("tgNetToSession", "pySession2" + UUIDUtils.get32UUId().substring(26) + "_" + DateUtil.formatByDate(new Date(), DateUtil.FORMAT.YYYYMMDDHHMMSS) + "_" + lines.size());
                    Path path = resPath.resolve(filepath).toAbsolutePath().normalize();
                    Path path2 = resPath.resolve(filepath2).toAbsolutePath().normalize();

                    try {
                        Files.createDirectories(resPath.resolve("tgNetToSession").normalize());
                    } catch (IOException e) {
                        log.error("创建upload文件夹失败", e);
                    }
                    boolean flag = false;
                    try {
                        if (Files.exists(path)) {
                            Files.delete(path);
                        }
                        if (Files.exists(path2)) {
                            FileUtils.deleteFile(path2);
                        }
                        Files.writeString(path, String.join("\n", lines), StandardOpenOption.CREATE_NEW);
                        List<String> files = new ArrayList<>();
                        for (SubTask subTask : subTasks) {
                            if (!subTask.getStatus().equals(SubTaskStatusEnums.success.getCode())) {
                                continue;
                            }
                            String tgnet = subTask.getParams().getOrDefault("tgnet", "").toString();
                            String line = subTask.getResult().getOrDefault("session", "").toString();
                            String[] split = line.split("----");
                            if (split.length >= 2) {
                                String name = Path.of(tgnet).getParent().getFileName().toString();
                                if (!Files.exists(path2)) {
                                    try {
                                        Files.createDirectories(path2);
                                    } catch (IOException e) {
                                        log.error("创建upload文件夹失败", e);
                                    }
                                }
                                String session = split[1];
                                Path pathSession = path2.resolve(name + ".session").toAbsolutePath().normalize();
                                if (Files.exists(pathSession)) {
                                    Files.delete(pathSession);
                                }
                                byte[] fileBytes = Base64.getDecoder().decode(session);

                                // 2️⃣ 写入文件
                                try (FileOutputStream fos = new FileOutputStream(pathSession.toFile())) {
                                    fos.write(fileBytes);
                                    fos.flush();
                                }
//                                Files.writeString(pathSession, session, StandardOpenOption.CREATE_NEW);
                                flag = true;
                                if (!files.contains(pathSession.toString())) {
                                    files.add(pathSession.toString());
                                }
                            }
                        }
                        // 压缩
                        if (flag && !files.isEmpty()) {
                            ZipUtil.zip(files, path2.toString() + ".zip");
                        }

                        // 删除
                        FileUtils.deleteFile(path2);
                    } catch (IOException e) {
                        log.error("groupTaskId: " + groupTask.get_id() + " 保存文件失败：" + e.getMessage());
                    }
                    if (groupTask.getResult() == null) {
                        groupTask.setResult(new HashMap());
                    }
                    groupTask.getResult().put("out", filepath.toString());
                    groupTask.getResult().put("out2", flag ? filepath2.toString() + ".zip" : "");
                } else {
                    groupTask.setStatus(GroupTaskStatusEnums.init.getCode());
                }
                groupTask = groupTaskRepository.save(groupTask);
            } else {
                if (subTaskList.isEmpty()) {
                    long success = subTaskRepository.countByGroupTaskIdEqualsAndStatusEquals(groupTask.get_id(), SubTaskStatusEnums.success.getCode());
                    long failed = subTaskRepository.countByGroupTaskIdEqualsAndStatusEquals(groupTask.get_id(), SubTaskStatusEnums.failed.getCode());
                    if (groupTask.getSuccess() != success) {
                        groupTask.setSuccess(success);
                    }
                    if (groupTask.getFailed() != failed) {
                        groupTask.setFailed(failed);
                    }

                    Calendar calendar = Calendar.getInstance();
                    calendar.add(Calendar.MINUTE, -2);
                    List<SubTask> restart = subTaskRepository.findRestart(groupTask.get_id(), calendar.getTime());
                    for (SubTask subTask : restart) {
                        try {
                            subTask.setUpdateTime(new Date());
                            subTask.setStatus(SubTaskStatusEnums.processing.getCode());
                            subTask.setAccid("");
                            subTaskRepository.save(subTask);
                        } catch (Exception e) {
                        }
                    }
                }
                groupTaskRepository.save(groupTask);
            }
        } catch (OptimisticLockingFailureException e) {
            log.info("{} OptimisticLockingFailureException: {}", groupTask.get_id(), e.getMessage());
        }
        return true;
    }

    @Override
    public boolean checkTask(SubTask task, Account account, Date now) {
        return true;
    }

    @Override
    public boolean runTask(SubTask task, Account account) {
        SubTask wt = null;
        try {
            wt = subTaskRepository.findById(task.get_id());
            if (wt == null) {
                log.info("id {} 任务不存在", task.get_id());
                return false;
            }
            if (!wt.getStatus().equals(SubTaskStatusEnums.init.getCode())) {
                log.info("id {} 任务状态不正确", task.get_id());
                return false;
            }
            GroupTask groupTask = groupTaskRepository.findById(task.getGroupTaskId()).orElse(null);
            if (groupTask == null) {
                this.reportTaskStatus(wt, SubTaskStatusEnums.failed, "任务已经结束");
                return false;
            }

            if (account == null || !account.getOnlineStatus().equals(AccountOnlineStatus.ONLINE.getCode())) {
                this.reportTaskStatus(wt, SubTaskStatusEnums.failed, "账号不在线");
                return false;
            }

            String tgnet = task.getParams().getOrDefault("tgnet", "").toString();
            String userconfing = task.getParams().getOrDefault("userconfing", "").toString();
            if (tgnet == null || userconfing == null) {
                this.reportTaskStatus(wt, SubTaskStatusEnums.failed, "文件不存在");
                return false;
            }
            File tgnetF =  new File(tgnet);
            File userconfF =  new File(userconfing);
            if (!tgnetF.exists() || !userconfF.exists()) {
                this.reportTaskStatus(wt, SubTaskStatusEnums.failed, "文件不存在");
                return false;
            }

            String convert = TgNetToSessionUtil.convert(TgNetToSessionUtil.fileToBase64(tgnet), TgNetToSessionUtil.fileToBase64(userconfing));
//            String convert = SessionParser.parse(TgNetToSessionUtil.fileToBase64(tgnet), TgNetToSessionUtil.fileToBase64(userconfing));
            if (StringUtil.isNotEmpty(convert)) {
                this.reportTaskStatus(wt, SubTaskStatusEnums.success, Map.of("session", convert));
            } else {
                this.reportTaskStatus(wt, SubTaskStatusEnums.failed, "转换失败");
            }
        } catch (Exception e) {
            this.reportTaskStatus(wt, SubTaskStatusEnums.failed, "转换失败" + e.getMessage());
            return false;
        }
        return false;
    }
}
