package org.seckill.exception;

/**
 * 秒杀关闭异常
 * Created by nr on 2017/05/02 0002.
 */
public class SeckillCloseException extends SeckillException{
    public SeckillCloseException(String message) {
        super(message);
    }

    public SeckillCloseException(String message, Throwable cause) {
        super(message, cause);
    }
}
