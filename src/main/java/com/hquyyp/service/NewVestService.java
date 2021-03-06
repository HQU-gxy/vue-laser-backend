package com.hquyyp.service;

import com.alibaba.fastjson.JSONObject;
import com.hquyyp.MqttInterface.MqMessager;
import com.hquyyp.domain.entity.NewRecordEntity;
import com.hquyyp.domain.entity.NewVestEntity;
import com.hquyyp.domain.model.PositionWebSocketModel;
import com.hquyyp.domain.model.SendDataModel;
import com.hquyyp.domain.model.ShootWebSocketModel;
import com.hquyyp.domain.po.BattleNewSetting;
import com.hquyyp.domain.vo.NewVestView;
import com.hquyyp.utils.OutsiderUtil;
import com.hquyyp.websocket.ShootWebSocket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.logging.Logger;

@Service
public class NewVestService {
    private static final Logger logger=Logger.getLogger(NewVestService.class.getName());

    @Autowired
    MqMessager mqMessager;

    @Autowired
    private ShootWebSocket shootWebSocket;

    @Autowired
    private NewBattleService newBattleService;

    protected List<NewVestView> vestEntityList=new ArrayList<>();

    @Value("${mq.PublishTopicPrefix}")
    private String PublishTopicPrefix;

    @Value("${mq.PublishTopicSuffix}")
    private String PublishTopicSuffix;


    public void NewLoadVest(BattleNewSetting battleSettingEntity) {
        this.vestEntityList.clear();
        List<NewVestEntity> blueTeamList = JSONObject.parseArray(battleSettingEntity.getBlueTeamList(), NewVestEntity.class);
        List<NewVestEntity> redTeamList = JSONObject.parseArray(battleSettingEntity.getRedTeamList(), NewVestEntity.class);

        for (NewVestEntity nve:blueTeamList){
            NewVestView newVestView=new NewVestView(nve);
            newVestView.setTeam("blue");
            newVestView.setAmmo(0);
            newVestView.setHp(100);
            this.vestEntityList.add(newVestView);
            NewRecordEntity newRecordEntity=NewRecordEntity.builder()
                    .id(nve.getId()).name(nve.getName()).kill(0)
                    .team("blue").beShooted(0).shoot(0)
                    .build();
            this.newBattleService.recordData.add(newRecordEntity);
        }
        for (NewVestEntity nve:redTeamList){
            NewVestView newVestView=new NewVestView(nve);
            newVestView.setTeam("red");
            newVestView.setAmmo(0);
            newVestView.setHp(100);
            this.vestEntityList.add(newVestView);
            NewRecordEntity newRecordEntity=NewRecordEntity.builder()
                    .id(nve.getId()).name(nve.getName()).kill(0)
                    .team("red").beShooted(0).shoot(0)
                    .build();
            this.newBattleService.recordData.add(newRecordEntity);
        }
    }

    public List<NewVestView> listAllNewVest() {
        //vestEntityList.sort(Comparator.comparingInt(VestEntity::getNum));
        return this.vestEntityList;
    }

    public NewVestView getNewVest(Integer id) {
        NewVestView newVestView=new NewVestView();
        for (NewVestView nvv:this.vestEntityList) {
            if (Integer.parseInt(nvv.getId()) == id){
                newVestView=nvv;
            }
        }
        return newVestView;
    }

    public void clearAllVest() {
        this.vestEntityList.clear();
    }

    public void newSendDieData(int vestNum) {
        for (NewVestView nvv:this.vestEntityList) {
            if (Integer.parseInt(nvv.getId()) == vestNum){
                nvv.setHp(0);
                String team=nvv.getTeam();
                String  equipment = nvv.getEquipment();
                String DieData="2 "+(("red".equals(team)) ? 1 : 0)+" "+equipment;
                try {
                    mqMessager.send(PublishTopicPrefix+nvv.getEquipment()+PublishTopicSuffix,2,DieData);
                }catch (Exception e){
                    System.out.println("??????mqtt????????????????????????!");
                }
            }
        }
    }

