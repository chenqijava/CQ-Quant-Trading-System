package com.nyy.gmail.cloud.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;
import com.nyy.gmail.cloud.common.exception.CommonException;
import com.nyy.gmail.cloud.helper.BufferedRandomAccessFile;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import java.nio.charset.Charset;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

@Slf4j
@Component
public class FileUtils {
    public static Path resPath;

    public FileUtils(@Value("${config.resBak}") String resDir) {
        resPath = Paths.get(resDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(resPath);
        } catch (IOException e) {
            throw new RuntimeException("can not create res folder.");
        }
    }

    /**
     * 判断文件是否存在
     *
     * @param fileName
     * @return
     */
    public static Boolean exists(String fileName) {
        File file = resPath.resolve(fileName).toAbsolutePath().normalize().toFile();
        return file.exists();
    }

    public static File existsAndReturn(String fileName) {
        File file = resPath.resolve(fileName).toAbsolutePath().normalize().toFile();
        return file.exists() ? file : null;
    }

    public static <T> void writeText(List<T> text, String textPath, Boolean append) {
        if (text == null) {
            File file = new File(textPath);
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        try (FileWriter writer = new FileWriter(textPath, append)) {
            int size = text.size();
            int index = 0;
            for (T t : text) {
                String st = t.toString().trim();
                writer.write(st + System.lineSeparator());
            }
        } catch (IOException e) {
            log.error("写入文件错误",e);
            throw new CommonException("写入文件错误");
        }

    }


    /**
     * 写入文本文件(不追加)
     *
     * @param text
     * @param textPath
     */
    public static void writeText(List<String> text, String textPath) {
        writeText(text, textPath, false);
    }

    /**
     * 写入字符串到文本文件(不追加)
     *
     * @param line
     * @param textPath
     */
    public static void writeText(String line, String textPath) throws IOException {
        if (line == null) {
            File file = new File(textPath);
            file.createNewFile();
            return;
        }
        ArrayList<String> text = new ArrayList<>();
        text.add(line);
        writeText(text, textPath, false);
    }

    public static void deleteFile(File file) {
        if (file == null || !file.exists()) {
            return;
        }

        if (file.isFile()) {
            file.delete();
        } else if (file.isDirectory()) {
            File files[] = file.listFiles();
            for (File currentFile : files) {//遍历文件夹下的目录
                if (currentFile.isFile()) {//如果是文件而不是文件夹==>可直接删除
                    if (currentFile.delete()) {
                        log.info("删除文件：" + currentFile.getAbsolutePath());
                    } else {
                        log.error("删除文件失败：" + currentFile.getAbsolutePath());
                    }
                } else if (currentFile.isDirectory()) {
                    deleteFile(currentFile);//是文件夹,递归调用方法
                }
            }
            file.delete();
        }
    }

    public static void deleteFile(Path path) {
        deleteFile(path, null);
    }

    public static void deleteFile(Path path, Long beforeTime) {

        if (path == null) {
            return;
        }

        beforeTime = beforeTime == null ? 0 : beforeTime;

        File pathDir = path.toFile();
        if (!pathDir.exists() || !pathDir.isDirectory()) {
            return;
        }

        long currentTimeMillis = System.currentTimeMillis();
        File[] files = pathDir.listFiles();
        for (File file : files) {
            // 根据path获取文件的基本属性类
            Path filePath = Paths.get(file.getAbsolutePath());
            BasicFileAttributes attrs = null;
            try {
                attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
                 // 从基本属性类中获取文件创建时间
                FileTime fileTime = attrs.creationTime();
                if (fileTime == null) {
                    fileTime = attrs.lastModifiedTime();
                }
                // 将文件创建时间转成毫秒
                long millis = fileTime.toMillis();
                long existsTime = currentTimeMillis - millis;
                // System.out.println(existsTime);
                if (existsTime > beforeTime) {
                    log.info("开始删除文件:" + file.getAbsolutePath());
                    FileUtils.deleteFile(file);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取文件非空行行数
     *
     * @param file
     * @return
     */
    public static int getFileNonEmptyLineCount(File file) {
        int nonEmptyLineCount = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // 过滤空行
                if (!line.trim().isEmpty()) {
                    nonEmptyLineCount++;
                }
            }
        } catch (IOException e) {
            log.error(StrUtil.format("读取文件:【{}】非空行个数错误", file.getName()), e);
        }
        return nonEmptyLineCount;
    }

    // 从文件中读取第一行
    public static String readFirstLine(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            return reader.readLine();
        } catch (IOException e) {
            log.error(StrUtil.format("读取文件:【{}】第一行错误", file.getName()), e);
        }
        return null;
    }

    /**
     * 获取zip包中非空行文件个数
     *
     * @param zipFilePath
     * @return
     */
    public static int getZipFileNonEmptyLineFileCount(File zipFilePath) {
        int emptyLineFileCount = 0;
        try {
            ZipFile zipFile = new ZipFile(zipFilePath);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();

                // 检查文件是否为文本文件（可以根据需要修改判断条件）
                if (entry.getName().endsWith(".txt") && !entry.getName().startsWith("01 all")) {
                    int lineCount = 0;

                    InputStream inputStream = zipFile.getInputStream(entry);
                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                    BufferedReader reader = new BufferedReader(inputStreamReader);

                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (StrUtil.isNotBlank(line)) {
                            lineCount++;
                        }
                    }

                    if (lineCount > 0) {
                        emptyLineFileCount++;
                    }
                    reader.close();

                    System.out.println("文件 " + entry.getName() + " 的行数为: " + lineCount);
                }
            }

            zipFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return emptyLineFileCount;
    }

    public static boolean isZipFile(File file) {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            byte[] buffer = new byte[2];
            int bytesRead = fileInputStream.read(buffer);
            if (bytesRead == 2) {
                // 判断文件头的前两个字节是否是 "PK"（0x50 0x4B）
                return (buffer[0] == 0x50 && buffer[1] == 0x4B);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
     public static boolean isZipFile(String filePath) {
        return isZipFile(new File(filePath));
    }


    /**
     * 分割文件
     *
     * @param path        路径
     * @param file        原始文件
     * @param newFileName 新文件名称不含后缀
     * @param suffix      后缀
     * @param count       每个文件行数
     */
    public static List<File> splitFile(Path path, File file, String newFileName, String suffix, int count) throws IOException {
        List<File> files = new ArrayList<>();
        //分割文件,每个文件读取count行
        try(BufferedReader reader = new BufferedReader(new FileReader(file))){
            String line = null;
            int lineNum = 0;
            int fileNum = 1;
            List<String> lines = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
              lines.add(line);
              lineNum++;
              if (lineNum == count) {
                  String fileName = newFileName + fileNum + suffix;
                  File newFile = path.resolve(fileName).toAbsolutePath().toFile();
                  FileUtil.writeLines(lines, newFile, CharsetUtil.UTF_8);
                  files.add(newFile);
                  lines.clear();
                  lineNum = 0;
                  fileNum++;
              }
          }
          if (lines.size() > 0) {
              String fileName = newFileName + fileNum + suffix;
              File newFile = path.resolve(fileName).toAbsolutePath().toFile();
              FileUtil.writeLines(lines, newFile, CharsetUtil.UTF_8);
              files.add(newFile);
          }
          return files;
        }
    }

        /**
     * 将给定文件按照 linesAllocation 的行数分配拆分成多个文件，并返回分割后的文件列表。
     *
     * @param path            输出文件的目标目录（绝对路径或相对路径）
     * @param file            源文件 File 对象
     * @param newFileName     拆分后文件的名前缀（如 "data_"）
     * @param suffix          文件后缀（如 ".txt"）
     * @param linesAllocation 每个目标文件应写入的行数分配
     * @return 拆分后生成的文件列表
     * @throws IOException 文件读写异常
     */
    public static List<File> splitFile(Path path,
                                       File file,
                                       String newFileName,
                                       String suffix,
                                       List<Integer> linesAllocation) throws IOException {
        List<File> splittedFiles = new ArrayList<>(linesAllocation.size());
        // 获取 linesAllocation 最大值
        int maxLines = linesAllocation.stream().max(Integer::compareTo).orElse(0);
        List<String> chunk = new ArrayList<>(maxLines);

        // 使用 try-with-resources 自动关闭 reader
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            // 当前正在写的文件索引
            int currentFileIndex = 0;
            // 当前文件应写的行数
            int allocated = linesAllocation.get(currentFileIndex);
            // 用于暂存一批行，当其行数达到 allocated 时，写入一个新文件
            String line;
            while ((line = br.readLine()) != null) {
                chunk.add(line);

                // 当前 chunk 已满，写入文件并切换到下一个
                if (chunk.size() == allocated) {
                    String fileName = newFileName + (currentFileIndex + 1) + suffix;
                    File newFile = path.resolve(fileName).toAbsolutePath().toFile();

                    // 使用 Hutool 一次性写出
                    FileUtil.writeLines(chunk, newFile, CharsetUtil.UTF_8);
                    splittedFiles.add(newFile);
                    chunk.clear();
                    currentFileIndex++;
                }
            }

            // 如果最后还有剩余行，但没凑满 allocated，就仍需要写出到文件
            if (!chunk.isEmpty()) {
                String fileName = newFileName + (currentFileIndex + 1) + suffix;
                File newFile = path.resolve(fileName).toAbsolutePath().toFile();
                FileUtil.writeLines(chunk, newFile, CharsetUtil.UTF_8);
                splittedFiles.add(newFile);
            }
        }

        return splittedFiles;
    }

    //    如需异步调用，重新写函数添加@Async("taskExecutor")，并且不能设置为static
    public static void zipFile(List<String> files, String zipFileName) {
        try {
            ZipUtil.zip(files, FileUtils.resPath.resolve(zipFileName).toString());
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new CommonException("压缩文件生成失败");
        }
    }


    public static void mkdirs(String baseDir) {
        File file = new File(baseDir);
        if (!file.exists()) {
            file.mkdirs();
        }
    }


    public static List<String> getFiles(String sourceDirPath) {
        List<String> files = new ArrayList<>();
        File sourceDir = new File(sourceDirPath);
        if (!sourceDir.exists()) {
            return files;
        }
        File[] sourceFiles = sourceDir.listFiles();
        if (sourceFiles == null || sourceFiles.length == 0) {
            return files;
        }
        for (File sourceFile : sourceFiles) {
            if (sourceFile.isFile()) {
                files.add(sourceFile.getAbsolutePath());
            }   else    {
                files.addAll(getFiles(sourceFile.getAbsolutePath()));
            }
        }
        return files;
    }

    public static long getFileLineNum(String filePath) {
        try {
            return Files.lines(Paths.get(filePath)).count();
        } catch (IOException e) {
            return -1;
        }
    }

    public static void convertCharacterSet(String oldFile, String newFile, String oldCharset, String newCharset) {
        try {
            FileInputStream fis = new FileInputStream(oldFile);
            InputStreamReader isr = new InputStreamReader(fis, oldCharset);
            BufferedReader br = new BufferedReader(isr);

            FileOutputStream fos = new FileOutputStream(newFile);
            OutputStreamWriter osw = new OutputStreamWriter(fos, newCharset);
            BufferedWriter bw = new BufferedWriter(osw);

            String line;
            while ((line = br.readLine()) != null) {
                bw.write(line);
                bw.newLine();
            }

            // 关闭流
            br.close();
            bw.close();

            // 删除原文件
            FileUtils.deleteFile(new File(oldFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取文件扩展名
     * @param fileName
     * @return
     */
    public static String getFileExtension(String fileName) {
        if ( fileName == null ){
            return null;
        }
        File file = new File(fileName);
        if ( !file.exists() || file.isDirectory() ){
            return null;
        }
        fileName = file.getName();
        int index = fileName.lastIndexOf(".");
        if ( index == -1 || index == fileName.length() - 1 ){
            return null;
        }
        return fileName.substring(index + 1);
    }

    /**
     * 从文件中读取前指定行数内容
     * @param file
     * @param i 指定行数
     * @return
     */
    public static List<String> readFileCountLines(File file, int i) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
                count++;
                if (count == i) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }


    /**
     * gzip 压缩文件
     * @param sourceFile 输入txt文件路径
     * @param outputFile 压缩后文件路径
     */
    public static void gzip(String sourceFile, String outputFile) {

        try (FileInputStream fis = new FileInputStream(sourceFile);
             FileOutputStream fos = new FileOutputStream(outputFile);
             GZIPOutputStream gzipOS = new GZIPOutputStream(fos)) {

            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                gzipOS.write(buffer, 0, length);
            }
            log.info("gzip success,src:【{}】,desc:【{}】", sourceFile, outputFile);
        } catch (Exception e) {
            log.error(StrUtil.format("gzip error,src:【{}】,desc:【{}】", sourceFile, outputFile), e);
        }
    }


    public static void deduplicateCSVFile(String filePath) {
        // 创建一个临时文件，用于存储去重后的内容
        File tempFile = new File(filePath + ".tmp");

        try {
            // 指定字符集，根据你的文件编码进行调整
            Charset charset = Charset.forName("UTF-8");

            // 创建CsvReader对象读取CSV文件
            CsvReader csvReader = new CsvReader(filePath, ',', charset);
            // 创建CsvWriter对象写入临时文件
            CsvWriter csvWriter = new CsvWriter(new FileWriter(tempFile), ',');

            // 使用HashSet来跟踪已经出现过的第一列的值
            Set<String> seenKeys = new HashSet<>();

            // 逐行读取CSV文件
            while (csvReader.readRecord()) {
                // 获取当前行的所有列
                String[] rowValues = csvReader.getValues();

                if (rowValues.length > 0) {
                    // 获取第一列的值
                    String key = rowValues[0];

                    // 如果该值未出现过，写入临时文件并记录
                    if (!seenKeys.contains(key)) {
                        seenKeys.add(key);
                        // 将整行写入CsvWriter
                        csvWriter.writeRecord(rowValues);
                    }
                }
            }
            // 关闭CsvReader和CsvWriter
            csvReader.close();
            csvWriter.close();
        } catch (IOException e) {
            log.error(StrUtil.format("去重文件【{}】失败", filePath), e);
            return;
        }

        // 删除原始文件
        File originalFile = new File(filePath);
        if (originalFile.delete()) {
            // 将临时文件重命名为原文件名
            if (!tempFile.renameTo(originalFile)) {
                log.error("无法重命名临时文件: {}为原文件名: {}。", tempFile, filePath);
            } else {
                log.info("去重完成，原文件: {} 已去重。", filePath);
            }
        } else {
            log.error("无法删除原始文件: {}。", filePath);
        }
    }

    public static long fileLineCount(Path path) {
        long count = 0;
        // try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile()))) {
        //     while (reader.readLine() != null) {
        //         count++;
        //     }
        // } catch (IOException e) {
        //     throw new CommonException("读取文件行数失败");
        // }

        try (BufferedRandomAccessFile file = new BufferedRandomAccessFile(path.toFile(), "r", 8192)) {
          while (file.getNextLine() != null) {
            count++;
          }
        } catch (IOException e) {
            throw new CommonException("读取文件行数失败");
        }
        return count;
    }

    public static Pair<List<String>, Long> readFileLines(Path path, long start, int count) {
        // try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile()))) {
        //     List<String> lines = new ArrayList<>();
        //     for (int i = 0; i < start; i++) {
        //         if(reader.readLine() == null){
        //             return lines;
        //         }
        //     }
        //     for (int i = 0; i < count; i++) {
        //         String line = reader.readLine();
        //         if(line == null){
        //             break;
        //         }
        //         lines.add(line);
        //     }
        //     return lines;
        // } catch (IOException e) {
        //     throw new RuntimeException("readFileLines error");
        // }
        try (BufferedRandomAccessFile file = new BufferedRandomAccessFile(path.toFile(), "r", 8192)) {
            file.seek(start);
            List<String> lines = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                String line = file.getNextLine();
                if (line == null) {
                    break;
                }
                if (StringUtils.isNotBlank(line)) {
                    lines.add(line);
                }
            }
            return Pair.of(lines, file.getFilePointer());
        } catch (IOException e) {
            throw new RuntimeException("readFileLines error");
        }
    }

    public static void writeFileLines(Path path, List<String> lines) {
        writeFileLines(path, lines, false);
    }

    public static void writeFileLines(Path path, List<String> lines, boolean append) {
        File file = path.toFile();
        if (!file.exists()) {
            file.getParentFile().mkdirs();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path.toFile(), append))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeCsv(Path path, List<String[]> data) {
        writeCsv(path, data, false);
    }

    public static void writeCsv(Path path, List<String[]> data, boolean append) {
        File file = path.toFile();
        if (!file.exists()) {
            file.getParentFile().mkdirs();
        }

        try (CSVWriter writer = new CSVWriter(new FileWriter(file, append))) {
            writer.writeAll(data, false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<String[]> readCsv(Path path) {
        try (CSVReader reader = new CSVReader(new FileReader(path.toFile()))) {
            return reader.readAll();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void makeSureNewFile(Path path) {
        File file = path.toFile();
        if (file.exists()) {
            file.delete();
        }
        try {
            file.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 读取csv文件行数
     * @param filePath
     * @return
     */
    public static int readCsvFileLineCount(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            int count = 0;
            while (reader.readLine() != null) {
                count++;
            }
            return count;
        } catch (IOException e) {
            log.error("读取文件行数失败", e);
        }
        return 0;
    }

    /**
     * 读取csv文件行数(title,空行不计数)
     * @param file
     * @return
     */
    public static int getCSVFileLineCount(File file) {
        int nonEmptyLineCount = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // 过滤空行
                if (!line.trim().isEmpty()) {
                    nonEmptyLineCount++;
                }
            }
        } catch (IOException e) {
            log.error(StrUtil.format("读取文件:【{}】非空行个数错误", file.getName()), e);
        }
        return nonEmptyLineCount;
    }

    // 根据文件行数,最小行数,最大行数,获取文件行数分配
    public static List<Integer> getLinesAllocation(int totalLines, int minLineNum, int maxLineNum) {
        // 根据总行数，计算拆分后大致需要的文件数
        int fileCount = (int) Math.ceil((double) totalLines / maxLineNum);
        // 计算每个文件的基础行数
        int baseLinesPerFile = totalLines / fileCount;
        // 计算余数（分配给若干文件，每个多加 1 行，使分配更均匀）
        int remainder = totalLines % fileCount;
        int[] linesAllocation = new int[fileCount];
        for (int i = 0; i < fileCount; i++) {
            linesAllocation[i] = baseLinesPerFile;
        }
        // 将余数部分均匀地分配到前 remainder 个文件，每个文件多加 1 行
        for (int i = 0; i < remainder; i++) {
            linesAllocation[i]++;
        }
        return Arrays.stream(linesAllocation).boxed().collect(Collectors.toList());
    }

    public static int removeFileEmptyLine(File file) throws IOException {
        int totalLineCount = 0;
        int emptyLineCount = 0;
        File tempFile = new File(file.getAbsolutePath() + ".tmp");
        try (BufferedReader reader = new BufferedReader(new FileReader(file));
             BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    totalLineCount++;
                    writer.write(line);
                    writer.newLine();
                } else {
                    emptyLineCount++;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        log.info("移除空行成功,新文件:【{}】,空行个数:【{}】,总行数:【{}】", file.getAbsolutePath(), emptyLineCount, totalLineCount);
        return totalLineCount;
    }

    public static String extractFilename(String path, int maxLen) {
        String fileName = Paths.get(path).getFileName().toString();
        int pos = fileName.lastIndexOf('.');
        if (pos > 0) {
            fileName = fileName.substring(0, pos);
        }
        if (fileName.length() > maxLen) {
            fileName = fileName.substring(0, maxLen);
        }
        return fileName;
    }
}

