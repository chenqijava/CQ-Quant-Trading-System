package com.nyy.gmail.cloud.common.configuration;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.mongodb.client.result.DeleteResult;
import com.nyy.gmail.cloud.common.MenuTree;
import com.nyy.gmail.cloud.common.MenuType;
import com.nyy.gmail.cloud.common.constants.Constants;
import com.nyy.gmail.cloud.entity.mongo.Menu;
import com.nyy.gmail.cloud.entity.mongo.Region;
import com.nyy.gmail.cloud.entity.mongo.Role;
import com.nyy.gmail.cloud.entity.mongo.User;
import com.nyy.gmail.cloud.utils.ByteUtil;
import com.nyy.gmail.cloud.utils.MD5Util;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Slf4j
@Component
public class MongoDataInit {
    @Value("${admin.password}")
    private String password;

    @Resource
    private MongoTemplate mongoTemplate;

    @Value("${application.taskType}")
    private String taskType;

    @SneakyThrows
    @PostConstruct
    public void constructMenus() {
        //  先将所有菜单的isDel置成true,在菜单初始化时会将isDel置成false,查询菜单时只会查isDel为false的数据
        mongoTemplate.updateMulti(new Query(), new Update().set("isDel", true), Menu.class);
        //  初始化菜单
        this.initMenus(Arrays.asList(new MenuTree().setKey(MenuType.resource).setIcon(new HashMap<String, String>() {{
                    put("type", "Frame-2.png");
                }}).setChildList(Arrays.asList(
                        new MenuTree().setKey(MenuType.accountGroup),
                        new MenuTree().setKey(MenuType.account)
                )),
                new MenuTree().setKey(MenuType.settings).setIcon(new HashMap<String, String>() {{
                    put("type", "zhongxin.png");
                }}).setChildList(
                        Arrays.asList(
                                new MenuTree().setKey(MenuType.socks5),
                                new MenuTree().setKey(MenuType.proxyAccount),
                                new MenuTree().setKey(MenuType.adminRole),
                                new MenuTree().setKey(MenuType.adminUser),
                                new MenuTree().setKey(MenuType.apiKey),
                                new MenuTree().setKey(MenuType.globalParams)
                        ))
        ), null);
        //  删除未更新的菜单项
        DeleteResult deleteResult = mongoTemplate.remove(new Query(where("isDel").is(true)), Menu.class);
        log.info("初始化删除菜单个数:{}", deleteResult.getDeletedCount());
        //  初始化admin角色
        List<String> menusIds =
            mongoTemplate.findAll(Menu.class).stream().map(menu -> menu.get_id()).collect(Collectors.toList());
        Role adminRole = mongoTemplate.findAndModify(new Query(where("userID").is("admin").and("name").is("admin")),
            new Update().set("permissions", menusIds), new FindAndModifyOptions().upsert(true).returnNew(true),
            Role.class);
        //  初始化admin用户
        String pwd = MD5Util.MD5(MD5Util.MD5("admin") + password);
        User user = mongoTemplate.findOne(new Query(where("userID").is("admin")), User.class);
        if (user != null) {
            user.setPassword(pwd);
            user.setName("管理");
            user.setRole(adminRole.get_id());
            user.setStatus("enable");
            mongoTemplate.save(user);
        } else {
            user = new User();
            user.setPassword(pwd);
            user.setName("管理");
            user.setRole(adminRole.get_id());
            user.setStatus("enable");
            user.setUserID("admin");
            mongoTemplate.save(user);
        }
//        mongoTemplate.indexOps(User.class).ensureIndex(new Index().on("userID", Sort.Order.asc("userID").getDirection()).unique());
//        Update update_Admin = new Update();
//        update_Admin.set("name", "管理").set("status", "enable").set("role", adminRole.get_id()).set("password", pwd);
//        mongoTemplate.findAndModify(new Query(where("userID").is("admin")), update_Admin,
//            new FindAndModifyOptions().upsert(true).returnNew(true), User.class);
    }

    public void initMenus(List menus, String parent) {
        Integer i = 0;
        for (Iterator itr = menus.iterator(); itr.hasNext(); ) {
            MenuTree menuTree = (MenuTree) itr.next();
            Update updateData = new Update().set("index", i++).set("isDel", false).set("parent", parent)
                .set("name", menuTree.getKey().getDesc()).set("url", menuTree.getKey().getPath()).set("type", menuTree.getKey().getType())
                .set("icon", menuTree.getIcon());
            Menu m = mongoTemplate.findAndModify(new Query(where("key").is(menuTree.getKey())), updateData,
                new FindAndModifyOptions().upsert(true).returnNew(true), Menu.class);
            if (menuTree.getChildList() != null)
                this.initMenus(menuTree.getChildList(), m.get_id());
        }
    }


    @PostConstruct
    public void initRegion() {
        int rank = 1;
        JSONObject regionJson = JSON.parseObject(Constants.REGION_JSON_STR);
        for (Map.Entry<String, Object> entry : regionJson.entrySet()) {
            Region region = new Region();
            String key = entry.getKey();
            String displayName = entry.getValue().toString();
            if (StringUtils.isAnyBlank(key, displayName)) {
                continue;
            }
            region.setName(key);
            region.setDisplayName(displayName);
            region.setRank(rank++);
            try {
                mongoTemplate.insert(region);
            } catch (org.springframework.dao.DuplicateKeyException e) {
                //                e.printStackTrace();
            }
        }

    }

}



