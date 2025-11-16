package com.nyy.gmail.cloud.common.annotation;

import com.nyy.gmail.cloud.common.MenuType;

import java.lang.annotation.*;

/**
 * 权限注解,添加在controller的类或方法上,只有已登录且拥有对应权限的用户才能访问
 */

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface RequiredPermission {
    MenuType[] value();
}
