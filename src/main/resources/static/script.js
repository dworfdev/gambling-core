const TG_ID = window.Telegram?.WebApp?.initDataUnsafe?.user?.id;

let activeHash = "";
let activePaymentType = "";
let currentWheelAngle = 0;
let isSpinning = false;

// ─── Toast ────────────────────────────────────────────────
function toast(msg, type = "info") {
  const t = document.createElement("div");
  t.className = `toast toast-${type}`;
  t.textContent = msg;
  document.getElementById("toast-container").appendChild(t);
  requestAnimationFrame(() => t.classList.add("show"));
  setTimeout(() => {
    t.classList.remove("show");
    setTimeout(() => t.remove(), 300);
  }, 3000);
}

// ─── Screen routing ───────────────────────────────────────
function showScreen(name) {
  const map = {
    wallet: "screen-wallet",
    main:   "screen-main",
    pay:    "screen-pay",
    admin:  "screen-admin",
  };
  Object.values(map).forEach(id => {
    const el = document.getElementById(id);
    if (el) el.style.display = "none";
  });
  const target = document.getElementById(map[name]);
  if (target) target.style.display = "flex";
  if (name === "admin") loadAdminStats();
}

async function navigateAway(screenName) {
  if (activeHash) await invalidateCurrentHash();
  showScreen(screenName);
  if (screenName === "main") setActiveNav("nav-main");
}

async function invalidateCurrentHash() {
  if (!activeHash) return;
  try {
    await fetch(`/api/payments/invalidate?id=${TG_ID}`, { method: "POST" });
  } catch (e) {}
  activeHash = "";
}

// ─── Nav ──────────────────────────────────────────────────
function setActiveNav(id) {
  document.querySelectorAll(".nav-item").forEach(el => el.classList.remove("active"));
  const el = document.getElementById(id);
  if (el) el.classList.add("active");
}

// ─── Load player data ─────────────────────────────────────
async function loadData() {
  if (!TG_ID) {
    toast("Откройте приложение через Telegram", "error");
    return;
  }

  try {
    let res = await fetch(`/api/players/${TG_ID}`);

    if (res.status === 400 || res.status === 404) {
      const createRes = await fetch(`/api/players?tgId=${TG_ID}`, { method: "POST" });
      if (!createRes.ok) throw new Error("Не удалось создать пользователя");
      res = await fetch(`/api/players/${TG_ID}`);
    }

    if (!res.ok) throw new Error(await res.text());
    const p = await res.json();

    const balStr = p.balance != null ? parseFloat(p.balance).toFixed(2) : "—";

    document.getElementById("p-id").textContent = p.tgId ?? "—";
    document.getElementById("p-balance").textContent = balStr;

    const balCard = document.getElementById("balance");
    if (balCard) balCard.textContent = balStr;

    const payBal = document.getElementById("pay-balance-display");
    if (payBal) payBal.textContent = balStr;

    const payHint = document.getElementById("pay-balance-hint");
    if (payHint) payHint.textContent = balStr;

    const adminBtn = document.getElementById("admin-btn");
    if (adminBtn) adminBtn.style.display = p.isAdmin ? "" : "none";

    const navAdmin = document.getElementById("nav-admin");
    if (navAdmin) navAdmin.style.display = p.isAdmin ? "" : "none";

    const walletEl = document.getElementById("p-wallet");
    if (p.walletAddress) {
      walletEl.textContent = p.walletAddress;
      walletEl.title = p.walletAddress;
      showScreen("main");
    } else {
      walletEl.textContent = "Wallet not linked";
      walletEl.title = "";
      showScreen("wallet");
    }
  } catch (e) {
    toast("Ошибка загрузки данных: " + e.message, "error");
  }
}

// ─── Bind wallet ──────────────────────────────────────────
async function saveWallet() {
  const addr = document.getElementById("wallet-input").value.trim();
  if (addr.length < 10) { toast("Слишком короткий адрес", "error"); return; }
  try {
    const res = await fetch("/api/payments/player/bind-wallet", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ id: TG_ID, address: addr }),
    });
    if (!res.ok) throw new Error(await res.text());
    toast("Кошелёк привязан", "success");
    await loadData();
  } catch (e) {
    toast("Ошибка привязки: " + e.message, "error");
  }
}

