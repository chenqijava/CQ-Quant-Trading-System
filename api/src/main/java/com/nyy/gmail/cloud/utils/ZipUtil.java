package com.nyy.gmail.cloud.utils;


import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;
import org.apache.tools.zip.ZipOutputStream;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;
import java.util.*;

public class ZipUtil {
    private static int BUFFERSIZE = 1024;

    public static Boolean modifyImage(String path, int w, int h) {
        File file = new File(path);
        try (InputStream io = new FileInputStream(file);) {
            return zoomImage(io, path, 200, 200);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static synchronized void unzip(String zipFileName, String extPlace) {
        try {
            (new File(extPlace)).mkdirs();
            File f = new File(zipFileName);
            ZipFile zipFile = new ZipFile(zipFileName);
            if ((!f.exists()) && (f.length() <= 0)) {
                throw new Exception("要解压的文件不存在!");
            }
            String strPath, gbkPath, strtemp;
            File tempFile = new File(extPlace);
            strPath = tempFile.getAbsolutePath();
            Enumeration e = zipFile.getEntries();
            while (e.hasMoreElements()) {
                org.apache.tools.zip.ZipEntry zipEnt = (org.apache.tools.zip.ZipEntry) e.nextElement();
                gbkPath = zipEnt.getName();
                if (zipEnt.isDirectory()) {
                    strtemp = strPath + File.separator + gbkPath;
                    File dir = new File(strtemp);
                    dir.mkdirs();
                    continue;
                } else {
                    //读写文件
                    InputStream is = zipFile.getInputStream(zipEnt);
                    BufferedInputStream bis = new BufferedInputStream(is);
                    gbkPath = zipEnt.getName();
                    strtemp = strPath + File.separator + gbkPath;

                    //建目录
                    String strsubdir = gbkPath;
                    for (int i = 0; i < strsubdir.length(); i++) {
                        if (strsubdir.substring(i, i + 1).equalsIgnoreCase("/")) {
                            String temp = strPath + File.separator + strsubdir.substring(0, i);
                            File subdir = new File(temp);
                            if (!subdir.exists())
                                subdir.mkdir();
                        }
                    }
                    FileOutputStream fos = new FileOutputStream(strtemp);
                    BufferedOutputStream bos = new BufferedOutputStream(fos);
                    int c;
                    while ((c = bis.read()) != -1) {
                        bos.write((byte) c);
                    }
                    bos.close();
                    fos.close();
                }
            }
            zipFile.close();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                throw e;
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }

    //        public static void unpack(File zip,File outputDir,String charsetName){
//        FileOutputStream out = null;
//        InputStream in = null;
//        //读出文件数据
//        ZipFile zipFileData = null;
//        ZipFile zipFile = null;
//        try {
//            //若目标保存文件位置不存在
//            if (outputDir != null) if (!outputDir.exists()) {
//                outputDir.mkdirs();
//            }
//            if (charsetName != null && charsetName != "") {
//                zipFile = new ZipFile(zip.getPath(), Charset.forName(charsetName));
//            } else {
//                zipFile = new ZipFile(zip.getPath(), Charset.forName("utf8"));
//            }
//            //zipFile = new ZipFile(zip.getPath(), Charset.forName(charsetName));
//            Enumeration<? extends ZipEntry> entries = zipFile.entries();
//            //处理创建文件夹
//            while (entries.hasMoreElements()) {
//                ZipEntry entry = entries.nextElement();
//                String filePath = "";
//                if (outputDir == null) {
//                    filePath = zip.getParentFile().getPath() + File.separator + entry.getName();
//                } else {
//                    filePath = outputDir.getPath() + File.separator + entry.getName();
//                }
//                File file = new File(filePath);
//                File parentFile = file.getParentFile();
//                if (!parentFile.exists()) {
//                    parentFile.mkdirs();
//                }
//                if (parentFile.isDirectory()) {
//                    continue;
//                }
//            }
//            if (charsetName != null && charsetName != "") {
//                zipFileData = new ZipFile(zip.getPath(), Charset.forName(charsetName));
//            } else {
//                zipFileData = new ZipFile(zip.getPath(), Charset.forName("utf8"));
//            }
//            Enumeration<? extends ZipEntry> entriesData = zipFileData.entries();
//            while (entriesData.hasMoreElements()) {
//                ZipEntry entry = entriesData.nextElement();
//                in = zipFile.getInputStream(entry);
//                String filePath = "";
//                if (outputDir == null) {
//                    filePath = zip.getParentFile().getPath() + File.separator + entry.getName();
//                } else {
//                    filePath = outputDir.getPath() + File.separator + entry.getName();
//                }
//                File file = new File(filePath);
//                if (file.isDirectory()) {
//                    continue;
//                }
//                out = new FileOutputStream(filePath);
//                int len = -1;
//                byte[] bytes = new byte[1024];
//                while ((len = in.read(bytes)) != -1) {
//                    out.write(bytes, 0, len);
//                }
//                out.flush();
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            try {
//                out.close();
//                in.close();
//                zipFile.close();
//                zipFileData.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }
    public static Map<String, Integer> getFile(String filepath) throws FileNotFoundException, IOException {
        Map<String, Integer> filemsg = new HashMap<>();
        File picture = new File(filepath);
        BufferedImage sourceImg = ImageIO.read(new FileInputStream(picture));
        if (sourceImg == null) return null;
        filemsg.put("width", sourceImg.getWidth());
        filemsg.put("height", sourceImg.getHeight());
        return filemsg;
    }

    public static Boolean zoomImage(InputStream in, String dest, int w, int h) throws Exception {
        double wr = 0, hr = 0;
        File destFile = new File(dest);
        BufferedImage bufImg = ImageIO.read(in);
        Image Itemp = bufImg.getScaledInstance(w, h, bufImg.SCALE_SMOOTH);
        wr = w * 1.0 / bufImg.getWidth();
        hr = h * 1.0 / bufImg.getHeight();
        AffineTransformOp ato = new AffineTransformOp(AffineTransform.getScaleInstance(wr, hr), null);
        Itemp = ato.filter(bufImg, null);
        try {
            ImageIO.write((BufferedImage) Itemp, dest.substring(dest.lastIndexOf(".") + 1), destFile);
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public static Boolean zip(List<String> paths, String fileName) {
        org.apache.tools.zip.ZipOutputStream zos = null;
        try {
            zos = new org.apache.tools.zip.ZipOutputStream(new FileOutputStream(fileName));
            for (String filePath : paths) {
                // 递归压缩文件
                File file = new File(filePath);
                String relativePath = file.getName();
                if (file.isDirectory()) {
                    relativePath += File.separator;
                }
                zipFile(file, relativePath, zos);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (zos != null) {
                    zos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void zipFile(File file, String relativePath, ZipOutputStream zos) {
        InputStream is = null;
        try {
            if (!file.isDirectory()) {
                org.apache.tools.zip.ZipEntry zp = new org.apache.tools.zip.ZipEntry(relativePath);
                zp.setUnixMode(644);
                zos.putNextEntry(zp);
                is = new FileInputStream(file);
                byte[] buffer = new byte[BUFFERSIZE];
                int length = 0;
                while ((length = is.read(buffer)) >= 0) {
                    zos.write(buffer, 0, length);
                }
                zos.flush();
                zos.closeEntry();
            } else {
                org.apache.tools.zip.ZipEntry zp = new org.apache.tools.zip.ZipEntry(relativePath);
                zp.setUnixMode(755);
                zos.putNextEntry(zp);
                zos.closeEntry();
                String tempPath = null;
                for (File f : file.listFiles()) {
                    tempPath = relativePath + f.getName();
                    if (f.isDirectory()) {
                        tempPath += File.separator;
                    }
                    zipFile(f, tempPath, zos);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void toZip(String srcDir, OutputStream out, boolean KeepDirStructure) throws RuntimeException {
        long start = System.currentTimeMillis();
        ZipOutputStream zos = null;
        try {
            zos = new ZipOutputStream(out);
            File sourceFile = new File(srcDir);
            compress(sourceFile, zos, sourceFile.getName(), KeepDirStructure);
            long end = System.currentTimeMillis();
            System.out.println("压缩完成，耗时：" + (end - start) + " ms");
        } catch (Exception e) {
            throw new RuntimeException("zip error from ZipUtils", e);
        } finally {
            if (zos != null) {
                try {
                    zos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static final int BUFFER_SIZE = 2 * 1024;

    private static void compress(File sourceFile, ZipOutputStream zos, String name, boolean KeepDirStructure) throws Exception {
        byte[] buf = new byte[BUFFER_SIZE];
        if (sourceFile.isFile()) {
            // 向zip输出流中添加一个zip实体，构造器中name为zip实体的文件的名字
            zos.putNextEntry(new ZipEntry(name));
            // copy文件到zip输出流中
            int len;
            FileInputStream in = new FileInputStream(sourceFile);
            while ((len = in.read(buf)) != -1) {
                zos.write(buf, 0, len);
            }
            // Complete the entry
            zos.closeEntry();
            in.close();
        } else {
            File[] listFiles = sourceFile.listFiles();
            if (listFiles == null || listFiles.length == 0) {
                // 需要保留原来的文件结构时,需要对空文件夹进行处理
                if (KeepDirStructure) {
                    // 空文件夹的处理
                    zos.putNextEntry(new ZipEntry(name + "/"));
                    // 没有文件，不需要文件的copy
                    zos.closeEntry();
                }
            } else {
                for (File file : listFiles) {
                    // 判断是否需要保留原来的文件结构
                    if (KeepDirStructure) {
                        // 注意：file.getName()前面需要带上父文件夹的名字加一斜杠,
                        // 不然最后压缩包中就不能保留原来的文件结构,即：所有文件都跑到压缩包根目录下了
                        compress(file, zos, name + "/" + file.getName(), KeepDirStructure);
                    } else {
                        compress(file, zos, file.getName(), KeepDirStructure);
                    }
                }
            }
        }
    }

    /**
     * 读取zip文件,获取文件列表
     * @param zipFilepath
     * @return
     * @throws IOException
     */
    public static List<String> getFileNames(String zipFilepath) throws IOException {
        //直接读取zip文件,获取文件列表
        // 打开ZIP文件
        // 打开ZIP文件
        java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(zipFilepath);
        // 获取ZIP文件条目的枚举
        Enumeration<? extends java.util.zip.ZipEntry> entries = zipFile.entries();
        List<String> filenames = new ArrayList<>(zipFile.size());
        // 遍历条目
        while (entries.hasMoreElements()) {
            java.util.zip.ZipEntry entry = entries.nextElement();
            // 获取条目名称
            String entryName = entry.getName();
            System.out.println(entryName);
            filenames.add(entryName);
        }
        // 最后，确保关闭ZIP文件以释放资源
        zipFile.close();
        return filenames;
    }
}
