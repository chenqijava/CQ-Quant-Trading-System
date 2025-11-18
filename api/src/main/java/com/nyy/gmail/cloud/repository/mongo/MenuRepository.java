package com.nyy.gmail.cloud.repository.mongo;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.nyy.gmail.cloud.common.MenuType;
import com.nyy.gmail.cloud.entity.mongo.Menu;

import jakarta.annotation.Resource;

import java.util.List;

@Repository
public class MenuRepository {

    @Resource
    private MongoTemplate mongoTemplate;

    public void saveMenu(Menu menu){
        mongoTemplate.insert(menu);
    }

    public void updateMenu(Menu menu){
        mongoTemplate.save(menu);
    }

    public List<Menu> findMenusByIds(List<String> ids){
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").in(ids));
        List<Menu> menuList = mongoTemplate.find(query,Menu.class);
        return menuList;
    }

    public Menu findMenuByKey(MenuType menuType){
        Query query = new Query();
        query.addCriteria(Criteria.where("key").is(menuType.toString()));
        Menu menu = mongoTemplate.findOne(query, Menu.class);
        return menu;
    }

    public List<Menu> findAll(){
        return mongoTemplate.findAll(Menu.class);
    }
}

