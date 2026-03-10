-- KEYS[1]: capacity, KEYS[2]: sold, KEYS[3]: reserved, KEYS[4]: status
-- ARGV[1]: capacity, ARGV[2]: status

-- Do nothing if already exists
if redis.call('EXISTS', KEYS[1]) == 1 then return end

redis.call('SET', KEYS[1], ARGV[1])
redis.call('SET', KEYS[2], '0')
redis.call('SET', KEYS[3], '0')
redis.call('SET', KEYS[4], ARGV[2])

return 1