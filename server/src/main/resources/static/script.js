(function () {
  var TOKEN_KEY = "chargeflow_jwt";

  function getToken() {
    return localStorage.getItem(TOKEN_KEY);
  }

  function setToken(token) {
    localStorage.setItem(TOKEN_KEY, token);
  }

  function clearToken() {
    localStorage.removeItem(TOKEN_KEY);
  }

  function redirectToLogin() {
    var current = window.location.pathname.split("/").pop() || "discover.html";
    var returnTo = encodeURIComponent(current);
    window.location.href = "login.html?returnTo=" + returnTo;
  }

  function requireAuth() {
    if (!getToken()) {
      redirectToLogin();
      return false;
    }
    return true;
  }

  async function authFetch(url, options) {
    var opts = options ? Object.assign({}, options) : {};
    var headers = Object.assign({}, opts.headers || {});
    var token = getToken();
    if (token) {
      headers.Authorization = "Bearer " + token;
    }
    opts.headers = headers;

    var response = await fetch(url, opts);
    if (response.status === 401) {
      clearToken();
      redirectToLogin();
      throw new Error("Unauthorized");
    }
    return response;
  }

  async function login(email, password) {
    var response = await fetch("/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email: email, password: password })
    });

    var raw = await response.text();
    var data = null;
    try { data = raw ? JSON.parse(raw) : null; } catch (e) { data = null; }

    if (!response.ok) {
      var message = (data && (data.message || data.error)) || "Login failed.";
      throw new Error(message);
    }

    if (!data || !data.token) {
      throw new Error("Login response did not include a token.");
    }

    setToken(data.token);
    return data;
  }

  function logout() {
    clearToken();
    window.location.href = "login.html";
  }

  function bindLogoutButton() {
    var btn = document.getElementById("logout-btn");
    if (!btn) return;
    btn.addEventListener("click", function () {
      logout();
    });
  }

  function initLoginForm() {
    var form = document.getElementById("login-form");
    if (!form) return;

    var emailInput = document.getElementById("username");
    var passwordInput = document.getElementById("password");
    var message = document.getElementById("login-message");

    form.addEventListener("submit", async function (event) {
      event.preventDefault();
      if (message) {
        message.textContent = "";
        message.className = "login-message";
      }

      try {
        await login((emailInput.value || "").trim(), passwordInput.value || "");
        var params = new URLSearchParams(window.location.search);
        var returnTo = params.get("returnTo") || "discover.html";
        window.location.href = returnTo;
      } catch (error) {
        if (message) {
          message.textContent = error.message || "Login failed.";
          message.className = "login-message error";
        }
      }
    });
  }

  function init() {
    initLoginForm();
    bindLogoutButton();
  }

  window.Auth = {
    getToken: getToken,
    setToken: setToken,
    clearToken: clearToken,
    requireAuth: requireAuth,
    authFetch: authFetch,
    login: login,
    logout: logout
  };

  document.addEventListener("DOMContentLoaded", init);
})();
