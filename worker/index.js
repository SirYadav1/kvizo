// Kvizo Cloudflare Worker — serves quiz data from GitHub repo
// Deploy: npx wrangler deploy (or use dashboard)

const GITHUB_RAW = "https://raw.githubusercontent.com/SirYadav1/kvizo-community/master";

const CORS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type",
};

async function fetchJSON(url) {
  const res = await fetch(url, {
    headers: { "User-Agent": "Kvizo-Worker/1.0", "Accept": "application/json" },
    cf: { cacheTtl: 300 },  // 5 min edge cache
  });
  if (!res.ok) throw new Error(`GitHub ${res.status}: ${url}`);
  return res.json();
}

// GET / — health check
function health() {
  return Response.json({ status: "ok", service: "kvizo-api", version: "v1" }, { headers: CORS });
}

// GET /manifest
async function getManifest() {
  const data = await fetchJSON(`${GITHUB_RAW}/manifest.json`);
  return Response.json(data, { headers: { ...CORS, "Cache-Control": "public, max-age=300" } });
}

// GET /quizzes?category=science
async function getQuizzes(category) {
  const manifest = await fetchJSON(`${GITHUB_RAW}/manifest.json`);
  let quizzes = manifest.quizzes || [];
  if (category && category !== "all") {
    quizzes = quizzes.filter(q => q.category.toLowerCase() === category.toLowerCase());
  }
  // Return lightweight list (no full questions)
  const list = quizzes.map(q => ({
    id: q.id,
    title: q.title,
    category: q.category,
    difficulty: q.difficulty,
    author: q.author,
    questions_count: q.questions_count,
  }));
  return Response.json({ quizzes: list, count: list.length }, { headers: { ...CORS, "Cache-Control": "public, max-age=300" } });
}

// GET /quiz/:id
async function getQuiz(quizId) {
  const manifest = await fetchJSON(`${GITHUB_RAW}/manifest.json`);
  const entry = manifest.quizzes.find(q => q.id === quizId);
  if (!entry) return Response.json({ error: "Quiz not found" }, { status: 404, headers: CORS });

  const quizData = await fetchJSON(`${GITHUB_RAW}/${entry.file}`);
  return Response.json(quizData, { headers: { ...CORS, "Cache-Control": "public, max-age=3600" } });
}

export default {
  async fetch(request, env, ctx) {
    try {
      const url = new URL(request.url);
      const path = url.pathname;
      const cat = url.searchParams.get("category");

      if (request.method === "OPTIONS") return new Response(null, { headers: CORS });
      if (path === "/" || path === "/health") return health();
      if (path === "/manifest") return getManifest();
      if (path === "/quizzes") return getQuizzes(cat);
      if (path.startsWith("/quiz/")) return getQuiz(path.split("/quiz/")[1]);

      return Response.json({ error: "Not found" }, { status: 404, headers: CORS });
    } catch (err) {
      return Response.json({ error: err.message }, { status: 500, headers: CORS });
    }
  },
};
