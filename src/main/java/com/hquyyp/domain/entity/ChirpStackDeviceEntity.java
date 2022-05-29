package com.hquyyp.domain.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;

/**
 * <p>
 *      从ChirpStack获取的设备对象
 * <p>
 *
 * @author liulingyu
 * @date 2022/5/24 20:43
 * @Version 1.0
 */
@Data
public class ChirpStackDeviceEntity {
    
    private String devEUI;

    private String name;
    
    private String applicationID;

    private String description;
    
    private String deviceProfileID;
    
    private String deviceProfileName;
    
    private int deviceStatusBattery;
    
    private int deviceStatusMargin;

    private boolean deviceStatusExternalPowerSource;
    
    private boolean deviceStatusBatteryLevelUnavailable;
    
    private int deviceStatusBatteryLevel;
    
    private Date lastSeenAt;
}
