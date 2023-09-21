package com.api.apiclientsdk.model;

import lombok.Data;

/**
 * 用户
 */
@Data
public class User {
    /**
     * 用户id
     */
    private long id;

    /**
     * 用户名称
     */
    private String userName;
}
