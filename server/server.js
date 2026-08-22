const express = require("express");
const fs = require("fs");
const path = require("path");
const crypto = require("crypto");

const CONFIG = JSON.parse(fs.readFileSync(path.join(__dirname, "config.json"), "utf8"));
const DATA_DIR = path.join(__dirname, "data");
const QUIZ_FILE = path.join(DATA_DIR, "quizzes.json");

const ADMIN_KEY = CONFIG.key;
const ONLINE_WINDOW_MS = (CONFIG.onlineWindowSeconds || 90) * 1000;

const app = express();
app.use(express.json({ limit: "5mb" }));

function loadQuizzes() {
  try {
    return JSON.parse(fs.readFileSync(QUIZ_FILE, "utf8")).quizzes || [];
  } catch {
    return [];
  }
}

function saveQuizzes(list) {
  const tmp = QUIZ_FILE + ".tmp";
  fs.writeFileSync(tmp, JSON.stringify({ quizzes: list }, null, 2));
  fs.renameSync(tmp, QUIZ_FILE);
}

// heartbeat registry: deviceId -> lastSeen
const devices = new Map();

function pruneDevices() {
  const now = Date.now();
  for (const [id, ts] of devices) {
    if (now - ts > ONLINE_WINDOW_MS * 2) devices.delete(id);
  }
}
setInterval(pruneDevices, 30_000);

function isAdmin(req) {
  const key = req.headers["x-admin-key"];
  return typeof key === "string" && key === ADMIN_KEY;
}

function requireAdmin(req, res, next) {
  if (!isAdmin(req)) {
    return res.status(401).json({ error: "Unauthorized" });
  }
  next();
}

function validQuestion(q) {
  if (!q || typeof q.questionText !== "string" || q.questionText.trim().length === 0) return false;
  const opts = [q.optionA, q.optionB, q.optionC, q.optionD];
  if (opts.filter((o) => typeof o === "string" && o.trim().length > 0).length < 2) return false;
  const ans = String(q.correctOption || "").toLowerCase();
  return ["a", "b", "c", "d"].includes(ans);
}

// ---------- public API ----------

// list all admin/community quizzes
app.get("/api/quizzes", (req, res) => {
  res.json({ quizzes: loadQuizzes() });
});

// heartbeat from the app: keeps a device marked online
app.post("/api/heartbeat", (req, res) => {
  const deviceId = req.body && req.body.deviceId;
  if (!deviceId || typeof deviceId !== "string" || deviceId.length < 8) {
    return res.status(400).json({ error: "deviceId required" });
  }
  devices.set(deviceId, Date.now());
  let online = 0;
  const now = Date.now();
  for (const ts of devices.values()) {
    if (now - ts <= ONLINE_WINDOW_MS) online++;
  }
  res.json({ ok: true, online, windowSeconds: ONLINE_WINDOW_MS / 1000 });
});

// ---------- admin API ----------

app.post("/api/admin/quizzes", requireAdmin, (req, res) => {
  const b = req.body || {};
  const title = String(b.title || "").trim();
  const questions = Array.isArray(b.questions) ? b.questions.filter(validQuestion) : [];
  if (!title) return res.status(400).json({ error: "Title required" });
  if (questions.length === 0) return res.status(400).json({ error: "At least one valid question required" });

  const now = Date.now();
  const quiz = {
    id: crypto.randomUUID(),
    title,
    category: String(b.category || "General Knowledge").trim(),
    difficulty: String(b.difficulty || "Easy").trim(),
    tags: String(b.tags || "").trim(),
    description: String(b.description || "").trim(),
    timeLimitSeconds: Number.isInteger(b.timeLimitSeconds) && b.timeLimitSeconds > 0 ? b.timeLimitSeconds : null,
    createdAt: now,
    updatedAt: now,
    questions: questions.map((q) => ({
      questionText: q.questionText.trim(),
      optionA: String(q.optionA || ""),
      optionB: String(q.optionB || ""),
      optionC: String(q.optionC || ""),
      optionD: String(q.optionD || ""),
      correctOption: String(q.correctOption).toLowerCase()
    }))
  };
  const list = loadQuizzes();
  list.push(quiz);
  saveQuizzes(list);
  res.json({ ok: true, quiz });
});

app.delete("/api/admin/quizzes/:id", requireAdmin, (req, res) => {
  const list = loadQuizzes();
  const next = list.filter((q) => q.id !== req.params.id);
  if (next.length === list.length) return res.status(404).json({ error: "Quiz not found" });
  saveQuizzes(next);
  res.json({ ok: true });
});

// online users for the admin panel
app.get("/api/admin/online", requireAdmin, (req, res) => {
  const now = Date.now();
  const entries = [];
  for (const [id, ts] of devices) {
    if (now - ts <= ONLINE_WINDOW_MS) entries.push({ deviceId: id.slice(0, 8), lastSeen: ts });
  }
  entries.sort((a, b) => b.lastSeen - a.lastSeen);
  res.json({ online: entries.length, windowSeconds: ONLINE_WINDOW_MS / 1000, devices: entries });
});

// static admin panel
app.use("/admin", express.static(path.join(__dirname, "admin")));
app.get("/", (req, res) => res.redirect("/admin"));

const PORT = process.env.PORT || 3000;
app.listen(PORT, "0.0.0.0", () => {
  console.log(`QuizForge server running on http://0.0.0.0:${PORT}`);
});
