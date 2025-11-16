package com.nyy.gmail.cloud.controller;

import com.nyy.gmail.cloud.common.MenuType;
import com.nyy.gmail.cloud.common.Session;
import com.nyy.gmail.cloud.common.annotation.RequiredPermission;
import com.nyy.gmail.cloud.common.exception.PermissionException;
import com.nyy.gmail.cloud.common.response.ResponseResult;
import com.nyy.gmail.cloud.common.response.Result;
import com.nyy.gmail.cloud.common.response.ResultCode;
import com.nyy.gmail.cloud.model.dto.vps.*;
import com.nyy.gmail.cloud.model.vo.vps.VpsInfoVO;
import com.nyy.gmail.cloud.model.vo.vps.VpsPageResult;
import com.nyy.gmail.cloud.repository.mongo.UserRepository;
import com.nyy.gmail.cloud.service.VpsInfoService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
//@RequiredPermission({MenuType.machine,MenuType.adminVps})
@RequestMapping({"/api/vps"})
public class VpsController {

    @Resource
    private VpsInfoService vpsInfoService;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private UserRepository userRepository;
    /**
     * 购买设备
     *
     * @param vpsBuyDTO
     * @return
     */
    @PostMapping("/buy")
    public Result<BigDecimal> buyVps(@RequestBody VpsBuyDTO vpsBuyDTO) {

        vpsBuyDTO.setUserId((String) request.getSession().getAttribute("userID"));
        return vpsInfoService.buyVps(vpsBuyDTO);
    }

    @PostMapping("/addVps")
    public Result addVps(@RequestBody VpsAddDTO vpsAddDTO) {

        vpsAddDTO.setUserId((String) request.getSession().getAttribute("userID"));
        return vpsInfoService.addVps(vpsAddDTO);
    }

    /**
     * 根据到期时间倒序
     *
     * @return
     */
    @PostMapping("/deadTime/query")
    public Result<List<VpsInfoDTO>> queryByUserId(@RequestBody VpsQueryDTO dto) {

        if (dto.getUserAdmin() != null && dto.getUserAdmin()) {
            if (dto.getFilters() != null && dto.getFilters().size() > 0 && dto.getFilters().get("userID") != null) {
                dto.setUserId((String) dto.getFilters().get("userID"));
                if(userRepository.findUserByCreateUserIDAndUserID((String) request.getSession().getAttribute("userID"),(String) dto.getFilters().get("userID")) ==null){
                    throw new PermissionException();
                }
            }
        } else {
            dto.setUserId((String) request.getSession().getAttribute("userID"));
        }

        return vpsInfoService.queryDeadTimeByUserId(dto);
    }

    /**
     * 批次续费
     *
     * @return
     */
    @PostMapping("/batch/renew")
    public Result<BigDecimal> batchRenew(@RequestBody VpsRenewDTO vpsRenewDTO) {

        vpsRenewDTO.setUserId((String) request.getSession().getAttribute("userID"));
        return vpsInfoService.batchRenewByDateBatch(vpsRenewDTO);
    }


    @PostMapping("{pageSize}/{pageNo}")
    public VpsPageResult<VpsInfoVO> queryVpsInfoByPage(@PathVariable Integer pageSize, @PathVariable Integer pageNo, @RequestBody(required = false) VpsQueryDTO dto) {

        if (dto == null) {
            dto = new VpsQueryDTO();
        }
        String userId = null;
        if (dto.getUserAdmin() != null && dto.getUserAdmin()) {
            if (dto.getFilters() != null && dto.getFilters().size() > 0 && dto.getFilters().get("userID") != null) {
                userId = (String) dto.getFilters().get("userID");
                if(userRepository.findUserByCreateUserIDAndUserID((String) request.getSession().getAttribute("userID"),(String) dto.getFilters().get("userID")) ==null){
                    throw new PermissionException();
                }
            }
        } else {
            userId = (String) request.getSession().getAttribute("userID");
        }
        dto.setUserId(userId);
        dto.setPageNo(pageNo);
        dto.setPageSize(pageSize);
        return vpsInfoService.queryVpsInfoByPage(dto);
    }

    @PostMapping("/updateDescription")
    public Result<Boolean> updateDesc(@RequestParam(name = "vpsId", required = true) String vpsId, @RequestParam(name = "description", required = true) String desc) {

        return vpsInfoService.updateDesc(vpsId, desc, Session.currentSession().userID);
    }

    @PostMapping("/statistics")
    public Result<Boolean> statistics() {

        return ResponseResult.success(true);
    }
}
