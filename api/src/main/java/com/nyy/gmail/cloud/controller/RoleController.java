package com.nyy.gmail.cloud.controller;

import com.nyy.gmail.cloud.common.annotation.RequiredPermission;
import com.nyy.gmail.cloud.common.exception.CommonException;
import com.nyy.gmail.cloud.entity.mongo.User;
import com.nyy.gmail.cloud.model.dto.RoleListDTO;
import com.nyy.gmail.cloud.model.vo.RoleDeleteVO;
import com.nyy.gmail.cloud.model.vo.RoleSaveVO;
import com.nyy.gmail.cloud.entity.mongo.Role;
import com.nyy.gmail.cloud.common.pagination.PageResult;
import com.nyy.gmail.cloud.repository.mongo.MenuRepository;
import com.nyy.gmail.cloud.repository.mongo.RoleRepository;
import com.nyy.gmail.cloud.repository.mongo.UserRepository;

import jakarta.servlet.http.HttpServletRequest;

import com.nyy.gmail.cloud.common.response.ResponseResult;
import com.nyy.gmail.cloud.common.response.Result;
import com.nyy.gmail.cloud.common.response.ResultCode;
import com.nyy.gmail.cloud.common.MenuType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author liang sishuai
 * @version 1.0
 * @date 2021-09-16
 */
@RestController
@RequestMapping("/api/consumer/role")
@RequiredPermission(MenuType.adminRole)
public class RoleController {


    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private MenuRepository menuRepository;

    @PostMapping("/card/save")
    public Result<RoleSaveVO> save(@RequestBody Role role) {
        String userID = (String) request.getSession().getAttribute("userID");
        try {
            if (role.get_id() != null && !role.get_id().equals("")) {

                Role r = roleRepository.findRoleById(role.get_id());
                if (!r.getUserID().equals(userID)) {
                    throw new CommonException(ResultCode.PARAMS_IS_INVALID);
                }
                if (StringUtils.isNotEmpty(role.getName()) && !r.getName().equals(role.getName())) {
                    //修改了角色名称，校验名称
                    List<Role> list = roleRepository.findRoleByName(role.getName(), userID);
                    if (!CollectionUtils.isEmpty(list)) {
                        return ResponseResult.failure(ResultCode.ROLE_NAME_REPEAT);
                    }
                }

                //更新
                role.setUserID(userID);

                User user = userRepository.findOneByUserID(userID);
                List<String> permissions = new ArrayList<>();
                Role ownRole = null;
                if (user != null) ownRole = roleRepository.findRoleById(user.getRole());
                if (role != null) permissions.addAll(menuRepository.findMenusByIds(ownRole.getPermissions()).stream().map(e -> e.get_id()).toList());
                role.setPermissions(role.getPermissions().stream().filter(e -> permissions.indexOf(e) >= 0).toList());

                roleRepository.updateRole(role);//version值必须和数据库中的值相等，保存的时候会+1，否则会报重复id主键的错误
                //把主账号没有，但是子账号有权限，移除
                List<User> users = userRepository.findUserByRole(role.get_id()); // 找出这个角色的所有账号
                if (!CollectionUtils.isEmpty(users)) {
                    List<String> userIds = users.stream().map(User::getUserID).collect(Collectors.toList());
                    userIds = findAllUserIds(userIds);// 找出这些账号创建的子账号
                    List<Role> list = roleRepository.findRoleByUserID(userIds); // 子账号的角色
                    list.forEach(e -> {
                        List<String> ps = new ArrayList<>(e.getPermissions());
                        e.getPermissions().forEach(p -> {
                            if (!role.getName().equals("admin") && !role.getPermissions().contains(p)) {
                                ps.remove(p);
                            }
                        });
                        e.setPermissions(ps);
                        roleRepository.updateRole(e);
                    });
                }
                RoleSaveVO roleSaveVO = new RoleSaveVO();
                roleSaveVO.set_id(role.get_id());
                Result<RoleSaveVO> result = ResponseResult.success(roleSaveVO);
                return result;
            } else {
                List<Role> list = roleRepository.findRoleByName(role.getName(), userID);
                if (!CollectionUtils.isEmpty(list)) {
                    return ResponseResult.failure(ResultCode.ROLE_NAME_REPEAT);
                }
                //新建
                role.setUserID(userID);
                User user = userRepository.findOneByUserID(userID);
                List<String> permissions = new ArrayList<>();
                Role ownRole = null;
                if (user != null) ownRole = roleRepository.findRoleById(user.getRole());
                if (role != null) permissions.addAll(menuRepository.findMenusByIds(ownRole.getPermissions()).stream().map(e -> e.get_id()).toList());
                role.setPermissions(role.getPermissions().stream().filter(e -> permissions.indexOf(e) >= 0).toList());

                roleRepository.saveRole(role);

                RoleSaveVO roleSaveVO = new RoleSaveVO();
                roleSaveVO.set_id(role.get_id());
                Result<RoleSaveVO> result = ResponseResult.success(roleSaveVO);
                return result;
            }
        } catch (Exception e) {
            Result<RoleSaveVO> result = ResponseResult.failure(ResultCode.ERROR, null);
            result.setMessage(e.getMessage());
            return result;
        }
    }

