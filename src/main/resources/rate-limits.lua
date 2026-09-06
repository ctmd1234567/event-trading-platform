for index, key in ipairs(KEYS) do
    local maximum = tonumber(ARGV[(index - 1) * 2 + 1])
    local current = tonumber(redis.call('GET', key) or '0')
    if current >= maximum then
        return 0
    end
end

for index, key in ipairs(KEYS) do
    local window = tonumber(ARGV[(index - 1) * 2 + 2])
    local count = redis.call('INCR', key)
    if count == 1 then
        redis.call('EXPIRE', key, window)
    end
end

return 1
