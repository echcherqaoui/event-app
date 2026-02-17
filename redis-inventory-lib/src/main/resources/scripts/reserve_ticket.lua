-- KEYS[1]: capacity, [2]: sold, [3]: reserved, [4]: status
-- KEYS[5]: timerKey ("res:lock:UUID") -> The expiration trigger
-- KEYS[6]: dataKey ("res:meta:UUID")  -> The forensic metadata backup
-- KEYS[7]: userLockKey ("u:l:UID:EID")
-- ARGV[1]: requiredStatus, [2]: requestedQty, [3]: timerTtl, [4]: dataTtl, [5]: metadata

-- Anti-Hoarding Check (User already has an active reservation)
if redis.call('EXISTS', KEYS[7]) == 1 then return -8 end

local status = redis.call('GET', KEYS[4])

-- Not eligible for reservations
if status ~= ARGV[1] then return -1 end

local capacity = tonumber(redis.call('GET', KEYS[1]) or '0')
local sold = tonumber(redis.call('GET', KEYS[2]) or '0')
local globalReserved = tonumber(redis.call('GET', KEYS[3]) or '0')
local requested = tonumber(ARGV[2])
local ttl = tonumber(ARGV[3]);

-- Insufficient inventory
if capacity - (sold + globalReserved) < requested then return -2 end

redis.call('INCRBY', KEYS[3], requested)

-- Set Expiration Trigger
redis.call('SET', KEYS[5], requested, 'EX', ttl)

-- Set Shadow Metadata: Lives longer to allow recovery if DB save fails
redis.call('SET', KEYS[6], ARGV[5], 'EX', tonumber(ARGV[4]))

redis.call('SET', KEYS[7], '1', 'EX', ttl) -- Lock the user
return 1