    private List<String> findAllUserIds(List<String> userIds) {
        List<String> tmp = new ArrayList<>(userIds);
        List<String> list = userRepository.findUserByCreateUserIDs(userIds).stream().map(User::getUserID).toList();
        if (list.isEmpty()) {
            return tmp;
        }
        List<String> ids = findAllUserIds(list);
        if (ids.size() == list.size()) {
            return tmp;
        }
        tmp.addAll(ids);
        return tmp;
    }

    @PostMapping("/delete")
    public Result<RoleDeleteVO> delete(@RequestBody List<String> ids) {
        String userID = (String) request.getSession().getAttribute("userID");
        List<Role> roles = roleRepository.findRoleByIds(ids, userID);
        Map failed = new HashMap();
        List<String> names = new ArrayList<String>();
        List<String> failedType = new ArrayList<String>();
        Long count = 0L;
        String msg = "该角色正在使用";

        for (Role role : roles) {
            count = userRepository.countByRole(role);
            if (count > 0) {
                if (failed.get(msg) == null) {
                    failedType.add(msg);
                }
                names.add(role.getName());
                failed.put(msg, names);
            } else {
                roleRepository.deleteRoleById(role.get_id());
            }
        }

        RoleDeleteVO roleDeleteVO = new RoleDeleteVO();
        roleDeleteVO.setFailed(failed);
        roleDeleteVO.setFailedType(failedType);
        Result<RoleDeleteVO> result = ResponseResult.success(roleDeleteVO);
        return result;
    }

    @GetMapping("/card/{_id}")
    public Result<Role> get(@PathVariable String _id) {
        String userID = (String) request.getSession().getAttribute("userID");
        Role role = roleRepository.findRoleByIdUserID(_id, userID);
        Result<Role> result = ResponseResult.success(role);
        return result;
    }

    @PostMapping("/{pageSize}/{page}")
    public Result<PageResult<Role>> list(@PathVariable("pageSize") int pageSize, @PathVariable("page") int page, @RequestBody(required = false) RoleListDTO roleListDTO) {
        String userID = (String) request.getSession().getAttribute("userID");

        if (roleListDTO == null) {
            roleListDTO = new RoleListDTO();
        }
        if (roleListDTO != null && roleListDTO.getFilters() != null) {
            Map<String, String> filtersMap = roleListDTO.getFilters();
            String name = filtersMap.get("name");
            //String userID = filtersMap.get("userID");

            if (StringUtils.isNotEmpty(name)) {
                roleListDTO.setName(name);
            }
        }
        // 全局的角色列表，只能看到自己创建的角色
        roleListDTO.setUserID(userID);

        PageResult<Role> pageResult = roleRepository.findRolePageList(roleListDTO, pageSize, page);//按id倒排序
        Result<PageResult<Role>> result = ResponseResult.success(pageResult);
        return result;
    }
}
