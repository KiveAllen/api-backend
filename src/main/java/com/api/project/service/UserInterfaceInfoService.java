package com.api.project.service;

import com.api.apicommon.model.entity.UserInterfaceInfo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author wiqun
* @description 针对表【user_interface_info(用户调用接口关系)】的数据库操作Service
* @createDate 2023-10-10 18:07:56
*/
public interface UserInterfaceInfoService extends IService<UserInterfaceInfo> {
    /**
     * 校验
     * @param userInterfaceInfo
     * @param add
     */
    void validUserInterfaceInfo(UserInterfaceInfo userInterfaceInfo, boolean add);

    /**
     * 调用接口统计
     * @param interfaceInfoId
     * @param userId
     * @return
     */
    boolean invokeCount(long interfaceInfoId,long userId);
}
