-- KEYS share the phone hash tag; failed attempts expire and success consumes the code atomically.
local attempts = redis.call('INCR', KEYS[2])
if attempts == 1 then redis.call('EXPIRE', KEYS[2], 600) end
if attempts > 5 then return -1 end
local code = redis.call('GET', KEYS[1])
if not code or code ~= ARGV[1] then return 0 end
redis.call('DEL', KEYS[1])
return 1
