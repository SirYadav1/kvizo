// Kvizo Cloudflare Worker — API Gateway
// Serves community quizzes and notifications via Cloudflare Workers
// All data comes from kvizo-community repo (GitHub raw)

const GITHUB_RAW = "https://raw.githubusercontent.com/SirYadav1/kvizo-community/master";
const CORS = { "Access-Control-Allow-Origin": "*", "Access-Control-Allow-Methods": "GET, POST, OPTIONS", "Access-Control-Allow-Headers": "Content-Type" };

const CACHE_QUIZZES = 300;
const CACHE_NOTIF = 60;

async function fetchJSON(url) {
  const res = await fetch(url, { headers: { "User-Agent": "Kvizo-Worker/1.0" }, cf: { cacheTtl: CACHE_QUIZZES } });
  if (!res.ok) throw new Error("GitHub " + res.status);
  return res.json();
}

function health() {
  return Response.json({ status: "ok", service: "kvizo-api", version: "v2" }, { headers: CORS });
}

async function getQuizzes(category) {
  const data = await fetchJSON(GITHUB_RAW + "/community.json");
  let quizzes = data.quizzes || [];
  if (category && category !== "all") {
    quizzes = quizzes.filter(q => (q.category || "").toLowerCase() === category.toLowerCase());
  }
  const list = quizzes.map(q => ({
    id: q.id, title: q.title, category: q.category,
    difficulty: q.difficulty || "Medium", author: q.author || "Unknown",
    questions_count: (q.questions || []).length,
    sha256: q.sha256 || ""
  }));
  return Response.json({ quizzes: list, count: list.length }, { headers: { ...CORS, "Cache-Control": "public, max-age=300" } });
}

async function getQuiz(quizId) {
  const data = await fetchJSON(GITHUB_RAW + "/community.json");
  const quiz = (data.quizzes || []).find(q => q.id === quizId);
  if (!quiz) return Response.json({ error: "Not found" }, { status: 404, headers: CORS });
  return Response.json(quiz, { headers: { ...CORS, "Cache-Control": "public, max-age=3600" } });
}

async function getManifest() {
  const data = await fetchJSON(GITHUB_RAW + "/manifest.json");
  const sig = await fetchText(GITHUB_RAW + "/manifest.json.sig");
  return Response.json({ manifest: data, signature: sig }, { headers: { ...CORS, "Cache-Control": "public, max-age=60" } });
}

async function getNotifications() {
  const data = await fetchJSON(GITHUB_RAW + "/notifications.json");
  const sig = await fetchText(GITHUB_RAW + "/notifications.json.sig");
  return Response.json({ notifications: data.notifications || [], signature: sig }, { headers: { ...CORS, "Cache-Control": "public, max-age=" + CACHE_NOTIF } });
}

async function fetchText(url) {
  const res = await fetch(url, { headers: { "User-Agent": "Kvizo-Worker/1.0" } });
  return await res.text();
}

async function handleNotify(request, env) {
  const body = await request.json().catch(() => ({}));
  const deviceToken = body.deviceToken;
  const notification = body.notification;
  if (!deviceToken || !notification) {
    return Response.json({ error: "deviceToken and notification required" }, { status: 400, headers: CORS });
  }
  return Response.json({ registered: true }, { headers: CORS });
}

async function handleNotifyAll(request, env) {
  return Response.json({ sent: 0 }, { headers: CORS });
}

export default {
  async fetch(request) {
    try {
      const url = new URL(request.url);
      const p = url.pathname;
      const cat = url.searchParams.get("category");
      if (request.method === "OPTIONS") return new Response(null, { headers: CORS });
      if (p === "/" || p === "/health") return health();
      if (p === "/api/quizzes") return await getQuizzes(cat);
      if (p.startsWith("/api/quiz/")) return await getQuiz(p.split("/api/quiz/")[1]);
      if (p === "/api/manifest") return await getManifest();
      if (p === "/api/notifications") return await getNotifications();
      if (p === "/api/notify" && request.method === "POST") return await handleNotify(request, env);
      if (p === "/api/notify/all" && request.method === "POST") return await handleNotifyAll(request, env);
      return Response.json({ error: "Not found" }, { status: 404, headers: CORS });
    } catch (e) {
      return Response.json({ error: e.message }, { status: 500, headers: CORS });
    }
  },
};
