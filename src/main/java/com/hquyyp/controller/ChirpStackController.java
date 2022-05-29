package com.hquyyp.controller;

import com.hquyyp.domain.dto.response.BaseResponse;
import com.hquyyp.domain.query.BaseQueryEntity;
import com.hquyyp.service.ChirpStackService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.jboss.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import javax.websocket.server.PathParam;
import java.util.Map;


/**
 * <p>
 *      ChirpStack服务控制器
 * <p>
 *
 * @author liulingyu
 * @date 2022/5/24 17:24
 * @Version 1.0
 */
@Api(tags = {"ChirpStack服务接口"})
@RestController
@RequestMapping("/chirpStack")
public class ChirpStackController {
    private static final Logger log = Logger.getLogger(com.hquyyp.controller.ChirpStackController.class.getName());


    @Autowired
    private ChirpStackService chirpStackService;

    /**
     * <p>
     *     chirpStack服务器登录接口
     * </p>
     * @deprecated
     * @author liulingyu
     * @date 2022-05-28 16:26
     * @version 1.1
     */
    @GetMapping("/login")
    public BaseResponse login(){

        chirpStackService.login();

        return BaseResponse.builder()
                .code(200)
                .status("SUCCESS")
                .msg("")
                .build();
    }

    @PostMapping("/get-devices")
    @ApiOperation(value = "获取设备列表",httpMethod = "POST",notes = "")
    public BaseResponse getDevices(@RequestBody BaseQueryEntity query){

        Map deviceList = chirpStackService.getDeviceList(query);

        return BaseResponse.builder()
                .code(200)
                .status("SUCCESS")
                .msg("")
                .data(deviceList)
                .build();
    }
    @GetMapping("/devEUI")
    @ApiOperation(value = "获取设备EUI列表",httpMethod = "GET",notes = "")
    public BaseResponse getDevEUIList(){

        Map devEUIList = chirpStackService.getDevEUIList();

        return BaseResponse.builder()
                .code(200)
                .status("SUCCESS")
                .msg("")
                .data(devEUIList)
                .build();
    }

    @DeleteMapping("/device/{devEUI}")
    @ApiOperation(value = "删除设备",httpMethod = "DELETE",notes = "根据devEUI删除设备")
    public BaseResponse deleteDevice(@PathVariable("devEUI") String devEUI){

        try {
            chirpStackService.deleteDeviceByEUI(devEUI);
        } catch (HttpClientErrorException e) {

            log.error(e.getMessage());
            return BaseResponse.builder()
                    .code(404)
                    .status("fail")
                    .msg("要删除的设备不存在")
                    .build();
        }

        return BaseResponse.builder()
                .code(200)
                .status("SUCCESS")
                .msg("")
                .build();
    }
}
