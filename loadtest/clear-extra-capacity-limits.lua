redis.call('DEL','order:voucher:{9900010450}')
redis.call('DEL','order:voucher:{9900010500}')
redis.call('DEL','order:voucher:{9900010600}')
redis.call('DEL','order:global')
return 4
