import http from 'k6/http';
import exec from 'k6/execution';
import { Counter } from 'k6/metrics';

const rate = Number(__ENV.RATE || 300);
const durationSeconds = Number(__ENV.DURATION_SECONDS || 10);
const voucherId = __ENV.VOUCHER_ID;
const baseUrl = __ENV.BASE_URL || 'http://127.0.0.1:8081';

if (!voucherId) throw new Error('VOUCHER_ID is required');
if (!Number.isInteger(rate) || rate < 1) throw new Error('RATE must be a positive integer');
if (!Number.isInteger(durationSeconds) || durationSeconds < 1) {
  throw new Error('DURATION_SECONDS must be a positive integer');
}

const accepted = new Counter('orders_accepted');
const soldOut = new Counter('orders_sold_out');
const rateLimited = new Counter('orders_rate_limited');
const unexpected = new Counter('orders_unexpected');

export const options = {
  discardResponseBodies: true,
  scenarios: {
    sustained_orders: {
      executor: 'constant-arrival-rate',
      rate,
      timeUnit: '1s',
      duration: `${durationSeconds}s`,
      preAllocatedVUs: rate,
      maxVUs: rate * 5,
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<1000'],
    orders_unexpected: ['count==0'],
    orders_rate_limited: ['count==0'],
    orders_sold_out: ['count==0'],
    dropped_iterations: ['count==0'],
    http_req_failed: ['rate==0'],
  },
};

function tokenFor(index) {
  return index.toString(16).padStart(32, '0');
}

export default function () {
  const index = exec.scenario.iterationInTest + 1;
  const response = http.post(
    `${baseUrl}/voucher-order/seckill/${voucherId}`,
    null,
    {
      headers: { authorization: `Bearer ${tokenFor(index)}` },
      tags: { endpoint: 'voucher-order-seckill-capacity' },
    },
  );

  if (response.status === 200) accepted.add(1);
  else if (response.status === 409) soldOut.add(1);
  else if (response.status === 429) rateLimited.add(1);
  else unexpected.add(1);
}
