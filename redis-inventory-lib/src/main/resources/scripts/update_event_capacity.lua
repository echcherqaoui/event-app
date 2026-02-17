-- KEYS[1]: capacity, KEYS[2]: sold, KEYS[3]: reserved, KEYS[4]=status
-- ARGV[1]: blockStatus, ARGV[2]: newCapacity,

if redis.call('EXISTS', KEYS[1]) == 0 then return -5 end

local status = redis.call('GET', KEYS[4])

if status == ARGV[1] then return -7 end

local newCapacity = tonumber(ARGV[2])
local sold = tonumber(redis.call('GET', KEYS[2]) or "0")
local reserved = tonumber(redis.call('GET', KEYS[3]) or "0")

if newCapacity < (sold + reserved) then return -2 end

redis.call('SET', KEYS[1], newCapacity)
return 1