    public void newSendDieInjure(int vestNum, int injure) {
        for (NewVestView nvv:this.vestEntityList) {
            if (Integer.parseInt(nvv.getId()) == vestNum){
                String team=nvv.getTeam();
                String equipment=nvv.getEquipment();
                String injureData="1 "+(("red".equals(team)) ? 1 : 0)+" "+equipment+" "+1;
                try {
                    if (injure<=0){
                        return;
                    }
                    else if (injure==1){
                        nvv.setHp(66);
                        mqMessager.send(PublishTopicPrefix+nvv.getEquipment()+PublishTopicSuffix,2,injureData);
                    }
                    else if (injure==2){
                        nvv.setHp(33);
                        mqMessager.send(PublishTopicPrefix+nvv.getEquipment()+PublishTopicSuffix,2,injureData);
                        mqMessager.send(PublishTopicPrefix+nvv.getEquipment()+PublishTopicSuffix,2,injureData);
                    }
                    else{
                        this.newSendDieData(vestNum);
                    }
                }catch (Exception e){
                    System.out.println("??????mqtt????????????????????????!");
                }
            }
        }
    }

    public void NewAllLoadAmmo(int ammoNum){
       for (NewVestView nvv:this.vestEntityList){
           NewLoadAmmo(Integer.parseInt(nvv.getId()),ammoNum);
       }
    }

    public void NewLoadAmmo(int vestNum, int ammoNum) {
        for (NewVestView nvv:this.vestEntityList){
            if (Integer.parseInt(nvv.getId())==vestNum){
                int oldammo = nvv.getAmmo();
                nvv.setAmmo(oldammo+ammoNum);

                //??????mqtt????????????
                SendDataModel sendDataModel=SendDataModel.builder()
                        .fPort(5).data(OutsiderUtil.intToBase64(ammoNum)).build();
                try {
                    mqMessager.send(PublishTopicPrefix+nvv.getEquipment()+PublishTopicSuffix,2,JSONObject.toJSONString(sendDataModel));
                }catch (Exception e){
                    System.out.println("??????mqtt????????????????????????!");
                }
            }
        }
    }

    public void refreshVest(NewVestView newVestEntity){
       this.vestEntityList.add(newVestEntity);
       Collections.sort(this.vestEntityList);
    }

