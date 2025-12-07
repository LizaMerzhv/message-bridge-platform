import http from 'k6/http';
import { check } from 'k6';
import encoding from 'k6/encoding';
import { sleep } from 'k6';
import { fail } from 'k6';

export const options = {
  vus: 1,
  iterations: 1,
};

const BASE_URL = __ENV.NOTIFI_BASE_URL || 'http://localhost:8080';

// публичный API-ключ
const API_KEY = __ENV.NOTIFI_API_KEY || 'demo-123';

// админ Basic Auth
const ADMIN_USER = __ENV.NOTIFI_ADMIN_USER || 'admin';
const ADMIN_PASS = __ENV.NOTIFI_ADMIN_PASS || 'admin';
const ADMIN_AUTH =
  'Basic ' + encoding.b64encode(`${ADMIN_USER}:${ADMIN_PASS}`);

// ID нотификаций (UUID, которые ты дала)
const PUBLIC_NOTIFICATION_ID =
  __ENV.NOTIFI_PUBLIC_NOTIFICATION_ID ||
  '22222222-2222-2222-2222-222222222222';

const ADMIN_NOTIFICATION_ID =
  __ENV.NOTIFI_ADMIN_NOTIFICATION_ID ||
  '22222222-2222-2222-2222-222222222222';

// код шаблона, по которому ищем id
const TEMPLATE_CODE = __ENV.NOTIFI_TEMPLATE_CODE || 'WELCOME_TEMPLATE123';

export default function () {
  const apiHeaders = {
    'Content-Type': 'application/json',
    'X-API-Key': API_KEY,
  };

  const adminHeaders = {
    'Content-Type': 'application/json',
    Authorization: ADMIN_AUTH,
  };

  //
  // 0. Найти шаблон по коду и вытащить его id
  //
  const searchUrl = `${BASE_URL}/admin/templates?code=${encodeURIComponent(
    TEMPLATE_CODE,
  )}&page=0&size=1&sort=createdAt,desc`;

  const resTplSearch = http.get(searchUrl, { headers: adminHeaders });

  check(resTplSearch, {
    'search template by code -> 200': (r) => r.status === 200,
  });

  if (resTplSearch.status !== 200) {
    fail(`Template search failed with status ${resTplSearch.status}`);
  }

  let templateId;

  try {
    const body = JSON.parse(resTplSearch.body);
    if (!body.content || body.content.length === 0) {
      fail(`Template with code ${TEMPLATE_CODE} not found in /admin/templates search`);
    }
    templateId = body.content[0].id;
  } catch (e) {
    fail(`Failed to parse template search response or get id: ${e}`);
  }

  //
  // 1. Public: GET /api/v1/notifications/{id}
  //
  const resPublicGet = http.get(
    `${BASE_URL}/api/v1/notifications/${PUBLIC_NOTIFICATION_ID}`,
    { headers: apiHeaders },
  );

  check(resPublicGet, {
    'get public notification -> 200': (r) => r.status === 200,
  });

  //
  // 2. Admin: GET /admin/notifications
  //
  const resAdminList = http.get(
    `${BASE_URL}/admin/notifications?page=0&size=1&sort=createdAt,desc`,
    { headers: adminHeaders },
  );

  check(resAdminList, {
    'admin notifications list -> 200': (r) => r.status === 200,
  });

  //
  // 3. Admin: GET /admin/notifications/{id}
  //
  const resAdminGet = http.get(
    `${BASE_URL}/admin/notifications/${ADMIN_NOTIFICATION_ID}`,
    { headers: adminHeaders },
  );

  check(resAdminGet, {
    'get admin notification -> 200': (r) => r.status === 200,
  });

  //
  // 4. Admin: GET /admin/notifications/{id}/attempts
  //
  const resAttempts = http.get(
    `${BASE_URL}/admin/notifications/${ADMIN_NOTIFICATION_ID}/attempts?page=0&size=3&sort=createdAt,desc`,
    { headers: adminHeaders },
  );

  check(resAttempts, {
    'list attempts -> 200': (r) => r.status === 200,
  });

  //
  // 5. Admin Templates: GET /admin/templates
  //
  const resTplList = http.get(
    `${BASE_URL}/admin/templates?page=0&size=5&sort=createdAt,desc`,
    { headers: adminHeaders },
  );

  check(resTplList, {
    'list templates -> 200': (r) => r.status === 200,
  });

  //
  // 6. Admin Templates: GET /admin/templates/{id}
  //
  const resTplGet = http.get(
    `${BASE_URL}/admin/templates/${templateId}`,
    { headers: adminHeaders },
  );

  check(resTplGet, {
    'get template by id -> 200': (r) => r.status === 200,
  });

  //
  // 7. Admin Templates: PUT /admin/templates/{id}
  //
  const updateBody = JSON.stringify({
    subject: 'Smoke test subject',
  });

  const resTplUpdate = http.put(
    `${BASE_URL}/admin/templates/${templateId}`,
    updateBody,
    { headers: adminHeaders },
  );

  check(resTplUpdate, {
    'update template -> 200': (r) => r.status === 200,
  });

  //
  // 8. Admin Templates: POST /admin/templates/{id}/deactivate
  //
  const resTplDeactivate = http.post(
    `${BASE_URL}/admin/templates/${templateId}/deactivate`,
    null,
    { headers: adminHeaders },
  );

  check(resTplDeactivate, {
    'deactivate template -> 200': (r) => r.status === 200,
  });

  //
  // 9. Admin Templates: POST /admin/templates/{id}/activate
  //
  const resTplActivate = http.post(
    `${BASE_URL}/admin/templates/${templateId}/activate`,
    null,
    { headers: adminHeaders },
  );

  check(resTplActivate, {
    'activate template -> 200': (r) => r.status === 200,
  });

  sleep(1);
}
