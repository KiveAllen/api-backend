package com.api.project.model.vo;

import com.api.apicommon.model.entity.InterfaceInfo;
import com.api.project.model.entity.Post;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 接口信息封装视窗
 *
 * @author api
 * @TableName product
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class InterfaceInfoVO extends InterfaceInfo {

    /**
     * 调用次数
     */
    private Integer totalNum;

    private static final long serialVersionUID = 1L;
}