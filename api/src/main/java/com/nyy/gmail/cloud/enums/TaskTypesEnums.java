package com.nyy.gmail.cloud.enums;

public enum TaskTypesEnums {
    // 测试任务
    TEST_TASK("TestTask", "测试任务"),
    // 其他任务...
    SendMessage("SendMessage", "发送消息", 0),
    AccountLogin("AccountLogin", "账号上线", 10),
    AccountLogout("AccountLogout", "账号下线", -1),

    ChatroomSendMessage("ChatroomSendMessage", "聊天室发消息", 0),

    AccountImport("AccountImport", "导入账号", 10),
    AccountExport("AccountExport", "导出账号", 0),
    ImageRecognition("ImageRecognition", "图片识别", 0),

    BatchSendEmail("BatchSendEmail", "群发邮件", 0),
    BatchSendEmailTest("BatchSendEmailTest", "群发邮件测试", 0),
    BatchSendEmailTestV2("BatchSendEmailTestV2", "群发邮件测试V2", 0),

    EmailCheckActive("EmailCheckActive", "邮箱筛活", 0),
    EmailLinkCheck("EmailLinkCheck", "邮箱链接检测", 0),
	SieveActive("SieveActive", "筛开通", 0),
    EmailContentAiGen("EmailContentAiGen", "邮件文案生成", 0),

    TgNetToSession("TgNetToSession", "TGNET转Session", 0),
	
	;

    private final String code;
    private final String description;
    private final int priority;

    TaskTypesEnums(String code, String description) {
        this.code = code;
        this.description = description;
        this.priority = 0;
    }

    TaskTypesEnums(String code, String description, int priority) {
        this.code = code;
        this.description = description;
        this.priority = priority;
    }

    /**
     * 获取任务类型的代码
     * @return 任务类型代码
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取任务类型的描述信息
     * @return 任务类型描述
     */
    public String getDescription() {
        return description;
    }

    public int getPriority() {
        return priority;
    }

    /**
     * 根据代码查找对应的任务类型枚举
     * @param code 任务类型代码
     * @return 对应的任务类型枚举，如果未找到则返回 null
     */
    public static TaskTypesEnums fromCode(String code) {
        for (TaskTypesEnums taskType : values()) {
            if (taskType.code.equals(code)) {
                return taskType;
            }
        }
        return null;
    }
}
