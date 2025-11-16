package com.nyy.gmail.cloud.tasks.impl;

import com.nyy.gmail.cloud.common.http.OkHttpClientFactory;
import com.nyy.gmail.cloud.entity.mongo.*;
import com.nyy.gmail.cloud.enums.*;
import com.nyy.gmail.cloud.framework.mq.SubTaskMQProducer;
import com.nyy.gmail.cloud.repository.mongo.AccountRepository;
import com.nyy.gmail.cloud.repository.mongo.GroupTaskRepository;
import com.nyy.gmail.cloud.repository.mongo.SubTaskRepository;
import com.nyy.gmail.cloud.service.Socks5Service;
import com.nyy.gmail.cloud.tasks.AbstractTask;
import com.nyy.gmail.cloud.tasks.BaseTask;
import com.nyy.gmail.cloud.utils.FileUtils;
import com.nyy.gmail.cloud.utils.TaskUtil;
import com.nyy.gmail.cloud.utils.UUIDUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.socksx.v5.*;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.net.Socket;

@Slf4j
@Component("EmailCheckActive")
public class EmailCheckActiveTaskImpl extends AbstractTask implements BaseTask {

    @Autowired
    private SubTaskMQProducer subTaskMQProducer;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private GroupTaskRepository groupTaskRepository;

    @Autowired
    private SubTaskRepository subTaskRepository;

    @Autowired
    private TaskUtil taskUtil;

