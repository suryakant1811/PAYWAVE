(function () {
  const API = "";

  const $ = (id) => document.getElementById(id);
  const authMsg = $("authMsg");
  const topupMsg = $("topupMsg");
  const xferMsg = $("xferMsg");

  function token() {
    return sessionStorage.getItem("accessToken");
  }

  function saveSession(data) {
    sessionStorage.setItem("accessToken", data.accessToken);
    sessionStorage.setItem("refreshToken", data.refreshToken);
    sessionStorage.setItem("userId", data.userId);
    sessionStorage.setItem("email", data.email);
  }

  function clearSession() {
    sessionStorage.clear();
  }

  function showMsg(el, text, ok) {
    el.textContent = text;
    el.classList.remove("hidden", "error", "ok");
    el.classList.add(ok ? "ok" : "error");
  }

  function hideMsg(el) {
    el.classList.add("hidden");
  }

  async function api(method, path, body, expect) {
    const headers = { "Content-Type": "application/json" };
    const t = token();
    if (t) headers.Authorization = "Bearer " + t;
    const res = await fetch(API + path, {
      method,
      headers,
      body: body == null ? undefined : JSON.stringify(body),
    });
    const raw = await res.text();
    let payload;
    try {
      payload = raw ? JSON.parse(raw) : {};
    } catch {
      payload = { success: false, error: { message: raw || res.statusText } };
    }
    if (typeof expect === "number" && res.status !== expect) {
      const msg = payload.error?.message || res.status + " " + res.statusText;
      throw new Error(msg);
    }
    if (!payload.success) {
      throw new Error(payload.error?.message || "Request failed");
    }
    return payload.data;
  }

  function uuid() {
    if (crypto.randomUUID) return crypto.randomUUID();
    return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, (c) => {
      const r = (Math.random() * 16) | 0;
      const v = c === "x" ? r : (r & 0x3) | 0x8;
      return v.toString(16);
    });
  }

  function showAuth() {
    $("screenAuth").classList.remove("hidden");
    $("screenApp").classList.add("hidden");
    $("btnLogout").classList.add("hidden");
  }

  function showApp() {
    $("screenAuth").classList.add("hidden");
    $("screenApp").classList.remove("hidden");
    $("btnLogout").classList.remove("hidden");
  }

  async function loadDashboard() {
    const me = await api("GET", "/api/v1/auth/me");
    const wallet = await api("GET", "/api/v1/wallets/me");
    $("balanceAmt").textContent = Number(wallet.balance).toLocaleString("en-IN", {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    });
    $("userLine").textContent = me.email + " · " + (me.status || "ACTIVE");
    $("userIdLine").textContent = "Your user ID (share for transfers): " + me.userId;

    const page = await api("GET", "/api/v1/transactions?size=15");
    const rows = page.content || [];
    const body = $("txnBody");
    body.innerHTML = "";
    $("txnEmpty").classList.toggle("hidden", rows.length > 0);
    for (const tx of rows) {
      const tr = document.createElement("tr");
      const when = tx.occurredAt ? new Date(tx.occurredAt).toLocaleString() : "—";
      const dirClass = tx.direction === "CREDIT" ? "dir-credit" : "dir-debit";
      const sign = tx.direction === "CREDIT" ? "+" : "−";
      tr.innerHTML =
        "<td>" + when + "</td>" +
        "<td>" + (tx.type || "") + " <span class='" + dirClass + "'>" + sign + "</span></td>" +
        "<td>₹ " + Number(tx.amount).toFixed(2) + "</td>" +
        "<td>" + (tx.status || "") + "</td>";
      body.appendChild(tr);
    }
  }

  document.querySelectorAll(".tabs button").forEach((btn) => {
    btn.addEventListener("click", () => {
      document.querySelectorAll(".tabs button").forEach((b) => b.classList.remove("active"));
      btn.classList.add("active");
      const tab = btn.dataset.tab;
      $("formLogin").classList.toggle("hidden", tab !== "login");
      $("formRegister").classList.toggle("hidden", tab !== "register");
      hideMsg(authMsg);
    });
  });

  $("formLogin").addEventListener("submit", async (e) => {
    e.preventDefault();
    hideMsg(authMsg);
    try {
      const data = await api("POST", "/api/v1/auth/login", {
        email: $("loginEmail").value.trim(),
        password: $("loginPassword").value,
      });
      saveSession(data);
      showApp();
      await loadDashboard();
    } catch (err) {
      showMsg(authMsg, err.message, false);
    }
  });

  $("formRegister").addEventListener("submit", async (e) => {
    e.preventDefault();
    hideMsg(authMsg);
    try {
      const data = await api(
        "POST",
        "/api/v1/auth/register",
        {
          fullName: $("regName").value.trim(),
          email: $("regEmail").value.trim(),
          phone: $("regPhone").value.trim(),
          password: $("regPassword").value,
        },
        201
      );
      saveSession(data);
      showApp();
      await loadDashboard();
    } catch (err) {
      showMsg(authMsg, err.message, false);
    }
  });

  $("formTopup").addEventListener("submit", async (e) => {
    e.preventDefault();
    hideMsg(topupMsg);
    try {
      const amount = $("topupAmount").value.trim();
      const result = await api("POST", "/api/v1/payments/topup", {
        amount,
        idempotencyKey: "ui-topup-" + uuid(),
        note: "web ui",
      });
      showMsg(topupMsg, "Top-up " + result.status + " · payment " + result.paymentId, result.status === "SUCCESS");
      await loadDashboard();
    } catch (err) {
      showMsg(topupMsg, err.message, false);
    }
  });

  $("formTransfer").addEventListener("submit", async (e) => {
    e.preventDefault();
    hideMsg(xferMsg);
    try {
      const result = await api("POST", "/api/v1/payments/transfer", {
        toUserId: $("xferUserId").value.trim(),
        amount: $("xferAmount").value.trim(),
        idempotencyKey: "ui-xfer-" + uuid(),
        note: "web ui",
      });
      showMsg(xferMsg, "Transfer " + result.status, result.status === "SUCCESS");
      await loadDashboard();
    } catch (err) {
      showMsg(xferMsg, err.message, false);
    }
  });

  $("btnRefresh").addEventListener("click", () => {
    loadDashboard().catch((err) => showMsg(authMsg, err.message, false));
  });

  $("btnLogout").addEventListener("click", async () => {
    const refresh = sessionStorage.getItem("refreshToken");
    try {
      if (refresh) await api("POST", "/api/v1/auth/logout", { refreshToken: refresh });
    } catch (_) {
      /* ignore */
    }
    clearSession();
    showAuth();
  });

  if (token()) {
    showApp();
    loadDashboard().catch(() => {
      clearSession();
      showAuth();
    });
  } else {
    showAuth();
  }
})();