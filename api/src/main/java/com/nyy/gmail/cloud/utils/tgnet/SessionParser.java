package com.nyy.gmail.cloud.utils.tgnet;

import com.nyy.gmail.cloud.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;


/**
 * 解析Telegram会话数据
 */
@Slf4j
public class SessionParser {

    static {
        try {
            Files.createDirectories(FileUtils.resPath.resolve("tgSession").normalize());
        } catch (IOException e) {
            log.error("创建upload文件夹失败", e);
        }
    }

    /**
     * 确定用户id，根据id构建文件路径，解析tgnetB64中的数据，
     * @param tgnetB64
     * @param userCfgB64
     * @param sessionDir
     * @param sessionName
     * @param returnSession
     * @return
     */
    // TODO 不清楚session的保存方式
    public static CompletableFuture<String> parse(String tgnetB64, String userCfgB64,
                                                  String sessionDir, String sessionName,
                                                  boolean returnSession) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String uid = null;
                if (sessionName != null && sessionName.matches("\\d+")) {
                    uid = sessionName;
                }

                if (uid == null && userCfgB64 != null) {
                    User user = extractUserCfg(userCfgB64);
                    if (user != null) {
                        uid = user.phone != null ? user.phone : String.valueOf(user.id);
                    }
                }

                uid = uid != null ? uid : String.valueOf(System.currentTimeMillis() * 10L);

                // 根据id构建文件路径
                String sessionFilePath = Paths.get(sessionDir, uid + ".session").toString();

                byte[] tgnetBytes = TelegramSessionParser.base64Decode(tgnetB64);

                // 根据字节内容写入文件中
                extractTgnet(tgnetBytes, sessionFilePath);

                // 以下内容暂且不知作用，传入的值始终为true
                if (!returnSession) {
                    return uid;
                }