    @Autowired
    private Socks5Service socks5Service;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+$"
    );

    @Override
    protected SubTaskRepository getSubTaskRepository() {
        return subTaskRepository;
    }

    @Override
    protected TaskUtil getTaskUtil() {
        return taskUtil;
    }

    @Override
    public boolean publishTask(GroupTask groupTask) {
        String userID = groupTask.getUserID();
        Map params = groupTask.getParams();
        groupTask.setUpdateTime(new Date());

        switch (GroupTaskStatusEnums.fromCode(groupTask.getStatus())) {
            case GroupTaskStatusEnums.waitPublish -> {
                groupTask.setStatus(GroupTaskStatusEnums.processing.getCode());
            }
            case GroupTaskStatusEnums.init, GroupTaskStatusEnums.processing -> {
                // 保存子任务
                int maxInsertCount = 5000;
                long taskCount = subTaskRepository.countByGroupTaskIdEquals(groupTask.get_id());
                List<String> addDatas = taskUtil.getAddData(groupTask.getParams(), maxInsertCount);
                List<SubTask> processingTasks = new ArrayList<>();
                if (!CollectionUtils.isEmpty(addDatas)) {
                    for (int i = 0; i < addDatas.size(); i++) {
                        if (!StringUtil.isEmpty(addDatas.get(i))) {
                            String email = addDatas.get(i).split("----")[0].toLowerCase().trim();
                            if (processingTasks.stream().anyMatch(e -> e.getParams().getOrDefault("email", "").equals(email))) {
                                continue;
                            }
                            SubTask subTask = taskUtil.newTaskModel(groupTask, Map.of(), Map.of("email", email), new Date());
                            processingTasks.add(subTask);
                            taskCount++;
                        }
                    }

                    subTaskRepository.batchInsert(processingTasks);
                    groupTaskRepository.save(groupTask);
                    return true;
                } else if (groupTask.getStatus().equals(GroupTaskStatusEnums.processing.getCode())) {
                    groupTask.setTotal(taskCount);
                    groupTask.setPublishTotalCount(taskCount);
                    groupTask.setPublishedCount(taskCount);
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
//        int onceMaxCount = 2000;
//
//        List<SubTask> subTaskList = null;
//        List<String> ids = groupTask.getIds();
//        Map<String, Account> accountMap = new HashMap<>();
//        List<TaskMessage> mqMsg = new ArrayList<>();
//        int count = 0;
//
//        subTaskList = subTaskRepository.findByUserIDEqualsAndGroupTaskIdEqualsAndStatusIn(userID, groupTask.get_id(), List.of(SubTaskStatusEnums.processing.getCode()), 1, onceMaxCount);
//
//        for (SubTask subTask : subTaskList) {
//            // 随机
//            String accid = ids.get(count % ids.size());
//
//            Account account = accountMap.get(accid);
//            if (account == null) {
//                account = accountRepository.findById(accid);
//            }
//            if (account == null) {
//                continue;
//            } else {
//                accountMap.put(accid, account);
//            }
//            taskUtil.distributeAccount(account, subTask);
//            mqMsg.add(taskUtil.domain2mqTask(subTask));
//
//            count++;
//        }
//
//        if (!mqMsg.isEmpty()) {
//            subTaskMQProducer.sendMessage(mqMsg);
//        }

        try {
            List<SubTask> subTaskList = new ArrayList<>();
            if (subTaskList != null) {
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
                        // 统计数据
                        groupTask = statisticsData(groupTask, true);
                        return true;
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
                                subTask.getParams().put("apiKeyId", "");
                                subTaskRepository.save(subTask);
                            } catch (Exception e) {
                            }
                        }
                    }
                    groupTaskRepository.save(groupTask);
                }
            }

            groupTask = statisticsData(groupTask, false);
        } catch (OptimisticLockingFailureException e) {
            log.info("{} OptimisticLockingFailureException: {}", groupTask.get_id(), e.getMessage());
        }
        return true;
    }

    private GroupTask statisticsData(GroupTask groupTask, boolean saveFile) {
        List<SubTask> normal = subTaskRepository.findNormal(groupTask.get_id());
        // 异常
        List<SubTask> except = subTaskRepository.findExcept(groupTask.get_id());

        // 未知
        List<SubTask> unknown = subTaskRepository.findUnknown(groupTask.get_id());

        // 不存在
        List<SubTask> noExists = subTaskRepository.findNoExists(groupTask.get_id());

        String normalFile = saveFile ? saveFile(normal.stream().map(e -> e.getParams().getOrDefault("email", "").toString()), groupTask.getDesc(), "normal") : "";
        String exceptFile = saveFile ? saveFile(except.stream().map(e -> e.getParams().getOrDefault("email", "").toString()), groupTask.getDesc(),"except") : "";
        String unknownFile = saveFile ? saveFile(unknown.stream().map(e -> e.getParams().getOrDefault("email", "").toString()), groupTask.getDesc(),"unknown") : "";
        String noExistsFile = saveFile ? saveFile(noExists.stream().map(e -> e.getParams().getOrDefault("email", "").toString()), groupTask.getDesc(),"noExists") : "";

        for (int i = 0; i < 10; i++) {
            try {
                if (groupTask != null) {
                    groupTask.getParams().put("normalCount", normal.size());
                    groupTask.getParams().put("exceptCount", except.size());
                    groupTask.getParams().put("unknownCount", unknown.size());
                    groupTask.getParams().put("noExistsCount", noExists.size());

                    if (saveFile) {
                        groupTask.getParams().put("normalFile", normalFile);
                        groupTask.getParams().put("exceptFile", exceptFile);
                        groupTask.getParams().put("unknownFile", unknownFile);
                        groupTask.getParams().put("noExistsFile", noExistsFile);
                    }

                    groupTaskRepository.save(groupTask);
                }
                break;
            } catch (Exception e) {
                try {
                    Thread.sleep(1000);
                    groupTask = groupTaskRepository.findById(groupTask.get_id()).orElse(null);
                } catch (InterruptedException ex) {
                }
            }
        }

        return groupTask;
    }

    private String saveFile(Stream<String> email, String desc, String normal) {
        try {
            List<String> list = email.toList();
            // 导出文件保存
            Path resPath = FileUtils.resPath;
            Path filepath = Path.of("emailCheckActive", UUIDUtils.get32UUId(), desc + "_" + normal + "_" + list.size() + ".txt");
            Path path = resPath.resolve(filepath).toAbsolutePath().normalize();

            try {
                Files.createDirectories(resPath.resolve("emailCheckActive").normalize());
            } catch (IOException e) {
                log.error("创建upload文件夹失败", e);
            }

            try {
                Files.createDirectories(path.getParent());
            } catch (IOException e) {
                log.error("创建upload文件夹失败", e);
            }
            if (Files.exists(path)) {
                Files.delete(path);
            }
            Files.writeString(path, String.join("\n", list), StandardOpenOption.CREATE_NEW);
            return filepath.toString();
        } catch (Exception e) {
            log.info("保存文件失败", e.getMessage());
        }
        return  "";
    }

    @Override
    public boolean checkTask(SubTask task, Account account, Date now) {
        return true;
    }

    public static String[] lookupMX(String domain, int timeoutMillis) throws NamingException, InterruptedException {
        NamingException lastEx = null;

        // 尝试 3 次，间隔 500ms
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                Hashtable<String, String> env = new Hashtable<>();
                env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
                env.put("com.sun.jndi.dns.timeout.initial", String.valueOf(timeoutMillis)); // 初始超时
                env.put("com.sun.jndi.dns.timeout.retries", "1"); // DNS 内部重试次数

                DirContext ctx = new InitialDirContext(env);
                Attributes attrs = ctx.getAttributes(domain, new String[]{"MX"});
                Attribute attr = attrs.get("MX");

                // 没有 MX 记录，则尝试 A 记录
                if (attr == null) {
                    attrs = ctx.getAttributes(domain, new String[]{"A"});
                    attr = attrs.get("A");
                    if (attr == null) {
                        return new String[0];
                    }
                }

                List<String> servers = new ArrayList<>();
                for (int i = 0; i < attr.size(); i++) {
                    String entry = (String) attr.get(i);
                    // 去掉末尾的点号和多余空格
                    entry = entry.trim();
                    if (entry.contains(" ")) {
                        String[] parts = entry.split("\\s+");
                        servers.add(parts[1].endsWith(".") ? parts[1].substring(0, parts[1].length() - 1) : parts[1]);
                    } else {
                        servers.add(entry.endsWith(".") ? entry.substring(0, entry.length() - 1) : entry);
                    }
                }

                return servers.toArray(new String[0]);
            } catch (NamingException e) {
                lastEx = e;
                Thread.sleep(500); // 等待 500ms 再重试
            }
        }
        return new String[]{};
