-- KEYS[1]: capacity, KEYS[2]: reserved, KEYS[3]: status,
-- ARGV[1]: blockedStatus

-- Ensure event actually exists in inventory
if redis.call('EXISTS', KEYS[1]) == 0 then return -5 end

-- Check if already cancelled to avoid redundant updates
local currentStatus = redis.call('GET', KEYS[3])
if currentStatus == ARGV[1] then return -7 end

redis.call('SET', KEYS[3], ARGV[1])
redis.call('SET', KEYS[2], '0')
return 1