// ─── Wheel (20 sectors) ───────────────────────────────────
function buildWheel() {}

// ─── Play dice ────────────────────────────────────────────
async function playDice() {
  if (isSpinning) return;

  const bet = parseFloat(document.getElementById("bet-amount").value);
  if (!bet || bet <= 0) { toast("Укажите ставку", "error"); return; }

  const wheel   = document.getElementById("dice-wheel");
  const outcome = document.getElementById("dice-outcome");
  const spinBtn = document.getElementById("spin-btn");

  isSpinning = true;
  spinBtn.disabled = true;
  outcome.textContent = "...";
  outcome.style.color = "var(--muted)";

  try {
    const res = await fetch(`/api/games/dice?id=${TG_ID}&bet=${bet}`, { method: "POST" });

    if (!res.ok) {
      const err = await res.json().catch(() => ({ message: "Ошибка сервера" }));
      toast(err.message, "error");
      reset(); return;
    }

    const raw = await res.text();
    const [sectorStr, resultType, winAmountStr] = raw.split("|");
    const sector = parseInt(sectorStr);

    const TOTAL_SECTORS = 20;
    const sectorDeg = 360 / TOTAL_SECTORS;

    const targetAngle = sector * sectorDeg + sectorDeg / 2;
    const currentMod = ((currentWheelAngle % 360) + 360) % 360;
    let delta = targetAngle - currentMod;
    if (delta < 0) delta += 360;

    currentWheelAngle += 10 * 360 + delta;
    wheel.style.transform = `rotate(${-currentWheelAngle}deg)`;

    setTimeout(() => {
      const win = parseFloat(winAmountStr).toFixed(2);
      if (resultType === "yellow") {
        outcome.textContent = `MAX WIN! +${win}`;
        outcome.style.color = "#FF9500";
        toast(`MAX WIN! +${win} 🎰`, "success");
      } else if (resultType === "green") {
        outcome.textContent = `ВЫИГРЫШ +${win}`;
        outcome.style.color = "var(--green)";
        toast(`Выигрыш +${win}`, "success");
      } else {
        outcome.textContent = "ПРОИГРЫШ";
        outcome.style.color = "var(--red)";
        toast("Не повезло", "error");
      }
      reset();
      loadData();
    }, 4100);

  } catch (e) {
    toast("Ошибка связи", "error");
    reset();
  }

  function reset() { isSpinning = false; spinBtn.disabled = false; }
}

// ─── Navigate to payment ──────────────────────────────────
async function navToPayment(type) {
  if (activeHash) await invalidateCurrentHash();

  try {
    const res = await fetch(`/api/payments/prepare?id=${TG_ID}`, { cache: "no-store" });
    if (!res.ok) throw new Error("Сервер вернул " + res.status);

    activeHash = (await res.text()).replace(/^"|"$/g, "").trim();
    activePaymentType = type;

    document.getElementById("pay-title").textContent =
      type === "deposit" ? "Пополнение" : "Вывод средств";
    document.getElementById("pay-exec-btn").onclick = () => executePayment(type);
    document.getElementById("pay-amount").value = "";

    showScreen("pay");
  } catch (e) {
    toast("Ошибка получения токена: " + e.message, "error");
  }
}

// ─── Execute payment ──────────────────────────────────────
async function executePayment(type) {
  const amount = parseFloat(document.getElementById("pay-amount").value);
  if (!amount || amount <= 0) { toast("Укажите сумму", "error"); return; }
  if (!activeHash) { toast("Сессия истекла — начните заново", "error"); return; }

  const endpoint = type === "deposit" ? "/api/payments/deposit" : "/api/payments/cashout";

  try {
    const res = await fetch(endpoint, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ id: TG_ID, amount, hash: activeHash }),
    });

    const text = await res.text();
    activeHash = "";
    if (res.ok) {
      toast(type === "deposit" ? "Заявка на пополнение отправлена" : "Заявка на вывод отправлена", "success");
      showScreen("main");
      await loadData();
    } else {
      try { toast(JSON.parse(text).message, "error"); }
      catch { toast(text, "error"); }
    }
  } catch (e) {
    toast("Ошибка сети", "error");
  }
}

