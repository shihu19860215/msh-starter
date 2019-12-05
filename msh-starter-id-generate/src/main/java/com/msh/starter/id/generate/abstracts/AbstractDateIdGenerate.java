package com.msh.starter.id.generate.abstracts;

import com.msh.frame.common.util.DateUtil;
import com.msh.frame.interfaces.IdGenerateable;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;


/**
 * LONG类型正数最大个数19个
 * 生成带有年月日开头的id
 * 日期6位,当天秒数5位，服务器id 1位 自增数字7位
 * 进过测试(100个线程生成5000万个id，时间所需5033毫秒)
 * ---------------------------------------------------------------
 * 可生成多个对象，每个对象生成的id唯一
 * 但多个对象生成的id可能重复,
 * 生成id最后的自增数字 位数数据中有自增效果，
 * 可在分库分表时，取模可以平均分配数据
 * 给需要分库分表的id 使用独立的id生成器对象
 * 保证id取余后的连续性
 * -----------------------------------------------------------------
 * 如果有可能出现几个小时都没有新数据的情况
 * 构造函数updateLastPassDaySecond设置为true
 * 启动个线程定时更新最后一次经过秒数
 *
 */
public abstract class AbstractDateIdGenerate implements IdGenerateable {
    final private static Object LOCK_OBJECT = new Object();
    /**
     * 一天毫秒数共占多少个数字位
     * 一天 86400 秒
     * 共占8位
     */
    final private int oneDayNumberMultiple = 100000;
    /**
     * 服务器id所占数字位数
     */
    private int serviceIdNumberMultiple;
    /**
     * 每毫秒生成id 位数
     */
    private int indexNumberMultiple;

    /**
     * 服务器id
     */
    private static volatile Integer serviceId;

    /**
     * 服务器id位移后的值
     */
    private static volatile Long serviceIdBitShiftValue;

    /**
     * 最后一次index为0时的当天经过秒数
     */
    private volatile long lastPassDaySecond=0L;

    /**
     * 每次生成一个id自增1，当大于iMax时置0，
     * 保证lastIndex几个数据位一直处于自增状态
     * 可在分库，分表时独立生成该对象来获取id
     */
    private volatile int lastIndex=0;

    /**
     *  计数累计
     */
    private volatile int i=0;

    /**
     *  计数累计 最大值
     */
    private volatile int iMax;

    /**
     * 日期开头的long值
     * 后面有dateBackNumberCount位数字
     */
    private volatile long dateFront;


    /**
     * 两个参数相乘不能大于100000000
     * @param indexNumberMultiple 每毫秒生成id 位数 10的倍数
     * @param serviceIdNumberMultiple 服务器id所占数字位数 10的倍数
     */
    public AbstractDateIdGenerate(int indexNumberMultiple, int serviceIdNumberMultiple ) {
        this.serviceIdNumberMultiple = serviceIdNumberMultiple;
        this.indexNumberMultiple = indexNumberMultiple;
        iMax = indexNumberMultiple - 1;
        Long dayLong = calculateDateFront();
        if(null == dayLong){
            throw new RuntimeException("生成日期头出错");
        }
        dateFront = dayLong;
        initServiceIdAndServiceIdBitShiftValue();
        updateLastPassDaySecondThread();
    }

    public AbstractDateIdGenerate() {
        this(10000000,10);
    }

    @Override
    public long getUniqueID() {
        synchronized (LOCK_OBJECT){
            long now= DateUtil.getPassDayMilliSecond()/1000;
            if(now != lastPassDaySecond){
                //如果当前经过时间 大于 最后一次获取的经过时间，说明第二天开始，重新获取日期开头
                if(now < lastPassDaySecond){
                    while(true) {
                        Long dayLong = calculateDateFront();
                        if (null != dayLong
                                && dayLong != dateFront) {
                            dateFront = dayLong;
                            break;
                        }
                    }
                }
                lastPassDaySecond = now;
                i=0;
                long destID = dateFront+ lastPassDaySecond *serviceIdNumberMultiple*indexNumberMultiple + serviceIdBitShiftValue  +lastIndex;
                lastIndex++;
                if(lastIndex > iMax){
                    lastIndex=0;
                }
                return destID;

            }
            if(i++>iMax){
                i=0;
                while (lastPassDaySecond == now){
                    now=DateUtil.getPassDayMilliSecond()/1000;
                }
                //如果当前经过时间 大于 最后一次获取的经过时间，说明第二天开始，重新获取日期开头
                if(now < lastPassDaySecond){
                    while(true) {
                        Long dayLong = calculateDateFront();
                        if (null != dayLong
                                && dayLong != dateFront) {
                            dateFront = dayLong;
                            break;
                        }
                    }
                }
                lastPassDaySecond=now;
            }
            long destID = dateFront+ lastPassDaySecond *serviceIdNumberMultiple*indexNumberMultiple + serviceIdBitShiftValue  +lastIndex;
            lastIndex++;
            if(lastIndex>iMax){
                lastIndex=0;
            }
            return destID;
        }
    }


    private Integer initServiceIdAndServiceIdBitShiftValue(){
        if(serviceId==null){
            synchronized (this.getClass()){
                if(serviceId==null){
                    Integer id=getServerId();
                    if(null==id){
                        throw new RuntimeException("获取服务器Id失败");
                    }
                    int serverIdBitCalculate = serviceIdNumberMultiple -1 ;
                    serviceId = id % serverIdBitCalculate;
                    serviceIdBitShiftValue=new Long(serviceId * indexNumberMultiple);
                }
            }
        }
        return serviceId;
    }


    /**
     * 获取服务服务唯一id
     * @return
     */
    abstract protected Integer getServerId();

    private Long calculateDateFront(){
        Long dayLong = null;
        try {
            DateFormat df =new SimpleDateFormat("yyMMdd");
            String day = df.format(new Date());
            dayLong = Long.valueOf(day) * oneDayNumberMultiple * serviceIdNumberMultiple * indexNumberMultiple;
        }catch (Exception e){

        }
        return  dayLong;
    }


    /**
     * 启动线程
     * 更新最后一次当天经过秒数线程
     * 如果一直没有更新，上一次的当天经过时间与这次当天经过时间间隔到一定范围，会出现日期头不更新
     * 例:
     * 第 M 次生成id
     * 190512 lastPassDaySecond 3600 190512日期在经过了3600秒后生成一次id
     * 第 M+1 次生成id
     * 190513 在超过4000秒 190513日期在经过了4000秒后生成一次id
     * 生成id 时, now > lastPassDaySecond ,不会更新日期头  则生成的id还会以190512开头
     */
    private void updateLastPassDaySecondThread(){
        new Thread(() -> {
            while(true){
                try {
                    TimeUnit.HOURS.sleep(1);
                    synchronized (LOCK_OBJECT){
                        long now= DateUtil.getPassDayMilliSecond()/1000;
                        if(now != lastPassDaySecond){
                            //如果当前经过时间 大于 最后一次获取的经过时间，说明第二天开始，重新获取日期开头
                            if(now < lastPassDaySecond){
                                while(true) {
                                    Long dayLong = calculateDateFront();
                                    if (null != dayLong
                                            && dayLong != dateFront) {
                                        dateFront = dayLong;
                                        break;
                                    }
                                }
                            }
                            lastPassDaySecond = now;
                        }
                    }
                }catch (Exception e){
                }
            }
        });
    }
}
