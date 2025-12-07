import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';
import { b64encode } from 'k6/encoding';

// =====================
// Конфигурация
// =====================

const BASE_URL = __ENV.NOTIFI_BASE_URL || 'http://localhost:8080';

const API_KEY = __ENV.NOTIFI_API_KEY || 'demo-123';

const ADMIN_USER = __ENV.NOTIFI_ADMIN_USER || 'admin';
const ADMIN_PASSWORD = __ENV.NOTIFI_ADMIN_PASSWORD || 'admin';

const PUBLIC_NOTIFICATION_ID = __ENV.NOTIFI_PUBLIC_NOTIFICATION_ID; // публичный id
const ADMIN_NOTIFICATION_ID = __ENV.NOTIFI_ADMIN_NOTIFICATION_ID;   // id для /admin/notifications/{id}
const TEMPLATE_ID = __ENV.NOTIFI_TEMPLATE_ID;                       // UUID шаблона
const TEMPLATE_CODE = __ENV.NOTIFI_TEMPLATE_CODE || 'WELCOME_TEMPLATE123';

// =====================
// Метрики по статусам
// =====================

export const http_2xx = new Counter('http_2xx');
export const http_4xx = new Counter('http_4xx');
export const http_5xx = new Counter('http_5xx');
export const http_429 = new Counter('http_429');

function recordStatus(res) {
  if (res.status >= 200 && res.status < 300) {
    http_2xx.add(1);
  } else if (res.status === 429) {
    http_429.add(1);
  } else if (res.status >= 400 && res.status < 500) {
    http_4xx.add(1);
  } else if (res.status >= 500) {
    http_5xx.add(1);
  }
}

// =====================
// Параметры k6
// =====================

export const options = {
  discardResponseBodies: false,
  scenarios: {
    public_api: {
      executor: 'constant-vus',
      vus: 3,
      duration: '2m',
      exec: 'publicScenario',
    },
    admin_browsing: {
      executor: 'ramping-vus',
      startVUs: 1,
      stages: [
        { duration: '30s', target: 3 },
        { duration: '1m', target: 5 },
        { duration: '30s', target: 0 },
      ],
      exec: 'adminScenario',
    },
  },
  thresholds: {
    http_5xx: ['count == 0'], // ни одного 5xx
    'http_req_duration{expected_response:true}': ['p(95) < 500'], // p95 < 500 ms для успешных
  },
};

// =====================
// Общие хедеры
// =====================

const publicParams = {
  headers: {
    'X-API-Key': API_KEY,
    'Content-Type': 'application/json',
    Accept: 'application/json',
  },
};

const adminAuthHeader = 'Basic ' + b64encode(`${ADMIN_USER}:${ADMIN_PASSWORD}`);

const adminParams = {
  headers: {
    Authorization: adminAuthHeader,
    'Content-Type': 'application/json',
    Accept: 'application/json',
  },
};

// =====================
// Сценарий: Public API
// =====================

export function publicScenario() {
  // 1) Частые GET по уже существующей нотификации
  if (PUBLIC_NOTIFICATION_ID) {
    const resGet = http.get(
      `${BASE_URL}/api/v1/notifications/${PUBLIC_NOTIFICATION_ID}`,
      publicParams,
    );
    recordStatus(resGet);

    // 200 — ок, 404 — тоже норм, если запись выпилили;
    // 401/429 считаем ожидаемыми при косяках ключа/лимите.
    check(resGet, {
      'get public notification -> 200/404/401/429 (ожидаемо)': (r) =>
        [200, 404, 401, 429].includes(r.status),
    });
  }

  // 2) Иногда создаём новую нотификацию
  if (Math.random() < 0.3) {
    const extReqId = `k6-${__VU}-${Date.now()}`;
    const payload = JSON.stringify({
      externalRequestId: extReqId,
      channel: 'EMAIL',
      to: 'demo.user@example.com',
      templateCode: TEMPLATE_CODE,
      variables: {
        username: 'K6 load test',
        support_email: 'support@example.com',
      },
      // чуть в будущее, чтобы не ловить окна отправки
      sendAt: new Date(Date.now() + 60_000).toISOString(),
    });

    const resPost = http.post(
      `${BASE_URL}/api/v1/notifications`,
      payload,
      publicParams,
    );
    recordStatus(resPost);

    // 201/200 — успех; 429 — ожидаемый rate limit под нагрузкой
    check(resPost, {
      'create public notification -> 201/200/429 (ожидаемо)': (r) =>
        [201, 200, 429].includes(r.status),
    });
  }

  sleep(1);
}

