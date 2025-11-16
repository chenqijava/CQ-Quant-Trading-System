package com.nyy.gmail.cloud.tasks.sieve;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.nyy.gmail.cloud.entity.mongo.GroupTask;
import com.nyy.gmail.cloud.entity.mongo.SubTask;
import com.nyy.gmail.cloud.enums.GroupTaskStatusEnums;
import com.nyy.gmail.cloud.enums.GroupTaskUserActionEnums;
import com.nyy.gmail.cloud.enums.SubTaskStatusEnums;
import com.nyy.gmail.cloud.model.bo.UploadFileBO;
import com.nyy.gmail.cloud.repository.mongo.GroupTaskRepository;
import com.nyy.gmail.cloud.repository.mongo.SubTaskRepository;
import com.nyy.gmail.cloud.tasks.sieve.bo.SieveActiveGroupTaskParams;
import com.nyy.gmail.cloud.tasks.sieve.bo.SieveActiveSubTaskParams;
import com.nyy.gmail.cloud.tasks.sieve.bo.SieveActiveTaskResult;
import com.nyy.gmail.cloud.tasks.sieve.dto.DownloadTaskDTO;
import com.nyy.gmail.cloud.tasks.sieve.dto.OpTaskDTO;
import com.nyy.gmail.cloud.tasks.sieve.dto.QueryTaskResponseDTO;
import com.nyy.gmail.cloud.tasks.sieve.dto.SendTaskDTO;
import com.nyy.gmail.cloud.tasks.sieve.enums.SieveActiveTaskResultTypeEnum;
import com.nyy.gmail.cloud.utils.FileUtils;
import com.nyy.gmail.cloud.utils.SieveActiveTaskUtils;
import com.nyy.gmail.cloud.utils.TaskUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Slf4j
@Component
public abstract class AbstractSieveActiveTask implements SieveActiveTask {
    @Autowired
    protected GroupTaskRepository groupTaskRepository;
    @Autowired
    protected SubTaskRepository subTaskRepository;
    @Autowired
    protected Executor taskThreadPool;
    @Autowired
    protected TaskUtil taskUtil;
    @Autowired
    protected SieveActiveApi sieveActiveApi;

    private static final List<SieveActiveTaskResultTypeEnum> sieveActiveTaskResultTypeEnums =  List.of(
            SieveActiveTaskResultTypeEnum.success,
            SieveActiveTaskResultTypeEnum.failed,
            SieveActiveTaskResultTypeEnum.unexecute
    );

    @Override
    public void publishTask(GroupTask groupTask) {
        SieveActiveGroupTaskParams groupTaskParams = groupTask.getParamBean(SieveActiveGroupTaskParams.class);
        String dataFile = groupTaskParams.getDataFilePath();
        File file = FileUtils.resPath.resolve(dataFile).toFile();
        int dataCount = FileUtils.getFileNonEmptyLineCount(file);
        int perTaskCount = this.maxDataCountPerTask();
        List<Integer> linesAllocation = FileUtils.getLinesAllocation(dataCount, perTaskCount, perTaskCount);
        //拆分文件
        List<File> files = SieveActiveTaskUtils.splitFiles(groupTask.get_id(), file, dataCount, linesAllocation);
        if (CollectionUtils.isEmpty(files)) {
            log.warn("任务ID:【{}】,拆分文件出错", groupTask.get_id());
            return;
        }

        List<SubTask> subTasks = new ArrayList<>(files.size());
        for (int i = 0; i < files.size(); i++) {
            SieveActiveSubTaskParams subTaskParams = new SieveActiveSubTaskParams()
                    .setIndex(i)
                    .setProject(groupTaskParams.getProject())
                    .setDataFilePath(FileUtils.resPath.relativize(Paths.get(files.get(i).getAbsolutePath())).toString())
                    .setDataTotal((long) FileUtils.getFileNonEmptyLineCount(files.get(i)));
            SubTask subTask = taskUtil.newTaskModel(groupTask, Map.of(), subTaskParams.toMap(), new Date());
            subTasks.add(subTask);
        }
        subTaskRepository.batchInsert(subTasks);

        groupTask.setStatus(GroupTaskStatusEnums.processing.getCode());
        groupTask.setPublishStatus(GroupTaskStatusEnums.success.getCode());
//        groupTask.setFinishTime(new Date());
        groupTaskRepository.save(groupTask);
        log.info("任务ID:【{}】,发布完成，子任务数量:{}", groupTask.get_id(), subTasks.size());
    }

