-- KEYS[1]: capacity, KEYS[2]: sold, KEYS[3]: reserved, KEYS[4]: status

local sold = tonumber(redis.call('GET', KEYS[2]) or "0")
local reserved = tonumber(redis.call('GET', KEYS[3]) or "0")

-- Indicate failure/cancellation required instead
if (sold + reserved) > 0 then return -2 end

-- Delete all inventory associated keys
redis.call('DEL', KEYS[1], KEYS[2], KEYS[3], KEYS[4])
return 1