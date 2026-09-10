local values = redis.call('HGETALL', KEYS[1])
if #values == 0 then
    return values
end

local ttl = redis.call('TTL', KEYS[1])
if ttl < tonumber(ARGV[2]) then
    redis.call('EXPIRE', KEYS[1], tonumber(ARGV[1]))
end

return values