    @Override
    public void runTask(GroupTask groupTask) {
        if (GroupTaskStatusEnums.success.getCode().equals(groupTask.getStatus()) || GroupTaskStatusEnums.failed.getCode().equals(groupTask.getStatus())) {
            return;
        }
        List<SubTask> subTasks = findPublishedSubTasks(groupTask);
        if (subTasks.isEmpty()) {
            log.info("任务ID:【{}】,没有待执行的子任务了，修改状态为init", groupTask.get_id());
            groupTask.setStatus(GroupTaskStatusEnums.init.getCode());
            groupTaskRepository.save(groupTask);
        }

        for (SubTask subTask: subTasks) {
            try {
                runSubTask(groupTask, subTask);
            } catch (Exception e) {
                log.error(StrUtil.format("任务ID:【{}】, 子任务ID:【{}】,执行失败", groupTask.get_id(), subTask.get_id()), e);
            }
        }
    }

    protected void runSubTask(GroupTask groupTask, SubTask subTask) {
        log.info("任务ID:【{}】，子任务ID:【{}】,开始执行筛活任务", groupTask.get_id(), subTask.get_id());
        String key = StrUtil.format("runSubTask-{}", subTask.get_id());
        synchronized (key.intern()) {
            SieveActiveSubTaskParams subTaskParams = subTask.getParamBean(SieveActiveSubTaskParams.class);
            String project = subTaskParams.getProject();
            String orderId = subTask.get_id();

            SendTaskDTO sendTaskDTO = new SendTaskDTO();
            //上传文件
            File file = FileUtils.resPath.resolve(subTaskParams.getDataFilePath()).toFile();
            try {
                UploadFileBO uploadFileBO = sieveActiveApi.uploadFile(file);
                sendTaskDTO.setDataFile(uploadFileBO.getFilepath());
                sendTaskDTO.setDataFileName(uploadFileBO.getName());
            } catch (Exception e) {
                String msg = StrUtil.format("任务ID:【{}】,子任务ID:【{}】,主控上传文件失败,上传文件:【{}】", groupTask.get_id(), subTask.get_id(), file.getName());
                log.error(msg, e);
                throw new RuntimeException(e);
            }

            sendTaskDTO.setOrderId(orderId);
            sendTaskDTO.setProject(project);
            sendTaskDTO.setType("sieveActivate");
            sendTaskDTO.setDataType("email");
            sendTaskDTO.setTotal(subTaskParams.getDataTotal().intValue());
            sendTaskDTO.setIdentify("false");
            sendTaskDTO.setExportDeleted(true);//对所有接口都导出是否冻结列,不过给用户的结果,根据订单中参数判断是否加这一列
            sendTaskDTO.setTaskName(StrUtil.format("gmail-cloud筛开通_{}", file.getName()));

            try {
                String groupTaskId = sieveActiveApi.postTask(sendTaskDTO);
                if (groupTaskId == null) {
                    log.error(StrUtil.format("任务ID:【{}】,子任务ID:{},请求筛活接口失败", groupTask.get_id(), subTask.get_id()));
                    return;
                }
                subTaskParams.setGroupTaskId(groupTaskId);
                subTaskParams.setStartTime(new Date());
                subTask.setStatus(SubTaskStatusEnums.doing.getCode());

                List<SieveActiveTaskResultTypeEnum> taskResultTypes = getTaskResultTypes();
                List<SieveActiveTaskResult> orderTaskResult = new ArrayList<>(taskResultTypes.size());
                taskResultTypes.forEach(taskResultType -> {
                    orderTaskResult.add(new SieveActiveTaskResult().setType(taskResultType));
                });
                subTaskParams.setResults(orderTaskResult);
                subTask.setParamsBean(subTaskParams);
            } catch (Exception e) {
                log.error(StrUtil.format("任务ID:【{}】,子任务ID:【{}】,请求筛活接口失败", groupTask.get_id(), subTask.get_id()), e);
            }
            subTaskRepository.save(subTask);
            log.info("任务ID:【{}】,子任务ID【{}】,状态:【{}】,本次筛活任务结束", groupTask.get_id(), subTask.get_id(), subTask.getStatus());
        }
    }

