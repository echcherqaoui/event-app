-- KEYS[1]: reserved, KEYS[2]: sentinel_key
-- ARGV[1]: quantity, ARGV[2]: sentinel_ttl

-- Already processed
if redis.call('EXISTS', KEYS[2]) == 1 then return 1 end

local globalReserved = tonumber(redis.call('GET', KEYS[1]) or '0')
local to_release = tonumber(ARGV[1])

-- Data inconsistency
if globalReserved < to_release then return -3  end

redis.call('DECRBY', KEYS[1], to_release)
redis.call('SET', KEYS[2], '1', 'EX', tonumber(ARGV[2]))
return 1