// =====================
// Сценарий: Admin API
// =====================

export function adminScenario() {
  // 1) Листинг нотификаций
  const listAdmin = http.get(
    `${BASE_URL}/admin/notifications?status=SENT&page=0&size=5&sort=createdAt,desc`,
    adminParams,
  );
  recordStatus(listAdmin);
  check(listAdmin, {
    'admin notifications list -> 200': (r) => r.status === 200,
  });

  // 2) Детали + попытки по одной нотификации
  if (ADMIN_NOTIFICATION_ID) {
    const getAdmin = http.get(
      `${BASE_URL}/admin/notifications/${ADMIN_NOTIFICATION_ID}`,
      adminParams,
    );
    recordStatus(getAdmin);
    check(getAdmin, {
      'get admin notification -> 200/404': (r) => [200, 404].includes(r.status),
    });

    const attempts = http.get(
      `${BASE_URL}/admin/notifications/${ADMIN_NOTIFICATION_ID}/attempts?page=0&size=5&sort=createdAt,desc`,
      adminParams,
    );
    recordStatus(attempts);
    check(attempts, {
      'list attempts -> 200/404': (r) => [200, 404].includes(r.status),
    });
  }

  // 3) Листинг шаблонов
  const listTpl = http.get(
    `${BASE_URL}/admin/templates?page=0&size=5&sort=createdAt,desc`,
    adminParams,
  );
  recordStatus(listTpl);
  check(listTpl, {
    'list templates -> 200': (r) => r.status === 200,
  });

  // 4) Поиск по коду
  if (TEMPLATE_CODE) {
    const searchTpl = http.get(
      `${BASE_URL}/admin/templates?code=${encodeURIComponent(
        TEMPLATE_CODE,
      )}&page=0&size=5`,
      adminParams,
    );
    recordStatus(searchTpl);
    check(searchTpl, {
      'search template by code -> 200': (r) => r.status === 200,
    });
  }

  // 5) Операции по конкретному шаблону (id)
  if (TEMPLATE_ID) {
    const detailTpl = http.get(
      `${BASE_URL}/admin/templates/${TEMPLATE_ID}`,
      adminParams,
    );
    recordStatus(detailTpl);
    check(detailTpl, {
      'get template by id -> 200/404': (r) => [200, 404].includes(r.status),
    });

    // State-операции делаем редко, чтобы состояние не разъезжалось
    if (Math.random() < 0.1) {
      const updatePayload = JSON.stringify({
        subject: 'K6 load test subject',
      });

      const updateTpl = http.put(
        `${BASE_URL}/admin/templates/${TEMPLATE_ID}`,
        updatePayload,
        adminParams,
      );
      recordStatus(updateTpl);
      check(updateTpl, {
        'update template -> 200/404/422': (r) =>
          [200, 404, 422].includes(r.status),
      });

      const deactivateTpl = http.post(
        `${BASE_URL}/admin/templates/${TEMPLATE_ID}/deactivate`,
        null,
        adminParams,
      );
      recordStatus(deactivateTpl);
      check(deactivateTpl, {
        'deactivate template -> 200/404/422': (r) =>
          [200, 404, 422].includes(r.status),
      });

      const activateTpl = http.post(
        `${BASE_URL}/admin/templates/${TEMPLATE_ID}/activate`,
        null,
        adminParams,
      );
      recordStatus(activateTpl);
      check(activateTpl, {
        'activate template -> 200/404/422': (r) =>
          [200, 404, 422].includes(r.status),
      });
    }
  }

  sleep(1);
}