    @Override
    public void checkTask(GroupTask groupTask) {
        if (GroupTaskStatusEnums.success.getCode().equals(groupTask.getStatus()) || GroupTaskStatusEnums.failed.getCode().equals(groupTask.getStatus())) {
            return;
        }

        List<CompletableFuture<Void>> futures = findRunningSubTasks(groupTask).stream().map(subTask -> CompletableFuture.runAsync(() -> {
            try {
                checkSubTask(groupTask, subTask);
            } catch (Exception e) {
                log.error(StrUtil.format("任务ID:【{}】, 子任务ID:【{}】,检查失败", groupTask.get_id(), subTask.get_id()), e);
            }
        }, taskThreadPool)).toList();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 检查是否全部完成
        List<SubTask> subTasks = findAllSubTasks(groupTask);
        boolean finished = subTasks.stream().allMatch(subTask ->
                SubTaskStatusEnums.success.getCode().equals(subTask.getStatus())
                        || SubTaskStatusEnums.failed.getCode().equals(subTask.getStatus())
        );
        log.info("任务ID:【{}】,主控GroupTask全部完成?结果:【{}】", groupTask.get_id(), finished);

        long total = 0L;
        long success = 0L;
        long failed = 0L;
        long unexecuteCount = 0L;
        long validDataCount = 0L;
        for (SubTask subTask: subTasks) {
            SieveActiveSubTaskParams subTaskParams = subTask.getParamBean(SieveActiveSubTaskParams.class);
            total += subTaskParams.getTotal() != null ? subTaskParams.getTotal() : 0L;
            success += subTaskParams.getSuccess() != null ? subTaskParams.getSuccess() : 0L;
            failed += subTaskParams.getFailed() != null ? subTaskParams.getFailed() : 0L;
            unexecuteCount += subTaskParams.getUnexecuteCount() != null ? subTaskParams.getUnexecuteCount() : 0L;
            validDataCount += subTaskParams.getValidDataCount() != null ? subTaskParams.getValidDataCount() : 0L;
        }
        SieveActiveGroupTaskParams groupTaskParams = groupTask.getParamBean(SieveActiveGroupTaskParams.class);
        groupTask.setSuccess(success);
        groupTask.setFailed(failed);
        groupTaskParams.setUnexecuteCount(unexecuteCount);
        groupTaskParams.setValidDataCount(validDataCount);
        if (finished) {
            this.finishTask(groupTask, groupTaskParams, subTasks);
        }
        groupTask.setParamsBean(groupTaskParams);
        groupTaskRepository.save(groupTask);
    }

