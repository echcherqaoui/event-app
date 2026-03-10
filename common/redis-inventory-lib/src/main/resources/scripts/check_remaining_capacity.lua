-- KEYS[1]: sold, KEYS[2]: reserved,  KEYS[3]: status
-- ARGV[1]: newCapacity, ARGV[2]: blockStatus

local status = redis.call('GET', KEYS[3])
if status == ARGV[2] then return -7 end

local sold = tonumber(redis.call('GET', KEYS[1]) or '0')
local reserved = tonumber(redis.call('GET', KEYS[2]) or '0')
local newCapacity = tonumber(ARGV[1])

if newCapacity < (sold + reserved) then return -2 end

return 1