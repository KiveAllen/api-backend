package com.api.project.service;

import com.api.apicommon.model.entity.InterfaceInfo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 *
 */
public interface InterfaceInfoService extends IService<InterfaceInfo> {

    /**
     * 校验
     * @param interfaceInfo
     * @param add
     */
    void validInterfaceInfo(InterfaceInfo interfaceInfo, boolean add);
}