//        throw lastEx;
    }

    public static boolean checkEmail(String proxyHost, int proxyPort,
                                     String username, String password,
                                     String smtpHost, int smtpPort,
                                     String sender, String receiver) throws Exception {

        EventLoopGroup group = new NioEventLoopGroup();
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline p = ch.pipeline();

                            // SOCKS5 编码/解码
                            if (!StringUtil.isEmpty(proxyHost)) {
                                // SOCKS5 编解码
                                p.addLast(Socks5ClientEncoder.DEFAULT);
                                p.addLast(new Socks5InitialResponseDecoder());
                                p.addLast(new Socks5PasswordAuthResponseDecoder());
                            }

                            // SMTP 明文解码
                            p.addLast(new LineBasedFrameDecoder(1024));
                            p.addLast(new StringDecoder(CharsetUtil.UTF_8));
                            p.addLast(new StringEncoder(CharsetUtil.UTF_8));

                            // SMTP 处理器
                            p.addLast(new SimpleChannelInboundHandler<Object>() {
                                private int step = 0;
                                private boolean useProxy = !StringUtil.isEmpty(proxyHost);

                                @Override
                                public void channelActive(ChannelHandlerContext ctx) {
                                    if (useProxy) {
                                        ctx.writeAndFlush(new DefaultSocks5InitialRequest(Socks5AuthMethod.PASSWORD));
                                    } else {
                                        // 直连的话，什么都不发，等待 SMTP 220 banner
                                    }
                                }

                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
                                    if (future.isDone()) return;
                                    if (useProxy) {
                                        // -------- SOCKS5 流程 --------
                                        if (msg instanceof Socks5InitialResponse) {
                                            ctx.writeAndFlush(new DefaultSocks5PasswordAuthRequest(username, password));
                                            return;
                                        } else if (msg instanceof Socks5PasswordAuthResponse) {
                                            Socks5PasswordAuthResponse authResp = (Socks5PasswordAuthResponse) msg;
                                            if (authResp.status() == Socks5PasswordAuthStatus.SUCCESS) {
                                                ctx.writeAndFlush(new DefaultSocks5CommandRequest(
                                                        Socks5CommandType.CONNECT,
                                                        Socks5AddressType.DOMAIN,
                                                        smtpHost,
                                                        smtpPort
                                                ));
                                            } else {
                                                throw new RuntimeException("SOCKS5 验证失败");
                                            }
                                            return;
                                        } else if (msg instanceof Socks5CommandResponse) {
                                            Socks5CommandResponse cmdResp = (Socks5CommandResponse) msg;
                                            if (cmdResp.status() != Socks5CommandStatus.SUCCESS) {
                                                throw new RuntimeException("SOCKS5 CONNECT 失败");
                                            }
                                            // 一旦 CONNECT 成功，后面就是 SMTP banner
                                            return;
                                        }
                                    }

                                    if (msg instanceof String) {
                                        log.info(step + " " + smtpHost + " msg: " + msg.toString());
                                        String line = ((String) msg).trim();
                                        if (line.contains("\0")) {
                                            String[] parts = line.split("\0");
                                            line = parts.length > 1 ? parts[1] : parts[0];
                                        }

                                        switch (step) {
                                            case 0:
                                                future.complete(true);
                                                if (line.startsWith("220")) {
                                                    future.complete(true);
                                                    ctx.writeAndFlush("HELO gmail.com\r\n");
                                                    step++;
                                                }
                                                break;
                                            case 1:
                                                if (line.startsWith("250")) {
                                                    ctx.writeAndFlush("MAIL FROM:<" + sender + ">\r\n");
                                                    step++;
                                                }
                                                break;
                                            case 2:
                                                if (line.startsWith("250")) {
                                                    ctx.writeAndFlush("RCPT TO:<" + receiver + ">\r\n");
                                                    step++;
                                                }
                                                break;
                                            case 3:
                                                if (line.startsWith("250") || line.startsWith("251")) {
                                                    future.complete(true); // 邮箱存在
                                                } else {
                                                    future.complete(false); // 邮箱不存在
                                                }
                                                ctx.writeAndFlush("QUIT\r\n");
                                                step++;
                                                break;
                                            default:
                                                ctx.close();
                                        }
                                    }
                                }

                                @Override
                                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                    // 代理不可用 / 连接失败 / 超时 -> 抛异常
                                    future.completeExceptionally(cause);
                                    ctx.close();
                                }

                                @Override
                                public void channelInactive(ChannelHandlerContext ctx) {
                                    if (!future.isDone()) {
                                        future.completeExceptionally(new RuntimeException("SMTP 连接关闭"));
                                    }
                                }
                            });
                        }
                    });

            ChannelFuture f = null;
            if (StringUtil.isEmpty(proxyHost)) {
                f = bootstrap.connect(smtpHost, smtpPort).sync();
            } else {
                f = bootstrap.connect(proxyHost, proxyPort).sync();
            }
            // 超时等待 SMTP 流程完成
            boolean exists = future.get(30, TimeUnit.SECONDS);
            f.channel().closeFuture().sync();
            return exists;
        } finally {
            group.shutdownGracefully();
        }
    }

    private void retrySubTask(SubTask task) {
        for (int i = 0; i < 10; i++) {
            try {
                SubTask wt = subTaskRepository.findById(task.get_id());
                wt.setStatus(SubTaskStatusEnums.processing.getCode());
                wt.setAccid("");
                wt.getParams().put("retry", Integer.parseInt(wt.getParams().getOrDefault("retry", "0").toString()) + 1);
                subTaskRepository.save(wt);
                break;
            } catch (Exception e) {
            }
        }
    }

    public String checkEmail (String email) throws Throwable {
        try {
            if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
                return "邮箱格式不合法，不存在";
            }

            // MX 校验
            String[] mxServer = lookupMX(email.split("@")[1], 3000);
            if (mxServer.length == 0) {
                return "邮箱MX解析失败，不存在";
            }

            for (int i = 0; i < Math.max(mxServer.length, 3); i++) {
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(mxServer[i % mxServer.length], 25), 5000); // 5秒超时
                    break;
                } catch (Exception e) {
                    if (i >= Math.max(mxServer.length, 3) - 1) {
                        return "SMTP验证失败，不存在";
                    }
                }
            }


            return "邮箱存在";
        } catch (Throwable e) {
            if (e.getMessage().contains("DNS name not found [response code 3]") || e.getMessage().contains("DNS error") || e.getMessage().contains("DNS server failure [response code 2]")) {
                return "邮箱MX解析失败，不存在";
            } else if (e.getMessage().contains("SMTP 连接关闭")) {
                return "SMTP 连接关闭，不存在";
            } else if (e instanceof ExecutionException && e.getCause() instanceof SocketException) {
            }
            throw e;
        }
    }

    @Override
    public boolean runTask(SubTask task, Account account) {
        return false;
//        SubTask wt = null;
//        try {
//            wt = subTaskRepository.findById(task.get_id());
//            if (wt == null) {
//                log.info("id {} 任务不存在", task.get_id());
//                return false;
//            }
//            if (!wt.getStatus().equals(SubTaskStatusEnums.init.getCode())) {
//                log.info("id {} 任务状态不正确", task.get_id());
//                return false;
//            }
//            GroupTask groupTask = groupTaskRepository.findById(task.getGroupTaskId()).orElse(null);
//            if (groupTask == null) {
//                this.reportTaskStatus(wt, SubTaskStatusEnums.failed, "任务已经结束");
//                return false;
//            }
//            if (groupTask.getStatus().equals(GroupTaskStatusEnums.success.getCode())) {
//                this.reportTaskStatus(wt, SubTaskStatusEnums.failed, "任务已经结束");
//                return false;
//            }
//            if (account == null || !account.getOnlineStatus().equals(AccountOnlineStatus.ONLINE.getCode())) {
////                this.reportTaskStatus(wt, SubTaskStatusEnums.failed, "账号不可用");
//                retrySubTask(wt);
//                return false;
//            }
//
//            if (wt.getParams().getOrDefault("retry", "0").toString().equals("5")) {
//                this.reportTaskStatus(wt, SubTaskStatusEnums.failed, "重试次数大于5次");
//                return false;
//            }
//
//            String email = task.getParams().getOrDefault("email", "").toString();
//            if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
//                this.reportTaskStatus(wt, SubTaskStatusEnums.success, "邮箱格式不合法，不存在");
//                return false;
//            }
//
//            // MX 校验
//            String[] mxServer = lookupMX(email.split("@")[1]);
//            if (mxServer.length == 0) {
//                this.reportTaskStatus(wt, SubTaskStatusEnums.success, "邮箱MX解析失败，不存在");
//                return false;
//            }
//
//            // SMTP 验证
//            Socks5 socks5 = socks5Service.getAccountSocks(account);
//            if (socks5 == null) {
//                throw new CommonException(ResultCode.NO_CAN_USE_SOCKS);
//            }
//            boolean verify = checkEmail(socks5.getIp(), socks5.getPort(), socks5.getUsername(), socks5.getPassword(), mxServer[0], 25, "check@test.com", email);
//            if (!verify) {
//                this.reportTaskStatus(wt, SubTaskStatusEnums.success, Map.of("msg", "SMTP验证失败，不存在", "mxServer", mxServer));
//                return false;
//            }
//
//            // TODO gmail 邮箱识别
//
//            this.reportTaskStatus(wt, SubTaskStatusEnums.success, Map.of("msg", "邮箱存在", "mxServer", mxServer));
//            return false;
//        } catch (Throwable e) {
//            if (e.getMessage().contains("DNS name not found [response code 3]") || e.getMessage().contains("DNS error") || e.getMessage().contains("DNS server failure [response code 2]")) {
//                this.reportTaskStatus(wt, SubTaskStatusEnums.success, "邮箱MX解析失败，不存在");
//            } else if (e.getMessage().contains("SMTP 连接关闭")) {
//                retrySubTask(wt);
//            } else if (e instanceof ExecutionException && e.getCause() instanceof SocketException) {
//                retrySubTask(wt);
//            } else {
//                if (wt != null) {
//                    this.reportTaskStatus(wt, SubTaskStatusEnums.failed, "出现异常：" + e.getMessage());
//                }
//            }
//        } finally {
//            if (account != null) {
//                socks5Service.releaseSocks5(account);
//            }
//        }
//        return false;
    }
}
