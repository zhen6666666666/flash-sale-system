-- KEYS[1]: 秒杀商品库存 Key (seckill:stock:{goodsId})
-- KEYS[2]: 用户已购买记录 Key (seckill:users:{goodsId})
-- ARGV[1]: 用户 ID
-- ARGV[2]: 每人限购数量

-- 1. 检查用户是否达到限购数量
local userBuyCount = redis.call('HEXISTS', KEYS[2], ARGV[1])
if userBuyCount == 1 then
    local count = tonumber(redis.call('HGET', KEYS[2], ARGV[1]))
    if count >= tonumber(ARGV[2]) then
        -- 超过限购数量，返回 -1
        return -1
    end
end

-- 2. 检查库存是否足够
local stock = tonumber(redis.call('GET', KEYS[1]))
if (not stock) or (stock <= 0) then
    -- 库存不足，返回 -2
    return -2
end

-- 3. 执行扣减库存
redis.call('DECR', KEYS[1])

-- 4. 记录/更新用户购买数量
redis.call('HINCRBY', KEYS[2], ARGV[1], 1)

-- 执行成功，返回 1
return 1