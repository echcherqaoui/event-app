-- KEYS[1]: reserved, KEYS[2]: lock, KEYS[3]: meta, KEYS[4]: userLock
-- ARGV[1]: quantity

-- Already expired or released
if redis.call('EXISTS', KEYS[3]) == 0 then return 1 end

local globalReserved = tonumber(redis.call('GET', KEYS[1]) or '0')
local to_release = tonumber(ARGV[1])

-- Data inconsistency
if globalReserved < to_release then return -3 end

redis.call('DECRBY', KEYS[1], to_release)

-- Atomic Cleanup of all related keys
redis.call('DEL', KEYS[2], KEYS[3], KEYS[4])
return 1