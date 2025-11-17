package com.nyy.gmail.cloud.common;

public enum MenuType {
//    cloudMenu("用户中心"),

//    userList("个人中心", "/cloud/"),
//    machine("设备管理", "/cloud/user/machine"),
//    socks5("IP管理", "/cloud/account/socks5/list"),
//    apiKey("API管理", "/cloud/user/apiKey/list"),

//    accountMenu("账号管理"),
    accountGroup("账号分组", "/cloud/account/accountGroup"),
    account("账号管理", "/cloud/account/account"),

//    batchSendMessage("群发消息"),
//    batchSendMessageTask("群发消息", "/admin/account/sendMessageTask"),

//    balanceMenu("财务管理"),
//    balanceDetail("我的账单", "/cloud/user/balanceDetail"),
//    payment("在线充值", "/cloud/user/payment"),
    resource("资源管理"),
    platform("平台管理", "/cloud/account/platform/list"),
    email("邮箱资源管理", "/cloud/account/email/list"),
    apiResource("API资源管理", "/cloud/account/apiResource/list"),
    receive("接码记录", "/cloud/account/receive/list"),
    task("任务管理", "/cloud/account/task/list"),
    googleStudio("Google AI管理", "/cloud/account/googleStudio/list"),
    chatgpt("Chatgpt AI管理", "/cloud/account/chatgpt/list"),

    billManage("账单管理", "/cloud/account/billManage/list"),
    orderManage("订单管理", "/cloud/account/order/list"),
    orderManageV2("订单管理V2", "/cloud/account/orderV2/list"),
    emailOrder("邮箱订单", "/cloud/account/emailOrder/list"),
//    emailCheckActive("邮箱筛活", "/cloud/account/emailCheckActive/list"),
    sieveActive("筛开通管理", "/cloud/account/sieveActive/list"),
    AITokenStatistics("AI token统计", "/cloud/account/aiTokenStatistics/list"),

    settings("系统设置"),
    socks5("IP管理", "/cloud/account/socks5/list"),
    proxyAccount("代理账号管理", "/cloud/user/proxyAccount/list"),

    adminRole("角色列表", "/admin/account/role/list"),
    aiModelManage("模型管理", "/admin/account/aiModel/list"),
    adminUser("用户列表", "/admin/account/user/list"),
    apiKey("API管理", "/cloud/user/apiKey/list"),
    aiServer("AI服务器管理", "/cloud/account/aiServer/list"),
//    adminVps("设备管理", "/admin/ac/account/vps/list"),
    globalParams("全局参数设置", "/admin/ac/account/params/globalParams"),

    dnsRecord("链接智能优化","/admin/common/dnsRecord/list"),
//    buttonTest("测试按钮权限按钮", "/admin/ac/account/params/globalParams:buttonTest", "button"),

    userCenter("个人中心"),
    userInfo("个人信息", "/cloud/user/center"),
    myOrder("我的订单", "/cloud/user/order"),
    myOrderV2("我的订单V2", "/cloud/user/orderV2"),
    myEmail("我的接码", "/cloud/user/email"),
    myBill("我的账单", "/cloud/user/bill"),
    recharge("在线充值", "/cloud/user/recharge"),
    ownerUser("我的下线", "/cloud/user/ownerUser"),
    batchSendEmail("邮件群发", "/cloud/user/batchSendEmail"),
    linkCheck("链接检测", "/cloud/user/linkCheck"),
    ContentAIGen("智能文案优化", "/cloud/user/contentAIGen"),
    tgNetToSession("TGNET转换", "/cloud/user/tgNetToSession"),
    systemMailTemplate("系统模板库", "/cloud/user/systemMailTemplate"),
    myMailTemplate("个人模板库", "/cloud/user/myMailTemplate"),
    ;
    private final String desc;
    private final String path;
    private final String type; // 'menu', 'button'

    private MenuType() {
        this.desc = "";
        this.path = "";
        this.type = "menu";
    }

    private MenuType(String desc) {
        this.desc = desc;
        this.path = "";
        this.type = "menu";
    }

    private MenuType(String desc, String path) {
        this.desc = desc;
        this.path = path;
        this.type = "menu";
    }

    private MenuType(String desc, String path, String type) {
        this.desc = desc;
        this.path = path;
        this.type = type;
    }

    public String getDesc() {
        return desc;    }

    public String getPath() {
        return path;
    }

    public String getType () {
        return type;
    }
}
