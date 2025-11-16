package com.nyy.gmail.cloud.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.lang.NonNull;

import java.util.ArrayList;
import java.util.List;

@Component
public class BeanUtils implements ApplicationContextAware {
    private static ApplicationContext context;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    public static <T> T getBean(Class<T> t) {
        return context.getBean(t);
    }

    public static Object getBean(String name) {
        return context.getBean(name);
    }

    /**
     * 拷贝对象
     * @param sourceBean    源对象
     * @param targetBean    目标对象
     */
    public static <E,T> void beanCopy(E sourceBean, T  targetBean){
        // 使用spring自带的Bean拷贝方法
        if ( sourceBean == null ){
            return;
        }
        org.springframework.beans.BeanUtils.copyProperties(sourceBean,targetBean);
    }

    /**
     * 拷贝列表对象
     * @param sourceBeans   源对象列表
     * @param targetClazz 目标对象类
     * @return
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public static <E,T> List<T> beansCopy(List<E> sourceBeans, Class<T> targetClazz)  {
        // 如果对象列表是null则直接返回null
        if ( sourceBeans == null ){
            return null;
        }
        // 新建目标类链表,并最终返回
        List<T> targetBeans = new ArrayList<>();
        // 遍历整个源对象列表
        for (E sourceBean : sourceBeans) {
            T targetBean = null;
            try {
                targetBean = targetClazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if ( targetBean == null ){
                continue;
            }
            beanCopy(sourceBean,targetBean);
            targetBeans.add(targetBean);
        }
        return  targetBeans;
    }
}
