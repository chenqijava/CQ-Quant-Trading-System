package com.nyy.gmail.cloud.enums;

public enum AiModelEnums {
    gemini_2_5_flash_lite("gemini-2.5-flash-lite", "gemini-2.5-flash-lite"),
    gemini_2_5_flash("gemini-2.5-flash", "gemini-2.5-flash"),
    gemini_2_0_flash("gemini-2.0-flash", "gemini-2.0-flash"),
    gemini_2_5_pro("gemini-2.5-pro", "gemini-2.5-pro"),
    auto("auto", "auto"),
    gpt_5("gpt-5", "gpt-5"),
    gpt_4o("gpt-4o", "gpt-4o"),
    gpt_4o_mini("gpt-4o-mini", "gpt-4o-mini"),
    ;

    private final String code;
    private final String description;

    AiModelEnums(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 获取账号状态对应的代码
     * @return 状态代码
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取账号状态的描述信息
     * @return 状态描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 根据代码查找对应的账号状态枚举
     * @param code 状态代码
     * @return 对应的账号状态枚举，如果未找到则返回 null
     */
    public static AiModelEnums fromCode(String code) {
        for (AiModelEnums status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}
