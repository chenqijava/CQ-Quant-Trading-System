package com.nyy.gmail.cloud.utils;


import org.redisson.api.RedissonClient;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import jakarta.annotation.Resource;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;


@Component
public class RedisUtil {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    //=============================common============================
    /**
     * 读取lua脚本的方法
     * @param script 接口，创建的时候会使用实现类 DefaultRedisScript
     * @param keys lua脚本中key的参数
     * @param args lua脚本中非key的的参数
     * @param <T> 返回类型
     * @return
     */
    public <T> T execute(RedisScript<T> script, List<String> keys, Object... args) {
        return stringRedisTemplate.execute(script,keys,args);
    }
    /**
     * 指定缓存失效时间
     *
     * @param key  键
     * @param time 时间(秒)
     * @return
     */
    public boolean expire(String key, long time) {
        try {
            if (time > 0) {
                redisTemplate.expire(key, time, TimeUnit.SECONDS);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 指定缓存失效时间，指定时间类型
     *
     * @param key      键
     * @param time     时间
     * @param timeUnit 类型
     * @return
     */
    public boolean expire(String key, long time, TimeUnit timeUnit) {
        try {
            if (time > 0) {
                redisTemplate.expire(key, time, timeUnit);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 根据key 获取过期时间
     *
     * @param key 键 不能为null
     * @return 时间(秒) 返回0代表为永久有效
     */
    public long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    /**
     * 判断key是否存在
     *
     * @param key 键
     * @return true 存在 false不存在
     */
    public boolean hasKey(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 删除缓存
     *
     * @param key 可以传一个值 或多个
     */
    @SuppressWarnings("unchecked")
    public void del(String... key) {
        if (key != null && key.length > 0) {
            if (key.length == 1) {
                redisTemplate.delete(key[0]);
            } else {
                redisTemplate.delete((Collection<String>) CollectionUtils.arrayToList(key));
            }
        }
    }

    //============================String=============================

    /**
     * 普通缓存获取
     *
     * @param key 键
     * @return 值
     */
    public Object get(String key) {
        return key == null ? null : redisTemplate.opsForValue().get(key);
    }

    /**
     * 普通缓存放入
     *
     * @param key   键
     * @param value 值
     * @return true成功 false失败
     */
    public boolean set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

    /**
     * 普通缓存放入并设置时间
     *
     * @param key   键
     * @param value 值
     * @param time  时间(秒) time要大于0 如果time小于等于0 将设置无限期
     * @return true成功 false 失败
     */
    public boolean set(String key, Object value, long time) {
        try {
            if (time > 0) {
                redisTemplate.opsForValue().set(key, value, time, TimeUnit.SECONDS);
            } else {
                set(key, value);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 递增
     *
     * @param key 键
     * @return
     */
    public long incr(String key, long delta) {
        if (delta < 0) {
            throw new RuntimeException("递增因子必须大于0");
        }
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 递减
     *
     * @param key 键
     * @return
     */
    public long decr(String key, long delta) {
        if (delta < 0) {
            throw new RuntimeException("递减因子必须大于0");
        }
        return redisTemplate.opsForValue().increment(key, -delta);
    }

    /**
     * 设置一个String类型的key，当且仅当key不存在时设置成功。
     * @param key 缓存key
     * @param value 缓存值
     * @param time 过期时间 小于等于0表示永久有效
     * @param unit 过期单位
     * @return 设置成功返回true；失败返回false
     */
    public boolean setNx(String key,Object value,long time, TimeUnit unit){
        try {
            if (time==-1) {
                unit=null;
            }
            Boolean flag = redisTemplate.opsForValue().setIfAbsent(key, value,time,unit);
            return Boolean.TRUE.equals(flag);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }



    public boolean getLock(String key,Object value,long time, TimeUnit unit){
        return this.setNx(key,value,time,unit);
    }

    public void unLock(String key){
        this.del(key);
    }


    //================================hash(Map)=================================

    /**
     * HashGet
     *
     * @param key  键 不能为null
     * @param item 项 不能为null
     * @return 值
     */
    public Object hget(String key, String item) {
        return redisTemplate.opsForHash().get(key, item);
    }

    /**
     * 获取hashKey对应的所有键值
     *
     * @param key 键
     * @return 对应的多个键值
     */
    public Map<Object, Object> hmget(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * HashSet
     *
     * @param key 键
     * @param map 对应多个键值
     * @return true 成功 false 失败
     */
    public boolean hmset(String key, Map<String, Object> map) {
        try {
            redisTemplate.opsForHash().putAll(key, map);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * HashSet 并设置时间
     *
     * @param key      键
     * @param map      对应多个键值
     * @param time     时间
     * @param timeUnit 时间单位
     * @return true成功 false失败
     */
    public boolean hmset(String key, Map<String, Object> map, long time, TimeUnit timeUnit) {
        try {
            redisTemplate.opsForHash().putAll(key, map);
            if (time > 0) {
                expire(key, time, timeUnit);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 向一张hash表中放入数据,如果不存在将创建
     *
     * @param key   键
     * @param item  项
     * @param value 值
     * @return true 成功 false失败
     */
    public boolean hset(String key, String item, Object value) {
        try {
            redisTemplate.opsForHash().put(key, item, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 向一张hash表中放入数据,如果不存在将创建
     *
     * @param key   键
     * @param item  项
     * @param value 值
     * @param time  时间(秒)  注意:如果已存在的hash表有时间,这里将会替换原有的时间
     * @return true 成功 false失败
     */
    public boolean hset(String key, String item, Object value, long time) {
        try {
            redisTemplate.opsForHash().put(key, item, value);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 删除hash表中的值
     *
     * @param key  键 不能为null
     * @param item 项 可以使多个 不能为null
     */
    public void hdel(String key, Object... item) {
        redisTemplate.opsForHash().delete(key, item);
    }

    /**
     * 判断hash表中是否有该项的值
     *
     * @param key  键 不能为null
     * @param item 项 不能为null
     * @return true 存在 false不存在
     */
    public boolean hHasKey(String key, String item) {
        return redisTemplate.opsForHash().hasKey(key, item);
    }

    /**
     * hash递增 如果不存在,就会创建一个 并把新增后的值返回
     *
     * @param key  键
     * @param item 项
     * @param by   要增加几(大于0)
     * @return
     */
    public double hincr(String key, String item, double by) {
        return redisTemplate.opsForHash().increment(key, item, by);
    }

    /**
     * hash递减
     *
     * @param key  键
     * @param item 项
     * @param by   要减少记(小于0)
     * @return
     */
    public double hdecr(String key, String item, double by) {
        return redisTemplate.opsForHash().increment(key, item, -by);
    }

    //============================set=============================

    /**
     * 根据key获取Set中的所有值
     *
     * @param key 键
     * @return
     */
    public Set<Object> sGet(String key) {
        try {
            return redisTemplate.opsForSet().members(key);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 根据value从一个set中查询,是否存在
     *
     * @param key   键
     * @param value 值
     * @return true 存在 false不存在
     */
    public boolean sIsMemberKey(String key, Object value) {
        try {
            Boolean flag = redisTemplate.opsForSet().isMember(key, value);
            return Boolean.TRUE.equals(flag);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 将数据放入set缓存
     *
     * @param key    键
     * @param values 值 可以是多个
     * @return 成功个数
     */
    public long sAdd(String key, Object... values) {
        try {
            return redisTemplate.opsForSet().add(key, values);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 将set数据放入缓存
     *
     * @param key    键
     * @param time   时间(秒)
     * @param values 值 可以是多个
     * @return 成功个数
     */
    public long sSetAndTime(String key, long time, Object... values) {
        try {
            Long count = redisTemplate.opsForSet().add(key, values);
            if (time > 0) expire(key, time);
            return count;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 获取set缓存的长度
     *
     * @param key 键
     * @return
     */
    public long sGetSetSize(String key) {
        try {
            return redisTemplate.opsForSet().size(key);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 移除set集合值为value的
     *
     * @param key    键
     * @param values 值 可以是多个
     * @return 移除的个数
     */
    public long setRemove(String key, Object... values) {
        try {
            Long count = redisTemplate.opsForSet().remove(key, values);
            return count;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 返回多个集合的并集  sunion
     *
     * @param listKeys
     * @return
     */
    public Set<Object> sUnion(List<String> listKeys) {
        try {
            return redisTemplate.opsForSet().union(listKeys);
        } catch (Exception e) {
            e.printStackTrace();
            return  null;
        }
    }

    /**
     * 返回多个集合的交集 sinter
     *
     * @param listKeys
     * @return
     */
    public Set<Object> sIntersect(List<String> listKeys) {
        try {
            return redisTemplate.opsForSet().intersect(listKeys);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 差集：返回集合key1中存在，但是key2中不存在的数据集合  sdiff
     *
     * @param listKeys
     * @return
     */
    public Set<Object> sDiff(List<String> listKeys) {
        try {
            return redisTemplate.opsForSet().difference(listKeys);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    //============================Zset=============================
    /**
     * 添加一个元素, zset与set最大的区别就是每个元素都有一个score，因此有个排序的辅助功能;  zadd
     *
     * @param key 键
     * @param value 值
     * @param score 分数
     */
    public boolean zSetAdd(String key, String value, double score) {
        try {
            Boolean aBoolean = stringRedisTemplate.opsForZSet().add(key, value, score);
            return Boolean.TRUE.equals(aBoolean);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 移除一个zset有序集合的key的一个或者多个值
     * zrem key member [member ...] ：移除有序集 key 中的一个或多个成员，不存在的成员将被忽略。当 key 存在但不是有序集类型时，返回一个错误。
     * @param key 集合的键key
     * @param values 需要移除的value
     * @return
     */
    public boolean zRem(String key, Object... values) {
        try {
            Long aLong = redisTemplate.opsForZSet().remove(key, values);
            return aLong!=null?true:false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    /**
     * score的增加or减少 zincrby
     *      通过 zincrby 来对score进行加/减；当元素不存在时，则会新插入一个
     *      从上面的描述来看，zincrby 与 zadd 最大的区别是前者是增量修改；后者是覆盖score方式
     * @param key 键
     * @param value 值
     * @param score 分数
     */
    public Double incrScore(String key, String value, double score) {
        try {
            return stringRedisTemplate.opsForZSet().incrementScore(key, value, score);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    /**
     * 查询value对应的score   zscore
     * 返回有序集zSet中key有成员 member 的 score 值。如果 member 元素不是有序集 key 的成员，或 key 不存在，返回 nil 。
     * @param key 键
     * @param value 值
     * @return 分数
     */
    public Double score(String key, String value) {
        try {
            return stringRedisTemplate.opsForZSet().score(key, value);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    /**
     * 返回set集合的长度
     *
     * @param key
     * @return
     */
    public Long zSize(String key) {
        try {
            return stringRedisTemplate.opsForZSet().zCard(key);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    /**
     * 判断value在zset中的排名  zrank命令
     *
     * @param key 键
     * @param value 值
     * @return score 越小排名越高;
     */
    public Long zRank(String key, String value) {
        try {
            return stringRedisTemplate.opsForZSet().rank(key, value);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    /**
     * 查询zSet集合中指定顺序的值， 0 -1 表示获取全部的集合内容  zrange
     *
     * @param key 键
     * @param start 开始
     * @param end 结束
     * @return 返回有序的集合，score小的在前面
     */
    public Set<String> zRange(String key, int start, int end) {
        try {
            return stringRedisTemplate.opsForZSet().range(key, start, end);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 查询zSet集合中指定顺序的值和score，0, -1 表示获取全部的集合内容
     *
     * @param key 键
     * @param start 开始
     * @param end 结束
     * @return
     */
    public Set<ZSetOperations.TypedTuple<String>> zRangeWithScore(String key, int start, int end) {
        try {
            return stringRedisTemplate.opsForZSet().rangeWithScores(key, start, end);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 查询zSet集合中指定顺序的值  zrevrange
     *
     * @param key 键
     * @param start 开始
     * @param end 结束
     * @return 返回有序的集合中，score大的在前面
     */
    public Set<String> revRange(String key, int start, int end) {
        try {
            return stringRedisTemplate.opsForZSet().reverseRange(key, start, end);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 根据score的值，来获取满足条件的zSet集合  zrangebyscore key min max [WITHSCORES] [LIMIT offset count]
     *  min 和 max 可以是 -inf 和 +inf ，这样一来，你就可以在不知道有序集的最低和最高 score 值的情况下
     * @param key key
     * @param min score最小值（包含）
     * @param max score最大值（包含）
     * @param offset 偏移量
     * @param count count查询的个数
     * @return 返回满足结果的结合（按照分数有小到大）
     */
    public Set<String> zSortRangeByScore(String key, double min, double max,long offset,long count) {
        try {
            return stringRedisTemplate.opsForZSet().rangeByScore(key, min, max,offset,count);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    /**
     * 根据score的值，来获取满足条件的zSet集合  zrevrangebyscore key min max [WITHSCORES] [LIMIT offset count]
     *  min 和 max 可以是 -inf 和 +inf ，这样一来，你就可以在不知道有序集的最低和最高 score 值的情况下
     * @param key key
     * @param min score最小值（包含）
     * @param max score最大值（包含）
     * @param offset 偏移量
     * @param count count查询的个数
     * @return 返回满足结果的结合（按照分数有大到小），value值，不包含score值
     */
    public Set<String> zSortRevRangeByScore(String key, double min, double max,long offset,long count) {
        try {
            return stringRedisTemplate.opsForZSet().reverseRangeByScore(key, min, max,offset,count);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    /**
     * 根据score的值，来获取满足条件的zSet集合  zrevrangebyscore key min max [WITHSCORES] [LIMIT offset count]
     *  min 和 max 可以是 -inf 和 +inf ，这样一来，你就可以在不知道有序集的最低和最高 score 值的情况下
     * @param key key
     * @param min score最小值（包含）
     * @param max score最大值（包含）
     * @param offset 偏移量
     * @param count count查询的个数
     * @return 返回满足结果的结合（按照分数有大到小），value值，包含score值
     */
    public Set<ZSetOperations.TypedTuple<String>>  zSortRevRangeByScoreWithScores(String key, double min, double max,long offset,long count) {
        try {
            return stringRedisTemplate.opsForZSet().reverseRangeByScoreWithScores(key, min, max,offset,count);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    //===============================list=================================

    /**
     * 获取list缓存的内容
     *
     * @param key   键
     * @param start 开始
     * @param end   结束  0 到 -1代表所有值
     * @return
     */
    public List<Object> lGet(String key, long start, long end) {
        try {
            return redisTemplate.opsForList().range(key, start, end);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取list缓存的长度
     *
     * @param key 键
     * @return
     */
    public long lGetListSize(String key) {
        try {
            return redisTemplate.opsForList().size(key);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 通过索引 获取list中的值
     *
     * @param key   键
     * @param index 索引  index>=0时， 0 表头，1 第二个元素，依次类推；index<0时，-1，表尾，-2倒数第二个元素，依次类推
     * @return
     */
    public Object lGetIndex(String key, long index) {
        try {
            return redisTemplate.opsForList().index(key, index);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 将list放入缓存
     *
     * @param key   键
     * @param value 值
     * @return
     */
    public boolean lSet(String key, Object value) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 将list放入缓存
     *
     * @param key   键
     * @param value 值
     * @param time  时间(秒)
     * @return
     */
    public boolean lSet(String key, Object value, long time) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
            if (time > 0) expire(key, time);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 将list放入缓存
     *
     * @param key   键
     * @param value 值
     * @return
     */
    public boolean lSetAll(String key, List<?> value) {
        try {
            redisTemplate.opsForList().rightPushAll(key, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 将list放入缓存
     *
     * @param key   键
     * @param value 值
     * @param time  时间(秒)
     * @return
     */
    public boolean lSet(String key, List<Object> value, long time) {
        try {
            redisTemplate.opsForList().rightPushAll(key, value);
            if (time > 0) expire(key, time);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 根据索引修改list中的某条数据
     *
     * @param key   键
     * @param index 索引
     * @param value 值
     * @return
     */
    public boolean lUpdateIndex(String key, long index, Object value) {
        try {
            redisTemplate.opsForList().set(key, index, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 移除N个值为value
     *
     * @param key   键
     * @param count 移除多少个
     * @param value 值
     * @return 移除的个数
     */
    public long lRemove(String key, long count, Object value) {
        try {
            Long remove = redisTemplate.opsForList().remove(key, count, value);
            return remove;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    //============================Geo地理位置的相关操作=============================
    /**
     * @Title: 添加元素,一个一个添加
     * @param key key
     * @param point 经纬度
     * @param member 成员
     * @return Long 返回影响的行
     */
    public Long geoAdd(String key, Point point, String member) {
        return stringRedisTemplate.opsForGeo().add(key, point, member);
    }

    /**
     * @Title: 添加元素,批量添加
     * @param key key
     * @param locations 地理位置的一个集合
     * @return Long 返回影响的行
     */
    public Long geoAddGeoLocation(String key,List<RedisGeoCommands.GeoLocation<Object>> locations) {
        return redisTemplate.opsForGeo().add(key, locations);
    }

    /**
     * @Title: 删除元素/成员
     * @param key
     * @param members 成员
     * @return Long 返回影响的行
     */
    public Long geoRemove(String key, String... members) {
        return redisTemplate.opsForGeo().remove(key, (Object[])members);
    }

    /**
     * @Title: 查询成员的经纬度
     * @param key key
     * @param members 成员
     * @return List<Point>
     */
    public List<Point> geoPos(String key, String... members) {
        return redisTemplate.opsForGeo().position(key, (Object[]) members);
    }

    /**
     * @Title: 查询成员的经纬度hash值
     * @param key
     * @param members
     * @return List<String>
     */
    public List<String> geoHash(String key, String... members) {
        return stringRedisTemplate.opsForGeo().hash(key, members);
    }

    /**
     * @Title: 查询2个成员之间的距离
     * @param key key
     * @param member1 成员1
     * @param member2 成员2
     * @param metric 单位
     * @return Double 距离
     */
    public Double geoDist(String key, String member1, String member2, Metric metric) {
        Distance distance = redisTemplate.opsForGeo().distance(key, member1, member2, metric);
        return distance != null ? distance.getValue() : null;
    }


    //============================bitMap位图的相关操作（常用的）=============================
    /**
     * 设置key字段第offset位bit数值
     *
     * @param key    字段
     * @param offset 位置
     * @param value  数值
     */
    public void setBit(String key, long offset, boolean value) {
        try {
            Boolean aBoolean = stringRedisTemplate.opsForValue().setBit(key, offset, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取key的所有记录（unsigned不带符号的，即首位数字也表示值）
     * 有点类似于String类型的substring道理
     * @param key 键
     * @param bits 获取的位数
     * @param offset 从那个位置开始
     * @return 返回的是一个十进制的集合，这儿只用了一个get命令所以只有一个数据，所以就不用集合了
     */
    public Long bitField(String key, int bits, long offset){
        try {
            List<Long> longs = stringRedisTemplate.opsForValue().bitField(key,
                    BitFieldSubCommands.create().
                            get(BitFieldSubCommands.BitFieldType.unsigned(bits)).valueAt(offset));
            if (longs==null || longs.isEmpty()) {
                return null;
            }
            return longs.get(0);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    /**
     * 判断该key字段offset位否为1
     *
     * @param key    字段
     * @param offset 位置
     * @return 结果
     */
    public boolean getBit(String key, long offset) {
        try {
            Boolean aBoolean = stringRedisTemplate.opsForValue().getBit(key, offset);
            return Boolean.TRUE.equals(aBoolean);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     *  统计key字段value为1的总数
     * @param key 键
     * @return
     */
    public Long bitCountAll(String key) {
        try {
            return (Long) stringRedisTemplate.execute((RedisCallback) con -> con.bitCount(key.getBytes()));
        } catch (Exception e) {
            e.printStackTrace();
            return  null;
        }
    }

    /**
     * 统计key字段value为1的总数,从start开始到end结束
     * 注意搜索的是字节，1个字节8个bit位，所以0，1表示搜索的是，前面2个字节，共16个bit位。
     * 换言之，0，1是前面16天的统计签到次数（1的个数）
     * @param key   字段
     * @param start 起始
     * @param end   结束
     * @return 总数
     */
    public Long bitCount(String key, Long start, Long end) {
        try {
            return (Long) stringRedisTemplate.execute((RedisCallback) con -> con.bitCount(key.getBytes(), start, end));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    //============================Stream-group消费者组（消息队列）=============================

    /**
     * 读取消费者组的信息
     * @param groupName 消费者组名称
     * @param consumerName 消费者名称
     * @param count 每次读取的数量
     * @param streamsKey 消息队列的key
     * @param readOffset 读取方式
     * @link 命令：XREADGROUP GROUP group consumer [COUNT count] [BLOCK milliseconds] [NOACK] STREAMS key [key ...] ID [ID ...]
     * @since redis 5.0+
     * @return 返回读取消息的数量list集合
     */
    public List<MapRecord<String, Object, Object>> xReadGroup(String groupName,String consumerName,
                                                              int count,long seconds,String streamsKey,
                                                              ReadOffset readOffset){
        //判断读取的数量,小于了1就表示读取数量为0，毫无意义
        if (0 == count) {
            return null;
        }
        StreamReadOptions block = StreamReadOptions.empty().count(count).block(Duration.ofSeconds(seconds));
        //判断是否需要阻塞读取
        if (seconds==0) {
            block=StreamReadOptions.empty().count(count);
        }
        List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().
                read(Consumer.from(groupName, consumerName),block,StreamOffset.create(streamsKey, readOffset));
        return list;
    }

    /**
     * 确认读取消息组的数量
     * @param streamsKey
     * @param groupName
     * @param recordIds
     * @line  命令:XACK key group ID [ID ...]
     * @since redis 5.0+
     * @return 读取的数量
     */
    public long xAck(String streamsKey, String groupName, String... recordIds){
        Long ackNums = stringRedisTemplate.opsForStream().acknowledge(streamsKey, groupName, recordIds);
        if (null==ackNums) {
            return 0;
        }
        return ackNums;
    }

    public Set<String> keys(String s) {
        Set<String> keys = stringRedisTemplate.keys(s);
        return keys;
    }

    public String getGoogleAuthUrl(String userID) {
        String val = stringRedisTemplate.opsForValue().get("GoogleAuthUrl:" + userID);
        return val;
    }

    public void setGoogleAuthUrl (String userID, String secret) {
        stringRedisTemplate.opsForValue().set("GoogleAuthUrl:" + userID, secret, 30, TimeUnit.MINUTES);
    }

    public String getUser2fa(String biz) {
        String userID = stringRedisTemplate.opsForValue().get("User2fa:" + biz);
        return userID;
    }

    public void setUser2FA(String userID, String biz) {
        stringRedisTemplate.opsForValue().set("User2fa:" + biz, userID, 5, TimeUnit.MINUTES);
    }
}