    protected void checkSubTask(GroupTask groupTask, SubTask subTask) {
        log.info("任务ID:【{}】,子任务ID:【{}】,开始检查筛活任务", groupTask.get_id(), subTask.get_id());
        //任务结果文件夹
        Path parentPath = SieveActiveTaskUtils.getParentPath(groupTask);
        String key = StrUtil.format("checkSubTask-{}", subTask.get_id());
        synchronized (key.intern()) {
            SieveActiveSubTaskParams subTaskParams = subTask.getParamBean(SieveActiveSubTaskParams.class);
            String groupTaskId = subTaskParams.getGroupTaskId();
            try {
                OpTaskDTO opTaskDTO = new OpTaskDTO().setGroupTaskId(groupTaskId);
                opTaskDTO.setGroupTaskId(groupTaskId);
                QueryTaskResponseDTO taskResponse = sieveActiveApi.query(opTaskDTO);
                log.info("任务ID:【{}】,子任务ID:【{}】,GroupTaskID:【{}】,status:【{}】,成功个数:【{}】,失败个数:【{}】", groupTask.get_id(),
                        subTask.get_id(), groupTaskId, taskResponse.getStatus(), taskResponse.getSuccess(), taskResponse.getFailed());
                //读取结果
                subTaskParams.setTotal(Long.valueOf(taskResponse.getTotal()));
                subTaskParams.setSuccess(Long.valueOf(taskResponse.getSuccess()));
                subTaskParams.setFailed(Long.valueOf(taskResponse.getFailed()));
                subTaskParams.setUnexecuteCount(taskResponse.getUnexecuteCount());
                subTaskParams.setValidDataCount(taskResponse.getValidDataCount());
                subTaskParams.setException(taskResponse.isException());
                if ("success".equals(taskResponse.getStatus()) && "success".equals(taskResponse.getPublishStatus())) {
                    log.info("任务ID:【{}】,子任务ID:【{}】完成,开始下载GroupTaskID:【{}】文件...", groupTask.get_id(), subTask.get_id(), groupTaskId);
                    if (taskResponse.getSuccess() + taskResponse.getFailed() != taskResponse.getTotal()) {
                        log.error("任务ID:【{}】,子任务ID:【{}】,GroupTask:【{}】数量不正确,总数:【{}】,成功数:【{}】,失败数:【{}】,未同步数:【{}】",
                                groupTask.get_id(), subTask.get_id(), groupTaskId, taskResponse.getTotal(),
                                taskResponse.getSuccess(), taskResponse.getFailed(), taskResponse.getUnexecuteCount());
                        return;
                    }
                    //api接口返回结果文件路径
                    List<SieveActiveTaskResult> results = subTaskParams.getResults();
                    Map<SieveActiveTaskResultTypeEnum, SieveActiveTaskResult> resultMap = taskResponse.getResults().stream().collect(Collectors.toMap(SieveActiveTaskResult::getType, a -> a));
                    boolean downloadFinished = results.stream().allMatch(sieveTaskResult -> {
                        try {
                            DownloadTaskDTO downloadTaskDTO = new DownloadTaskDTO();
                            downloadTaskDTO.setGroupTaskId(groupTaskId)
                                    .setParentPath(parentPath)
                                    .setType(sieveTaskResult.getType())
                                    .setDataFilepath(FileUtils.resPath.resolve(subTaskParams.getDataFilePath()).toString())
                            ;
                            // 主控结果是返回的文件路径,空号检测不会返回路径
                            if (resultMap.containsKey(sieveTaskResult.getType())) {
                                downloadTaskDTO.setFilepath(resultMap.get(sieveTaskResult.getType()).getFilepath());
                            }
                            File file = sieveActiveApi.downloadFile(downloadTaskDTO);
                            sieveTaskResult.setName(file.getName());
                            sieveTaskResult.setFilepath(FileUtils.resPath.relativize(Paths.get(file.getAbsolutePath())).toString());
                            // csv 读取第一行,判断是否有标题
                            int count = FileUtils.getFileNonEmptyLineCount(file);
                            // 读取第一行,
                            if (count > 0) {
                                String firstLine = FileUtils.readFirstLine(file);
                                // 非邮箱,不用减一个
                                if (StrUtil.isNotBlank(firstLine) && !firstLine.contains("@")) {
                                    // 判断是否有大于5位数的数字,如果有,则认为是手机号,count不变,如果没有认为是表头,count -1
                                    if (!firstLine.matches(".*\\d{5,}.*")) {
                                        count = count - 1;
                                    }
                                }
                            }
                            sieveTaskResult.setCount(count);
                            return true;
                        } catch (Exception e) {
                            String msg = StrUtil.format("任务ID:【{}】,子任务ID:【{}】,下载GroupTaskID:【{}】,下载文件类型:【{}】失败",
                                    groupTask.get_id(), subTask.get_id(), groupTaskId, sieveTaskResult.getType());
                            log.error(msg, e);
                        }
                        return false;
                    });

                    if (downloadFinished) {
                        log.info("任务ID:【{}】,子任务ID:【{}】,下载GroupTaskID:【{}】文件结束", groupTask.get_id(), subTask.get_id(), groupTaskId);
                        subTask.setFinishTime(new Date());
                        subTask.setStatus(SubTaskStatusEnums.success.getCode());
                    }
                }
                subTask.setParamsBean(subTaskParams);
                subTaskRepository.save(subTask);
            } catch (Exception e) {
                String msg = StrUtil.format("任务ID:【{}】,子任务ID:【{}】,查询GroupTaskID:【{}】结果失败", groupTask.get_id(), subTask.get_id(), groupTaskId);
                log.error(msg, e);
            }
        }
    }

