package com.nyy.gmail.cloud.repository.mongo;

import com.nyy.gmail.cloud.model.dto.RoleListDTO;
import com.nyy.gmail.cloud.common.pagination.MongoPageHelper;
import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.entity.mongo.Role;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import jakarta.annotation.Resource;
import java.util.List;

@Repository
public class RoleRepository {

    @Resource
    private MongoTemplate mongoTemplate;

    @Autowired
    private MongoPageHelper mongoPageHelper;

    public void saveRole(Role role){
        mongoTemplate.insert(role);
    }

    public void updateRole(Role role){
        mongoTemplate.save(role);
    }

    public Role _updateRole(Role role){
        Query query = new Query(Criteria.where("_id").is(role.get_id()));
        Update update = new Update();
        update.set("userID", role.getUserID());
        update.set("name", role.getName());
        update.set("permissions", role.getPermissions());
        return mongoTemplate.findAndModify(query, update, Role.class);
    }

    public Role findRoleById(String id){
        Query query = new Query(Criteria.where("_id").is(id));
        Role role = mongoTemplate.findOne(query,Role.class);
        return role;
    }

    public List<Role> findRoleByName(String name,String userID){
        Query query = new Query(Criteria.where("name").is(name).and("userID").is(userID));
        List<Role> roles = mongoTemplate.find(query,Role.class);
        return roles;
    }

    public Role findRoleByIdUserID(String id, String userID){
        Query query = new Query(Criteria.where("_id").is(id).and("userID").is(userID));
        Role role = mongoTemplate.findOne(query,Role.class);
        return role;
    }

    public List<Role> findRoleByIds(List<String> ids, String userID){
        Query query = new Query(Criteria.where("userID").is(userID).and("_id").in(ids));
        List<Role> roleList = mongoTemplate.find(query,Role.class);
        return roleList;
    }

    public List<Role> findRoleByIds(List<String> ids){
        Query query = new Query(Criteria.where("_id").in(ids));
        List<Role> roleList = mongoTemplate.find(query,Role.class);
        return roleList;
    }

    public void deleteRoleById(String id){
        Query query = new Query(Criteria.where("_id").is(id));
        mongoTemplate.remove(query,Role.class);
    }

    public PageResult<Role> findRolePageList(RoleListDTO roleListDTO, int pageSize, int pageNumber){
        if (roleListDTO == null) {
            Query query = new Query();
            PageResult<Role> pageResult =  mongoPageHelper.pageQuery(query, Role.class, pageSize, pageNumber);
            return pageResult;
        }else {
            Query query = new Query();
            if (StringUtils.isNotEmpty(roleListDTO.getName())){
                query.addCriteria(Criteria.where("name").regex(roleListDTO.getName()));
            }
            if (StringUtils.isNotEmpty(roleListDTO.getUserID())){
                query.addCriteria(Criteria.where("userID").is(roleListDTO.getUserID()));
            }
            PageResult<Role> pageResult = mongoPageHelper.pageQuery(query, Role.class, pageSize, pageNumber);
            return pageResult;
        }
    }

    public Role findRoleByPermission(String permission){
        Query query = new Query(Criteria.where("permissions").elemMatch(new Criteria().is(permission)));
        Role role = mongoTemplate.findOne(query, Role.class);
        return role;
    }

    public List<Role> findRoleByUserID(List<String> userID) {
        Query query = new Query(Criteria.where("userID").in(userID));
        List<Role> roleList = mongoTemplate.find(query,Role.class);
        return roleList;
    }
}
