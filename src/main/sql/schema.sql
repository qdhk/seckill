--数据库初始化脚本
CREATE  DATABASE seckill;
--使用数据库
use seckill;
CREATE  TABLE seckill(
  seckill_id bigint NOT NULL  AUTO_INCREMENT,
  name VARCHAR (120) NOT NULL ,
  number int NOT NULL,
  start_time TIMESTAMP NOT NULL,
  end_time TIMESTAMP NOT NULL,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (seckill_id),
  KEY idx_start_time(start_time),
  KEY idx_end_time(end_time),
  KEY idx_create_time(create_time)
)ENGINE=InnoDB  AUTO_INCREMENT=1000 DEFAULT CHARSET=utf8 COMMENT='秒杀库存表';


--初始化数据
insert into
  seckill(name,number,start_time,end_time)
VALUES
  ('1000元秒杀iPhone6',100,'2015-11-01 00:00:00','2015-11-02 00:00:00'),
  ('1500元秒杀ipad2',200,'2015-11-01 00:00:00','2015-11-02 00:00:00'),
  ('300元秒杀米4',300,'2015-11-01 00:00:00','2015-11-02 00:00:00'),
  ('200元秒杀诺基亚',400,'2015-11-01 00:00:00','2015-11-02 00:00:00');

--秒杀成功明细表
--用户登录认证相关的信息
CREATE  TABLE success_killed(
   seckill_id bigint NOT NULL ,
   user_phone bigint NOT NULL ,
   state tinyint NOT NULL DEFAULT -1 ,
   create_time TIMESTAMP NOT NULL  COMMENT '创建时间',
   PRIMARY KEY (seckill_id,user_phone),
   KEY idx_create_time(create_time)
)ENGINE=InnoDB  DEFAULT CHARSET=utf8 COMMENT='秒杀成功明细表';

--连接数据库控制台