                byte[] sessionContent = Files.readAllBytes(Paths.get(sessionFilePath));
                String sessionB64 = Base64.getEncoder().encodeToString(sessionContent);
                return uid + "----" + sessionB64;

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static String parse(String tgnetB64, String userCfgB64) {
        String sessionDir = FileUtils.resPath.toString();

        try {
            String uid = null;
            String phone = "";

            if (userCfgB64 != null) {
                User user = extractUserCfg(userCfgB64);
                if (user != null) {
                    uid = String.valueOf(user.id);
                    phone = StringUtils.isEmpty(user.phone) ? "" : user.phone;
                }
            }

            uid = uid != null ? uid : String.valueOf(System.currentTimeMillis() * 10L);

            // 根据id构建文件路径
            Path sessionFilePath = Paths.get(sessionDir, "tgSession", uid + ".session");

            byte[] tgnetBytes = TelegramSessionParser.base64Decode(tgnetB64);

            // 根据字节内容写入文件中
            extractTgnet(tgnetBytes, sessionFilePath.toString());

            byte[] sessionContent = Files.readAllBytes(sessionFilePath);
            String sessionB64 = Base64.getEncoder().encodeToString(sessionContent);

            FileUtils.deleteFile(sessionFilePath);
            return uid + "----" + sessionB64 + "----" + phone + "----------------";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 处理目录中的会话数据
     */
    public static CompletableFuture<Void> processDirectory(String rootDir, String outDir) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (!Files.exists(Paths.get(rootDir))) {
                    System.out.println("错误：目录 " + rootDir + " 不存在");
                    return;
                }

                List<String> sessions = new ArrayList<>();
                File rootDirectory = new File(rootDir);
                // 读取rootDir下的文件夹里的所有内容
                for (File dir : rootDirectory.listFiles(File::isDirectory)) {
                    Map<String, String> data = new HashMap<>();

                    // 读取目录中的文件
                    for (File file : dir.listFiles(File::isFile)) {
                        String base64Content = TelegramSessionParser.fileToBase64(file.getAbsolutePath());
                        if (base64Content != null) {
                            data.put(file.getName(), base64Content);
                        }
                    }

                    try {
                        String tgnet = data.get("tgnet.dat");
                        String user = data.get("userconfing.xml");

                        if (tgnet != null && user != null) {
                            String result = SessionParser.parse(tgnet, user, outDir, dir.getName(), true)
                                    .get();

                            sessions.add(result);
                        }
                    } catch (Exception e) {
                        System.err.println("处理目录 " + dir.getName() + " 时出错: " + e.getMessage());
                    }
                }

                // 保存结果
                String outputPath = "C:\\Users\\Administrator\\Desktop\\test\\tg-session.txt";
                try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputPath))) {
                    for (String line : sessions) {
                        writer.write(line);
                        log.info("{}",line);
                        writer.newLine();
                    }
                    System.out.println("\n结果已保存 ");
                }

            } catch (Exception e) {
                System.err.println("处理目录时出错: " + e.getMessage());
            }
        });
    }


    private static String getConfigData(String string, String name) {
        // 使用正则表达式匹配 '"name">([^<]*)<'
        String regex = "\"" + name + "\">([^<]*)<";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
        java.util.regex.Matcher matcher = pattern.matcher(string);

        // 如果找到匹配项，返回第一个捕获组的内容
        if (matcher.find()) {
            return matcher.group(1);
        }

        // 如果没有匹配项，返回 null
        return null;
    }


    // 获取用户信息
    private static User extractUserCfg(String userConfigB64) {
        // 实现用户配置提取逻辑
        try {
            // 解码Base64字符串
            byte[] configBytes = Base64.getDecoder().decode(userConfigB64);
            String config = new String(configBytes, "UTF-8");

            // 提取用户配置数据
            String user = getConfigData(config, "user");
            if (user != null) {
                user = user.replace("&#10;", "").replace(" ", "");
                byte[] userBytes = Base64.getDecoder().decode(user);

                // 将数据转为2进制
                DataInputStream reader = new DataInputStream(new ByteArrayInputStream(userBytes));

                reader.skipBytes(4); // 跳过前4个字节

                // User类
                return User.fromReader(reader);
            }
        } catch (Exception e) {
            // 忽略异常，返回null
        }
        return null;
    }

    // 获取会话信息
    // TODO 这里的save方法不了解有什么作用
    private static Session extractTgnet(byte[] tgnetBytes, String filePath) throws Exception {
        // 实现网络配置提取逻辑
        ByteArrayInputStream stream = new ByteArrayInputStream(tgnetBytes);
        int currentId = readHeaders(stream);
        DataCenter[] dcArray = readDataCenter(stream);
        for (DataCenter dc : dcArray) {

            if (dc.getDcId() == currentId) {
                return dc.save(filePath);
            }
        }

        throw new Exception("extract tgnet fail, data center not found");
    }

    //读取telegram 的请求头
    private static int readHeaders(InputStream stream) throws IOException {
        int currentDataCenterId = 0;

        // 读取固定字段
        byte[] dataSizeBytes = new byte[4];
        stream.read(dataSizeBytes);
        int dataSize = ByteBuffer.wrap(dataSizeBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();

        stream.skip(4); // configVersion
        stream.skip(4); // testBackend
        stream.skip(4); // clientBlocked

        // lastInitSystemLangcode (string)
        readString(stream);

        stream.skip(4); // currentDataCenter

        // currentDataCenterId
        byte[] dcIdBytes = new byte[4];
        stream.read(dcIdBytes);
        currentDataCenterId = ByteBuffer.wrap(dcIdBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();

        stream.skip(4); // timeDifference
        stream.skip(4); // lastDcUpdateTime
        stream.skip(8); // pushSessionId
        stream.skip(4); // regForInternalPush
        stream.skip(4); // lastServerTime

        // 读取session信息
        int sessionSize = readInt(stream);
        for (int i = 0; i < sessionSize; i++) {
            stream.skip(8); // session
        }

        return currentDataCenterId;
    }

    // 解析 Telegram 网络配置数据中的数据中心信息
    private static DataCenter[] readDataCenter(InputStream stream) throws IOException {
        int dcCount = readInt(stream);
        DataCenter[] dataCenters = new DataCenter[dcCount];

        for (int i = 0; i < dcCount; i++) {
            // 跳过配置版本信息
            stream.skip(4);
            //读取数据中心 ID 和最后初始化版本
            int dcId = readInt(stream);
            int lastInitVersion = readInt(stream);
            // 跳过媒体版本信息
            stream.skip(4);

            DataCenter dataCenter = new DataCenter(dcId);
            dataCenter.setLastInitVersion(lastInitVersion);

            // 读取IP地址信息
            for (IPType ipType : IPType.values()) {
                List<Address> addrs = extractIp(stream);
                if (ipType == IPType.IPv4) {
                    dataCenter.setAddrArray(addrs);
                }
            }

            stream.skip(4); // isCdnDatacenter

            // 读取认证密钥
            int authKeyPermSize = readInt(stream);
            if (authKeyPermSize > 0) {
                byte[] authKeyPerm = new byte[authKeyPermSize];
                stream.read(authKeyPerm);
                dataCenter.setAuthKey(authKeyPerm);
            }

            stream.skip(8); // authKeyPermId

            int authKeyTempSize = readInt(stream);
            if (authKeyTempSize > 0) {
                byte[] authKeyTemp = new byte[authKeyTempSize];
                stream.read(authKeyTemp);
            }
            stream.skip(8); // authKeyTempId

            int authKeyMediaTempSize = readInt(stream);
            if (authKeyMediaTempSize > 0) {
                byte[] authKeyMediaTemp = new byte[authKeyMediaTempSize];
                stream.read(authKeyMediaTemp);
            }
            stream.skip(8); // authKeyMediaTempId

            int authorized = readInt(stream);
            dataCenter.setAuthorized(authorized);

            // 读取salts
            extractSalts(stream);
            extractSalts(stream);

            dataCenters[i] = dataCenter;
        }
        return dataCenters;
    }

    // 从输入流中读取一个 4 字节的整数
    private static int readInt(InputStream stream) throws IOException {
        byte[] bytes = new byte[4];
        stream.read(bytes);
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    // 从 Telegram 二进制数据流中读取字符串数据
    private static String readString(InputStream stream) throws IOException {
        int sl = 1;
        int length = stream.read();

        if (length == 254) {
            sl = 4;
            throw new IOException("String length 254 is not implemented");
        }

        int addition = (length + sl) % 4;
        if (addition != 0) {
            addition = 4 - addition;
        }

        byte[] bytes = new byte[length];
        stream.read(bytes);

        String string;
        try {
            string = new String(bytes, "UTF-8");
        } catch (Exception e) {
            string = new String(bytes);
        }

        // 跳过填充字节
        stream.skip(addition);

        return string;
    }

    // 从 Telegram 网络配置数据中提取 IP 地址信息
    private static List<Address> extractIp(InputStream stream) throws IOException {
        int ipCount = readInt(stream);  // 读取IP数量
        List<Address> addrArray = new ArrayList<>();

        for (int i = 0; i < ipCount; i++) {
            String addr = readString(stream);  // 读取地址
            int port = readInt(stream);        // 读取端口
            int flag = readInt(stream);        // 读取标志
            String secret = readString(stream); // 读取密钥

            Address address = new Address(addr, port, flag, secret.getBytes());
            addrArray.add(address);

            // 注意：这里省略了打印日志的部分，如需保留可添加相应的日志输出
        }

        return addrArray;
    }

    // 从 Telegram 网络数据流中读取并跳过服务器 salts 信息
    private static void extractSalts(InputStream stream) throws IOException {
        int saltCount = readInt(stream);  // 读取salt数量

        // 读取并跳过所有salt数据
        for (int i = 0; i < saltCount; i++) {
            stream.skip(16);  // 跳过每个salt的16字节数据
        }
    }
}
