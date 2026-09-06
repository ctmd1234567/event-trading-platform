for index=1,12000 do
  local token=string.format('%032x',index)
  local userId=9100000+index
  local key='login:token:' .. token
  redis.call('HSET',key,'id',tostring(userId),'nickName','capacity_user_' .. index)
  redis.call('EXPIRE',key,7200)
  redis.call('DEL','order:user:{' .. userId .. '}')
end

redis.call('DEL','order:voucher:{9900010250}')
redis.call('DEL','order:voucher:{9900010300}')
redis.call('DEL','order:voucher:{9900010350}')
redis.call('DEL','order:voucher:{9900010400}')
redis.call('DEL','order:global')

return 12000