    @Override
    public void stopTask(GroupTask groupTask) {
        List<SubTask> subTasks = findAllSubTasks(groupTask);
        subTasks.forEach(subTask -> {
            if (SubTaskStatusEnums.success.getCode().equals(subTask.getStatus()) || SubTaskStatusEnums.failed.getCode().equals(subTask.getStatus())) {
                return;
            }

            SieveActiveSubTaskParams subTaskParams = subTask.getParamBean(SieveActiveSubTaskParams.class);
            try {
                if (StringUtils.isNotEmpty(subTaskParams.getGroupTaskId())) {
                    sieveActiveApi.stop(new OpTaskDTO().setGroupTaskId(subTaskParams.getGroupTaskId()));
                }
            } catch (Exception e) {
                log.error(StrUtil.format("任务ID:【{}】,子任务ID:【{}】,主控任务ID:【{}】,停止任务失败", groupTask.get_id(), subTask.get_id(), subTaskParams.getGroupTaskId()), e);
            }
        });
    }

    @Override
    public void forceStopTask(GroupTask groupTask) {
        List<SubTask> subTasks = findAllSubTasks(groupTask);
        subTasks.forEach(subTask -> {
            if (SubTaskStatusEnums.success.getCode().equals(subTask.getStatus()) || SubTaskStatusEnums.failed.getCode().equals(subTask.getStatus())) {
                return;
            }

            SieveActiveSubTaskParams subTaskParams = subTask.getParamBean(SieveActiveSubTaskParams.class);
            try {
                if (StringUtils.isNotEmpty(subTaskParams.getGroupTaskId())) {
                    sieveActiveApi.stop(new OpTaskDTO().setGroupTaskId(subTaskParams.getGroupTaskId()));
                }
            } catch (Exception e) {
                log.error(StrUtil.format("任务ID:【{}】,子任务ID:【{}】,主控任务ID:【{}】,停止任务失败", groupTask.get_id(), subTask.get_id(), subTaskParams.getGroupTaskId()), e);
            }
            subTask.setFinishTime(new Date());
            subTask.setStatus(SubTaskStatusEnums.failed.getCode());
            subTaskRepository.save(subTask);
        });

        groupTask.setStatus(GroupTaskStatusEnums.failed.getCode());
        groupTask.setResult(Map.of("msg", "手动取消任务"));
        groupTask.setFinishTime(new Date());
        groupTask.setUserAction(GroupTaskUserActionEnums.ForceFinish.getCode());
        groupTaskRepository.save(groupTask);
    }

