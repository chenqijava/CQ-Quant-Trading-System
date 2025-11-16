package com.nyy.gmail.cloud.common.response;

import lombok.Getter;

/**
 * * 响应码枚举，对应HTTP状态码
 */
@Getter
public enum ResultCode {

    SUCCESS(1, "success"),                     //成功

    ERROR(0, "error"),                     //失败

    BAD_REQUEST(400, "Bad Request"),          //失败

    UNAUTHORIZED(401, "认证失败"),             //未认证

    LOGIN_ERROR(402, "登录失败"),             // 登录失败

    NOT_FOUND(404, "接口不存在"),              //接口不存在

    FILENAME_BAD_REQUEST(410, "文件名错误,不能包含 .. 等字符"), //文件名错误包含..

    TWO_FA_ERROR(413, "谷歌验证码不正确"),

    INTERNAL_SERVER_ERROR(500, "系统繁忙"),   //服务器内部错误

    METHOD_NOT_ALLOWED(405,"方法不被允许"),

    DATA_IS_EXISTED(101,"数据已经存在"),

    DATA_NOT_EXISTED(102,"数据不存在"),

    /*参数错误:1001-1999*/
    PARAMS_IS_INVALID(1001, "参数无效"),

    PARAMS_IS_BLANK(1002, "参数为空"),

    PRODUCT_NOT_EXI(1003, "未找到对应商品"),

    MINIMUM_NOT_REACHED(1004, "未达到最小数量"),

    INSUFFICIENT_USER_BALANCE(1005,"用户余额不足"),
    // 钱包未设置
    WALLET_NOT_SET(1006,"钱包未设置"),

    NO_AUTHORITY(1007,"无权限"),

    ROLE_NAME_REPEAT(1101,"角色名称已存在"),

    ORDER_EXISTED(2001,"订单已存在"),

    NO_AVAILABLE_WALLET(2002,"没有可用的钱包"),

    WALLET_NOT_EXISTED(2003, "wallet not exists"),

    FREEZE_AMOUNT_INVALID(2004, "freeze amount invalid"),

    WALLET_BALANCE_INSUFFICIENT(2005, "wallet balance insufficient"),

    RESTORE_AMOUNT_INVALID(2006, "restore amount invalid"),

    WALLET_FROZEN_BALANCE_INSUFFICIENT(2007, "wallet frozen balance insufficient"),

    AMOUNT_INVALID(2008, "amount invalid"),

    USER_BALANCE_INSUFFICIENT(2009, "user balance insufficient"),

    RESTORE_USER_BALANCE_FAILED(2010, "restore user balance failed"),

    DEDUCT_USER_BALANCE_FAILED(2011, "deduct user balance failed"),

    ORDER_STATUS_ERROR(2012, "order status error"),

    NO_CAN_USE_SOCKS(2013, "没有可用的代理"),

    NO_CAN_USE_DEVICE(2014, "没有可用设备"),

    REACH_USE_DEVICE(2014, "已超过可用设备"),

    GATEWAY_CALL_ERROR(2015, "网关调用错误"),

    NO_ACCOUNT(2016, "账号不存在"),
    NO_VPS(2017, "没有可用的设备"),
    DEFAULT_GROUP_NOT_EXIST(2018, "默认分组不存在"),
    DELETE_ACCOUNT_FAIL(2019, "账号删除失败"),

    NO_ONLINE_ACCOUNT(2020, "无可用在线账号"),
    TOO_LONG_TEXT(2021, "发送消息内容过长"),
    ACCOUNT_NO_TOKEN(2022, "账号没有TOKEN"),
    NO_PERMISSION(2023, "没有权限"),
    FILE_TYPE_NOT_ALLOWED(2024, "不支持的文件类型"),
    ACCOUNT_BANNED(2025, "账号被封"),
    LIMIT_FILE_SIZE(2026, "限制文件大小"),
    GOOGLE_AI_ERROR(2027, "GOOGLE AI 报错"),
    NOT_SUPPORT_PROMPT_VERSION(2028, "不支持的提示词版本"),
    OUT_OF_STOCK(2029, "库存不足"),
    RELEASE_QUICK(2030, "释放太频繁，每个小时最多释放60个"),
    NO_ENOUGH_ACCOUNT(2031, "可用账号数量不足"),
    TOO_MANY_REQUEST(2032, "请求太频繁"),
    NO_SUPPORT_MODEL(2033, "不支持的模型"),
    CHATGPT_AI_ERROR(2034, "CHATGPT AI 报错"),
    SEND_MAIL_FAIL(2035, "发送邮件失败"),
    CREATE_ORDER_FAIL(2036, "create order failed"),

    MAIL_TEMPLATE_GROUP_IN_USE(2037, "只能删除空分组");

    private Integer code;

    private String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
