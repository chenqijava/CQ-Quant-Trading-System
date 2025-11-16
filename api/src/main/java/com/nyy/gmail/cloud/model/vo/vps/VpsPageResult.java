package com.nyy.gmail.cloud.model.vo.vps;

import com.nyy.gmail.cloud.common.pagination.PageResult;
import lombok.Data;

import java.util.Map;

@Data
public class VpsPageResult<VpsInfoVO> extends PageResult<VpsInfoVO> {

    private Map<String,Integer> runStatusCount;

    private Map<String,Integer> deadStatusCount;

    private Map<String,Integer> bindStatusCount;

    private Map<String,Integer> userBindCount;
}
