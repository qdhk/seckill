package org.seckill.exception;

/**
 * 秒杀相关业务异常
 * Created by nr on 2017/05/02 0002.
 */
public class SeckillException extends RuntimeException{
    public SeckillException(String message) {
        super(message);
    }

    public SeckillException(String message, Throwable cause) {
        super(message, cause);
    }
}