    public void handleVestMqData(String data){
        logger.info("???mqtt???????????????"+data);
        String[] dataSplit=null;
        //??????????????????????????????
        try{
            dataSplit = data.split(" ");
        }catch (Exception e){
            logger.info("???mqtt????????????????????????????????????");
        }
        //???????????? ????????? ???????????? ????????????
        //    0     1       2       3
        if("91".equals(dataSplit[0])) {
            //????????????
            if (dataSplit.length == 4) {
                String position="";
                switch (Integer.parseInt(dataSplit[3])) {
                    case 1:
                        position = "??????";
                        break;
                    case 4:
                        position = "??????";
                        break;
                    case 8:
                        position = "??????";
                        break;
                    case 16:
                        position = "??????";
                        break;
                    case 32:
                        position = "??????";
                        break;
                    case 64:
                        position = "??????";
                        break;
                    case 128:
                        position = "??????";
                        break;
                }
                //?????????
                NewVestView shooter=null;
                NewVestView shootee=null;
                for (NewVestView nvv:this.vestEntityList) {
                    if (Integer.parseInt(nvv.getId()) == Integer.parseInt(dataSplit[1])){
                        shooter=nvv;
                    }
                    if (Integer.parseInt(nvv.getId()) == Integer.parseInt(dataSplit[2])){
                        shootee=nvv;
                    }
                }
                this.vestEntityList.remove(shootee);
                if (shooter!=null && shootee!=null){
                    //??????????????????????????????
                    if (shootee.getHp() > 34) {
                        shootee.setHp(shootee.getHp()-33);
                        //???????????????
                        for (NewRecordEntity nre:this.newBattleService.getRecordData()) {
                            if (nre.getId() == shootee.getId()) {
                                Integer beShooted = nre.getBeShooted();
                                nre.setBeShooted(++beShooted);
                                //System.out.println("shootee"+nre.toString());
                            }
                            if (nre.getId()==shooter.getId()){
                                Integer shoot = nre.getShoot();
                                nre.setShoot(++shoot);
                                //System.out.println("shooter"+nre.toString());
                            }
                        }
                    } else if (shootee.getHp()<=34){
                        shootee.setHp(0);
                        //????????????
                        for (NewRecordEntity nre:this.newBattleService.getRecordData()) {
                            if (nre.getId() == shootee.getId()) {
                                Integer beShooted = nre.getBeShooted();
                                nre.setBeShooted(++beShooted);
                                //System.out.println("shootee"+nre.toString());
                            }
                            if (nre.getId()==shooter.getId()){
                                Integer shoot = nre.getShoot();
                                nre.setShoot(++shoot);
                                Integer kill = nre.getKill();
                                nre.setKill(++kill);
                            }
                        }
                    }
                    //System.out.println("??????list???"+JSONObject.toJSONString(this.newBattleService.getRecordData()));
                    //System.out.println(JSONObject.toJSONString();
                    Collections.sort(this.newBattleService.getRecordData());
                    this.refreshVest(shootee);
                    //????????????????????????????????????
                    logger.info("??????????????????"+shooter.getTeam()+"??????"+shooter.getId()+"?????????"+shootee.getTeam()+"??????"+shootee.getId()+"??????"+position+"??????");
                    //websocket????????????
                    ShootWebSocketModel shootWebSocketModel= ShootWebSocketModel.builder()
                            .mark("0").shooteeNum(shootee.getId()).shooteeTeam(shootee.getTeam())
                            .shooterTeam(shooter.getTeam()).shooterNum(shooter.getId())
                            .position(position).time(new Date())
                            .build();
                    //??????websocket??????????????????
                    shootWebSocket.sendMessage(shootWebSocketModel);
                    //shootWebSocket.sendMessage(JSON.toJSONString(shootWebSocketModel););
                    // } else {
                    //    log.info("????????????????????????????????????!");
                    // }
                }else {
                    logger.info("???mqtt??????????????????????????????????????????");
                }
            } else {
                logger.info("???mqtt???????????????????????????????????????????????????");
            }
        }
        //????????????0  ??????1 ??????lng2 ??????lat3 ???????????????4 ???????????????5  ??????6
        //    1     2       3        4         5        6           7
        if ("138".equals(dataSplit[0])) {
            if (dataSplit.length == 7) {
                NewVestView newVestView=null;
                for (NewVestView nvv:this.vestEntityList) {
                    if (Integer.parseInt(nvv.getId()) == Integer.parseInt(dataSplit[1])){
                        newVestView=nvv;
                    }
                }
                this.vestEntityList.remove(newVestView);
                if (newVestView != null) {
                    // log.info("111");
                    // vestEntity.builder()
                    //        .num(Integer.parseInt(dataSplit[2])).lng(Integer.parseInt(dataSplit[3]))
                    //        .lat(Integer.parseInt(dataSplit[4])).hp(Integer.parseInt(dataSplit[5]))
                    //       .ammo(Integer.parseInt(dataSplit[6])).lastReportTime(new Date())
                    //        .build();
                    //???????????????????????????????????????????????????websocket
                    Double lng=Double.parseDouble(dataSplit[2]);
                    Double lat=Double.parseDouble(dataSplit[3]);
                    // double[] doubles = GpsCoordinateUtils.calWGS84toGCJ02(lat, lng);
                    //lat=doubles[0];
                    //lng=doubles[1];
                    //System.out.println("????????????GCJ02???:??????"+lng+"??????"+lat);
                    if (newVestView.getLat()!=lat || newVestView.getLng()!= lng){
                        PositionWebSocketModel positionWebSocketModel= PositionWebSocketModel.builder()
                                .mark("1").num(newVestView.getId())
                                .lat(lat).lng(lng).time(new Date())
                                .build();
                        shootWebSocket.sendMessage(positionWebSocketModel);
                    }

                    //???????????????
                    int shootNum=Integer.parseInt(dataSplit[4])+Integer.parseInt(dataSplit[5]);
                    int hp;
                    if(shootNum<0 || shootNum>2){
                        hp=0;
                    }else {
                        hp = 100 - (33 * shootNum);
                    }

                    //??????vestEntityMap????????????
                    //newVestView.setId(Integer.parseInt(dataSplit[2]));
                    newVestView.setLng(lng);
                    newVestView.setLat(lat);
                    if (hp>=0){
                        newVestView.setHp(hp);
                    }else {
                        newVestView.setHp(0);
                    }
                    newVestView.setAmmo(Integer.parseInt(dataSplit[6]));
                    newVestView.setLastReportTime(new Date());
                    System.out.println(newVestView.toString());
                } else {
                    logger.info("???mqtt?????????" + dataSplit[1] + "??????????????????");
                }
                this.refreshVest(newVestView);
            } else {
                logger.info("???mqtt???????????????????????????????????????????????????");
            }
        }
        //
        if ("2".equals(dataSplit[0])){}
    }
}
