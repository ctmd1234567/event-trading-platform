local now = redis.call('TIME')
local nowMillis = tonumber(now[1]) * 1000 + math.floor(tonumber(now[2]) / 1000)
local remaining = {}

for index, key in ipairs(KEYS) do
    local capacity = tonumber(ARGV[(index - 1) * 2 + 1])
    local windowMillis = tonumber(ARGV[(index - 1) * 2 + 2]) * 1000
    local state = redis.call('HMGET', key, 'tokens', 'updated')
    local tokens = tonumber(state[1]) or capacity
    local updated = tonumber(state[2]) or nowMillis
    tokens = math.min(capacity, tokens + math.max(0, nowMillis - updated) * capacity / windowMillis)
    if tokens < 1 then
        return 0
    end
    remaining[index] = tokens - 1
end

for index, key in ipairs(KEYS) do
    local windowSeconds = tonumber(ARGV[(index - 1) * 2 + 2])
    redis.call('HSET', key, 'tokens', remaining[index], 'updated', nowMillis)
    redis.call('EXPIRE', key, math.max(2, windowSeconds * 2))
end

return 1