// ─── Admin: stats ─────────────────────────────────────────
async function loadAdminStats() {
  try {
    const res = await fetch(`/api/admin/stats?tgId=${TG_ID}`);
    if (!res.ok) return;
    const data = await res.json();
    const el = document.getElementById("stat-users");
    if (el) el.textContent = data.totalUsers ?? "—";
  } catch (e) {}
}

// ─── Admin: load pending ──────────────────────────────────
async function loadPending() {
  const targetId = document.getElementById("admin-target-tgid").value.trim();
  const list = document.getElementById("pending-list");
  list.innerHTML = "<span class='hint'>Загрузка...</span>";

  try {
    const params = new URLSearchParams({ tgId: TG_ID, page: 0 });
    if (targetId) params.append("targetTgId", targetId);

    const res = await fetch(`/api/admin/pending?` + params.toString());
    if (!res.ok) {
      const err = await res.json();
      list.innerHTML = `<span class='hint'>Ошибка: ${err.message}</span>`;
      return;
    }

    const page = await res.json();
    const transactions = page.content ?? page;

    if (!transactions.length) {
      list.innerHTML = "<span class='hint'>Нет ожидающих заявок</span>";
      return;
    }

    list.innerHTML = "";
    transactions.forEach(tx => renderTxCard(tx, list));
  } catch (e) {
    list.innerHTML = `<span class='hint'>Ошибка: ${e.message}</span>`;
  }
}

function renderTxCard(tx, container) {
  const card = document.createElement("div");
  card.className = "tx-card";
  card.id = `tx-${tx.id}`;

  const typeLabel = tx.type === "DEPOSIT" ? "Пополнение" : "Вывод";
  const created = tx.createdAt ? new Date(tx.createdAt).toLocaleString("ru") : "—";

  card.innerHTML = `
    <div><b>${typeLabel}</b> — <span class="tx-amount">${parseFloat(tx.amount).toFixed(2)} USDT</span></div>
    <div class="tx-meta">ID: ${tx.id} · tgId: ${tx.tgId}</div>
    <div class="tx-meta">Кошелёк: ${tx.walletAddress ?? "—"}</div>
    <div class="tx-meta">Создана: ${created}</div>
    <div class="tx-actions">
      <button class="btn-approve" onclick="approveTransaction(${tx.id}, true, this)">Одобрить</button>
      <button class="btn-reject"  onclick="approveTransaction(${tx.id}, false, this)">Отклонить</button>
    </div>
  `;
  container.appendChild(card);
}

async function approveTransaction(transactionId, approve, btn) {
  btn.closest(".tx-actions").querySelectorAll("button").forEach(b => b.disabled = true);

  try {
    const params = new URLSearchParams({ tgId: TG_ID, transactionId, approve });
    const res = await fetch("/api/admin/approve?" + params.toString(), { method: "POST" });

    const text = await res.text();
    if (res.ok) {
      toast(`Заявка #${transactionId} ${approve ? "одобрена" : "отклонена"}`, "success");
      const card = document.getElementById(`tx-${transactionId}`);
      if (card) {
        card.style.opacity = "0.4";
        card.querySelector(".tx-actions").innerHTML =
          `<span class="tx-meta">${approve ? "✓ Одобрено" : "✗ Отклонено"}</span>`;
      }
    } else {
      try { toast(JSON.parse(text).message, "error"); }
      catch { toast(text, "error"); }
      btn.closest(".tx-actions").querySelectorAll("button").forEach(b => b.disabled = false);
    }
  } catch (e) {
    toast("Ошибка сети", "error");
    btn.closest(".tx-actions").querySelectorAll("button").forEach(b => b.disabled = false);
  }
}

// ─── Init ─────────────────────────────────────────────────
document.addEventListener("DOMContentLoaded", () => {
  buildWheel();
  loadData();
});