    protected void finishTask(GroupTask groupTask, SieveActiveGroupTaskParams groupTaskParams, List<SubTask> subTasks) {
        String fileName = FileUtils.extractFilename(groupTaskParams.getDataFilePath(), 100);

        List<SieveActiveTaskResultTypeEnum> sieveTaskResultTypeEnums = getTaskResultTypes();
        Map<SieveActiveTaskResultTypeEnum, String> typeFilenameMap = SieveActiveTaskUtils.getTaskResultTypeEnumStringMap(sieveTaskResultTypeEnums, fileName);
        Map<SieveActiveTaskResultTypeEnum, List<SieveActiveTaskResult>> typeOrderTaskResultsMap = new HashMap<>();
        sieveTaskResultTypeEnums.forEach(type ->
                typeOrderTaskResultsMap.put(type, new ArrayList<>())
        );
        // 只过滤完成的订单
        subTasks.stream().filter(subTask -> SubTaskStatusEnums.success.getCode().equals(subTask.getStatus())).forEach(subTask -> {
            subTask.getParamBean(SieveActiveSubTaskParams.class).getResults().forEach(sieveTaskResult -> {
                typeOrderTaskResultsMap.get(sieveTaskResult.getType()).add(sieveTaskResult);
            });
        });
        List<String> cloudResultFiles = mergeFiles(groupTask, typeOrderTaskResultsMap, typeFilenameMap);
        groupTaskParams.setResults(cloudResultFiles);
        groupTask.setFinishTime(new Date());
        groupTask.setStatus(GroupTaskStatusEnums.success.getCode());
    }

    protected List<String> mergeFiles(GroupTask groupTask, Map<SieveActiveTaskResultTypeEnum, List<SieveActiveTaskResult>> typeOrderTaskResultsMap, Map<SieveActiveTaskResultTypeEnum, String> typeFilenameMap) {
        List<String> cloudResultFiles = new ArrayList<>();
        Path parentPath = SieveActiveTaskUtils.getParentPath(groupTask);
        Path resPath = FileUtils.resPath;
        //结果文件目录

        typeOrderTaskResultsMap.forEach((type, orderTaskResults) -> {
            if (typeFilenameMap.containsKey(type)) {
                Integer total = orderTaskResults.stream().mapToInt(SieveActiveTaskResult::getCount).sum();
                String filename = StrUtil.format(typeFilenameMap.get(type), total);
                File file = SieveActiveTaskUtils.getTotalResultFile(parentPath, filename);
                //如果文件已经存在删除文件
                if (file.exists()) {
                    file.delete();
                }

                String title = "";
                if (SieveActiveTaskResultTypeEnum.success == type) {
                    title = getTitle(groupTask);
                } else if (SieveActiveTaskResultTypeEnum.failed == type
                        || SieveActiveTaskResultTypeEnum.forbidden == type
                        || SieveActiveTaskResultTypeEnum.unknown == type
                        || SieveActiveTaskResultTypeEnum.unexecute == type
                ) {
                    title = getFailTitle();
                }
                FileUtil.writeUtf8String(title, file);
                orderTaskResults.forEach(orderTaskResult -> {
                    File srcFile = resPath.resolve(orderTaskResult.getFilepath()).toFile();
                    //把srcFile 文件合并到file里
                    try {
                        List<String> lines = FileUtil.readUtf8Lines(srcFile);
                        FileUtil.appendUtf8Lines(lines, file);
                    } catch (Exception e) {
                        log.error("", e);
                    }
                });
                cloudResultFiles.add(resPath.relativize(Paths.get(file.getAbsolutePath())).toString());
            }
        });
        return cloudResultFiles;
    }

    public String getTitle(GroupTask orderTask) {
        return "\uFEFF邮箱";
    }

    public String getFailTitle() {
        return "\uFEFF邮箱";
    }

    public List<SubTask> findAllSubTasks(GroupTask groupTask) {
        return subTaskRepository.findAllByGroupTaskId(groupTask.get_id());
    }

    public List<SubTask> findPublishedSubTasks(GroupTask groupTask) {
        return subTaskRepository.findAllByGroupTaskIdAndStatus(groupTask.get_id(), SubTaskStatusEnums.processing.getCode());
    }

    public List<SubTask> findRunningSubTasks(GroupTask groupTask) {
        return subTaskRepository.findAllByGroupTaskIdAndStatus(groupTask.get_id(), SubTaskStatusEnums.doing.getCode());
    }

    protected List<SieveActiveTaskResultTypeEnum> getTaskResultTypes() {
        return sieveActiveTaskResultTypeEnums;
    }

    protected int maxDataCountPerTask() {
        return 500_000;
    }
}
