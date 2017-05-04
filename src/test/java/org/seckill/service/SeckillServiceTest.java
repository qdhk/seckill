package org.seckill.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.seckill.dto.Exposer;
import org.seckill.dto.SeckillExecution;
import org.seckill.entity.Seckill;
import org.seckill.exception.RepeatKillException;
import org.seckill.exception.SeckillCloseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

/**
 * Created by nr on 2017/05/02 0002.
 */
@RunWith(SpringJUnit4ClassRunner.class)
//告诉junit spring 配置文件
@ContextConfiguration({"classpath:spring/spring-dao.xml","classpath:spring/spring-service.xml"})
public class SeckillServiceTest {

    private final Logger logger= LoggerFactory.getLogger(this.getClass());

    @Autowired
    private SeckillService seckillService;

    @Test
    public void getSeckillList() throws Exception {
        List<Seckill> list=seckillService.getSeckillList();
        logger.info("list={}",list);
    }

    @Test
    public void getById() throws Exception {
        long id =1000;
        Seckill seckill=seckillService.getById(id);
        logger.info("seckill={}",seckill);
    }

//    @Test
//    public void exportSeckillUrl() throws Exception {
//        long id =1000;
//        Exposer exposer=seckillService.exportSeckillUrl(id);
//        logger.info("exposer={}",exposer);
//        //exposer=Exposer{exposed=true,
//        // md5='e31cbb9fa97ffb47412dbfe8a248990c',
//        // seckillId=1000, now=0, start=0, end=0}
//    }

//    @Test
//    public void executeSeckill() throws Exception {
//        long id =1000;
//        long phone =15230272836L;
//        String md5="e31cbb9fa97ffb47412dbfe8a248990c";
//        try{
//            SeckillExecution execution =seckillService.executeSeckill(id,phone,md5);
//            logger.info("result={}",execution);
//        }catch (RepeatKillException e){
//            logger.error(e.getMessage());
//        }catch (SeckillCloseException e){
//            logger.error(e.getMessage());
//        }
//        //result=SeckillExecution{seckillId=1000, state=1, stateInfo='秒杀成功', successKilled=SuccessKilled
//        // {seckillId=1000, userphone=15230272836, state=0, createTime=Tue May 02 18:16:26 CST 2017}
//    }

    //测试代码完整逻辑，注意可重复执行
    @Test
    public void SeckillLogic() throws Exception {
        long id =1001;
        Exposer exposer=seckillService.exportSeckillUrl(id);
        if(exposer.isExposed()){
            logger.info("exposer={}",exposer);
            long phone =15230272836L;
            String md5=exposer.getMd5();
            try{
                SeckillExecution execution =seckillService.executeSeckill(id,phone,md5);
                logger.info("result={}",execution);
            }catch (RepeatKillException e){
                logger.error(e.getMessage());
            }catch (SeckillCloseException e){
                logger.error(e.getMessage());
            }
        }else {
            //秒杀未开启
            logger.warn("exposer={}",exposer);
        }

    }

    @Test
    public void executeSeckillProcedure(){
        long seckillId=1000;
        long phone =15230573835L;
        Exposer exposer =seckillService.exportSeckillUrl(seckillId);
        if(exposer.isExposed()){
            String md5=exposer.getMd5();
            SeckillExecution execution=seckillService.executeSeckillProcedure(seckillId,phone,md5);
            logger.info(execution.getStateInfo());
        }
    }

}