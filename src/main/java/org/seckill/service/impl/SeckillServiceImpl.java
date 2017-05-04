package org.seckill.service.impl;

import org.apache.commons.collections.MapUtils;
import org.seckill.dao.SeckillDao;
import org.seckill.dao.SuccessKilledDao;
import org.seckill.dao.cache.RedisDao;
import org.seckill.dto.Exposer;
import org.seckill.dto.SeckillExecution;
import org.seckill.entity.Seckill;
import org.seckill.entity.SuccessKilled;
import org.seckill.enums.SeckillStatEnum;
import org.seckill.exception.RepeatKillException;
import org.seckill.exception.SeckillCloseException;
import org.seckill.exception.SeckillException;
import org.seckill.service.SeckillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by nr on 2017/05/02 0002.
 */
@Service
public class SeckillServiceImpl implements SeckillService {
    private Logger logger= LoggerFactory.getLogger(this.getClass());

    //注入service依赖
    @Autowired //@Resource @Inject
    private SeckillDao seckillDao;

    @Autowired
    private SuccessKilledDao successKilledDao;
    @Autowired
    private RedisDao redisDao;

    //md5盐值字符串，
    private final String slat="sefxcv*^T%#$#!#WW";

    public List<Seckill> getSeckillList() {
        return seckillDao.queryAll(0,4);
    }

    public Seckill getById(long seckillId) {
        return seckillDao.queryById(seckillId);
    }

    public Exposer exportSeckillUrl(long seckillId) {
        //优化点：缓存优化,：超时的基础上维护一致性，seckill对象一般是不变的，变了就废弃掉
        /**
         * get from cache
         * if null
         * get db
         * else
         *  put cache
         *
         */
        //1.访问redis
        Seckill seckill=redisDao.getSeckill(seckillId);
        if(seckill == null){
            //2.访问数据库
            seckill = seckillDao.queryById(seckillId);
            if(seckill==null){
                return new Exposer(false,seckillId);
            }else {
                //3.放入redis
                redisDao.putSeckill(seckill);
            }
        }

        Date startTime=seckill.getStartTime();
        Date endTime=seckill.getEndTime();
        Date nowTime=new Date();
        if(nowTime.getTime() < startTime.getTime()
                || nowTime.getTime() > endTime.getTime()){
            return new Exposer(false,seckillId,nowTime.getTime(),startTime.getTime(),endTime.getTime());
        }
        //转化特定字符串的过程，不可逆
        String md5=getMD5(seckillId);
        return new Exposer(true,md5,seckillId);
    }

    private String getMD5(long seckillId){
        String base=seckillId + "/" + slat;
        String md5= DigestUtils.md5DigestAsHex(base.getBytes());
        return md5;
    }

    /**
     *
     * 使用注解控制事务方法的有点：
     * 1：开放团队达成一致约定，明确标注事务方法的编程风格。
     * 2：保证事务方法的执行时间尽可能短，不要穿插其他的网络操作，RPC/HTTP请求
     *        或者剥离到请求事务方法外部
     * 3：不是所有的方法都需要事务，如只有一条修改操作，只读操作不需要事务控制
     */
    @Transactional
    public SeckillExecution executeSeckill(long seckillId, long userPhone, String md5) throws SeckillException, SeckillCloseException, RepeatKillException {
       if(md5==null || !md5.equals(getMD5(seckillId))){
           throw new SeckillException("seckill data rewrite");
       }
       //执行秒杀逻辑：减库存 + 记录购买行为
        Date nowTime = new Date();
        //优化点，先insert（可并行），再减库存
       try{
           //记录购买行为
           int insertCount = successKilledDao.insertSuccessKilled(seckillId,userPhone);
           //唯一
           if(insertCount <= 0){
               //重复秒杀
               throw new RepeatKillException("seckill repeated");
           }else {
               //减库存,热点商品竞争
               int updateCount = seckillDao.reduceNumber(seckillId,nowTime);
               if(updateCount <= 0){
                   //没有更新记录,秒杀结束，rollback
                   throw new SeckillCloseException("seckill is closed");
               }else{
                   //秒杀成功,commit
                   SuccessKilled successKilled = successKilledDao.queryByIdWithSeckill(seckillId,userPhone);
                   return new SeckillExecution(seckillId, SeckillStatEnum.SUCCESS,successKilled);
               }
           }
       }catch (SeckillCloseException e1){
           throw e1;
       }catch (RepeatKillException e2){
           throw e2;
       }catch (Exception e){
           logger.error(e.getMessage(),e);
           //所有编译器异常，转化为运行期异常
           throw new SeckillException("seckill inner error"+ e.getMessage());
       }
    }

    public SeckillExecution executeSeckillProcedure(long seckillId, long userPhone, String md5) {
        if(md5==null || !md5.equals(getMD5(seckillId))){
            return new SeckillExecution(seckillId,SeckillStatEnum.DATA_REWRITE);
        }
        Date killTime=new Date();
        Map<String,Object> map=new  HashMap<String,Object>();
        map.put("seckillId",seckillId);
        map.put("phone",userPhone);
        map.put("killTime",killTime);
        map.put("result",null);
        //执行存储过程
        try {
            seckillDao.killByProcedure(map);
            //获取result
            int result=MapUtils.getInteger(map,"result",-2);
            if(result==1){
                SuccessKilled sk=successKilledDao.queryByIdWithSeckill(seckillId,userPhone);
                return new SeckillExecution(seckillId,SeckillStatEnum.SUCCESS);
            }else {
                return new SeckillExecution(seckillId,SeckillStatEnum.stateof(result));
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new SeckillExecution(seckillId, SeckillStatEnum.INNER_ERROR);
        }
    }
}
