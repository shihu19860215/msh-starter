package com.msh.starter.id.generate.abstracts;


import com.msh.frame.interfaces.IdGenerateable;

/**
 * 注 :
 *
 * 根据系统自己设置 serviceIdBitCount，indexBitCount
 * serviceIdBitCount indexBitCount 加起来不要超过 28
 * serviceIdBitCount为服务id所占位数
 * 默认4位(0-15)
 * indexBitCount为每秒内生成最大数
 * 进过测试(100个线程生成1000万个id，时间所需855毫秒)
 * 进过测试(100个线程生成5000万个id，时间所需3856毫秒)
 * indexBitCount=14时速度最快
 * ---------------------------------------------------------------
 * 可生成多个对象，每个对象生成的id唯一
 * 但多个对象生成的id可能重复,
 * 生成id最后的indexBitCount 位数据中有自增效果，
 * 可在分库分表时，取模可以平均分配数据
 * 给需要分库分表的id 使用独立的id生成器对象
 * 保证id取模后的连续性
 */
public abstract class AbstractDefaultIdGenerate implements IdGenerateable {
    /**
     * 服务器id所占位数
     */
    private int serviceIdBitCount=4;
    /**
     * 每毫秒生成id数
     */
    private int indexBitCount=14;
    /**
     * 2018-01-01 00:00:00
     */
    private static long ID_BEGIN_TIME= 1514736000000L;

    /**
     * 服务器id
     */
    private static volatile Integer serviceId;
    /**
     * 服务器id位移后的值
     */
    private static volatile Long serviceIdBitShiftValue;
    /**
     * 最后一次index为0时的时间戳
     */
    private volatile long lastCurrentTimeMillis=0L;
    /**
     * 每次生成一个id自增1，当大于indexBit时置0，
     * 保证indexBitCount几个比特位一直处于自增状态
     * 可在分库，分表时独立生成该对象来获取id
     */
    private volatile int lastIndexBit=0;
    /**
     * 时间戳需要的位移位数
     */
    private int currentTimeMillisDiffBitCount;
    /**
     *  计数累计
     */
    private volatile int i=0;
    /**
     *  计数累计 最大值
     */
    private int indexBit;


    public AbstractDefaultIdGenerate() {
        this.currentTimeMillisDiffBitCount=serviceIdBitCount+indexBitCount;
        this.indexBit=(0x01<<indexBitCount)-1;
    }

    public AbstractDefaultIdGenerate(int serviceIdBitCount, int indexBitCount) {
        this.serviceIdBitCount = serviceIdBitCount;
        this.indexBitCount = indexBitCount;
        this.currentTimeMillisDiffBitCount=serviceIdBitCount+indexBitCount;
        this.indexBit=(0x01<<indexBitCount)-1;
    }

    @Override
    public synchronized long getUniqueID() {
        long now=System.currentTimeMillis();
        if(now!=lastCurrentTimeMillis){
            lastCurrentTimeMillis=now;
            i=0;
            long destID =  now- ID_BEGIN_TIME;
            destID = (destID << currentTimeMillisDiffBitCount) + lastIndexBit+ getServiceIdBitShiftValue() ;
            lastIndexBit++;
            if(lastIndexBit>indexBit){
                lastIndexBit=0;
            }
            return destID;
        }
        if(i++>indexBit){
            i=0;
            while (lastCurrentTimeMillis==now){
                now=System.currentTimeMillis();
            }
            lastCurrentTimeMillis=now;
        }
        long destID =  now- ID_BEGIN_TIME;
        destID = (destID << currentTimeMillisDiffBitCount) + lastIndexBit +getServiceIdBitShiftValue();
        lastIndexBit++;
        if(lastIndexBit>indexBit){
            lastIndexBit=0;
        }
        return destID;
    }

    private Long getServiceIdBitShiftValue(){
        if(null==serviceIdBitShiftValue){
            initServiceIdAndServiceIdBitShiftValue();
        }
        return serviceIdBitShiftValue;
    }

    private Integer initServiceIdAndServiceIdBitShiftValue(){
        if(serviceId==null){
            synchronized (this.getClass()){
                if(serviceId==null){
                    Integer id=getServerId();
                    if(null==id){
                        throw new RuntimeException("获取服务器Id失败");
                    }
                    int serverIdBitCalculate = (1 << (serviceIdBitCount+1)) -1 ;
                    serviceId = id & serverIdBitCalculate;
                    serviceIdBitShiftValue=new Long(serviceId << indexBitCount);
                }
            }
        }
        return serviceId;
    }


    /**
     * 获取服务唯一id
     * @return
     */
    abstract protected Integer getServerId();
}
