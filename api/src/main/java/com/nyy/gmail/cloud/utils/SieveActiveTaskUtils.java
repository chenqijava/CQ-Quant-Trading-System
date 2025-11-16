package com.nyy.gmail.cloud.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.nyy.gmail.cloud.entity.mongo.GroupTask;
import com.nyy.gmail.cloud.tasks.sieve.bo.SieveActiveTaskResult;
import com.nyy.gmail.cloud.tasks.sieve.enums.SieveActiveTaskResultTypeEnum;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class SieveActiveTaskUtils {

    public static Path getSplitPath(String taskId) {
        return FileUtils.resPath.resolve("task").resolve(taskId);
    }

    public static List<File> splitFiles(String taskId, File file, int dataCount, List<Integer> linesAllocation) {
        List<File> files = null;
        log.info("拆分文件,任务ID:【{}】,数据个数:【{}】,阈值个数:【{}】", taskId, dataCount, linesAllocation);
        Path path = getSplitPath(taskId).resolve("data");

        //拆分文件
        String fileName = file.getName();
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        String prefix = fileName.substring(0, fileName.lastIndexOf("."));
        String newFileName = prefix + "_";
        try {
            files = FileUtils.splitFile(path, file, newFileName, suffix, linesAllocation);
        } catch (IOException e) {
            log.error(StrUtil.format("任务:【{}】,拆分文件错误", taskId), e);
            return null;
        }
        return files;
    }

    public static Path getResultDir(Path parentPath) {
        return parentPath.resolve("result");
    }

    public static Path getResultDir(Path parentPath, String groupTaskId) {
        return getResultDir(parentPath).resolve(groupTaskId);
    }

    public static Path getParentPath(GroupTask groupTask) {
        return FileUtils.resPath.resolve("task").resolve(groupTask.get_id());
    }

    public static Path getTotalResultDir(Path parentPath) {
        return getResultDir(parentPath).resolve("total");
    }

    public static File getTotalResultFile(Path parentPath, String filename) {
        // result 自动化结果
        // total 总的合并结果
        Path resultPath = getTotalResultDir(parentPath);
        File file = resultPath.toFile();
        if (!file.exists()) {
            file.mkdirs();
        }
        return resultPath.resolve(filename).toAbsolutePath().toFile();
    }

    public static Map<SieveActiveTaskResultTypeEnum, String> getTaskResultTypeEnumStringMap
            (List<SieveActiveTaskResultTypeEnum> orderTaskResultTypeEnums, String resultFilenameWithoutExt) {
        Map<SieveActiveTaskResultTypeEnum, String> typeFilenameMap = new HashMap<>();
        orderTaskResultTypeEnums.forEach(type -> {
            switch (type) {
                case success:
                    typeFilenameMap.put(type, StrUtil.format("{}-成功账号-{}.csv", resultFilenameWithoutExt));
                    break;
                case failed:
                    typeFilenameMap.put(type, StrUtil.format("{}-未成功账号-{}.txt", resultFilenameWithoutExt));
                    break;
                case unexecute:
                    typeFilenameMap.put(type, StrUtil.format("{}-未筛数据-{}.txt", resultFilenameWithoutExt));
                    break;
                case forbidden:
                    typeFilenameMap.put(type, StrUtil.format("{}-禁用数据-{}.txt", resultFilenameWithoutExt));
                    break;
                case unknown:
                    typeFilenameMap.put(type, StrUtil.format("{}-未知数据-{}.txt", resultFilenameWithoutExt));
                    break;
            }
        });

        return typeFilenameMap;
    }

}
