-- KEYS[1]: reserved, KEYS[2]: sold, KEYS[3]: lock,  KEYS[4]: status
-- KEYS[5]: dataKey, KEYS[6]: userLockKey
-- ARGV[1]: status, ARGV[2]: to_release

local status = redis.call('GET', KEYS[4])
-- Not eligible for confirmation
if status ~= ARGV[1] then return -1 end

local reservedQuantity = tonumber(redis.call('GET', KEYS[3]) or '0')

-- Reservation expired (Shadow Key is gone)
if reservedQuantity == 0 then return -4 end

local to_release = tonumber(ARGV[2])

-- Mismatch (User trying to pay for fewer/more than reserved)
if reservedQuantity ~= to_release then return -6 end

local globalReserved = tonumber(redis.call('GET', KEYS[1]) or '0')

-- Data inconsistency (Global reserved count too low)
if globalReserved < reservedQuantity then return -3 end

redis.call('DECRBY', KEYS[1], reservedQuantity)
redis.call('INCRBY', KEYS[2], reservedQuantity)

-- Atomic Cleanup of ALL keys
redis.call('DEL', KEYS[3], KEYS[5], KEYS[6])
return 1