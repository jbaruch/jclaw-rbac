(function () {
    'use strict';

    // --- State ---
    let authHeader = null;
    let userInfo = null;

    // --- All tool definitions with display metadata ---
    const ALL_TOOLS = [
        { id: 'getCurrentTime', label: 'Get Time',    icon: '\u23F0', minRole: 'ANALYST' },
        { id: 'getWeather',     label: 'Get Weather',  icon: '\u26C5', minRole: 'ANALYST' },
        { id: 'searchCfps',     label: 'Search CFPs',  icon: '\uD83D\uDCC5', minRole: 'ANALYST' },
        { id: 'listUsers',      label: 'List Users',   icon: '\uD83D\uDC65', minRole: 'ADMIN'   },
        { id: 'introspect',     label: 'Introspect',   icon: '\uD83D\uDD27', minRole: 'ADMIN'   }
    ];

    // --- DOM refs ---
    const loginSection     = document.getElementById('login-section');
    const dashboardSection = document.getElementById('dashboard-section');
    const loginForm        = document.getElementById('login-form');
    const loginError       = document.getElementById('login-error');
    const usernameInput    = document.getElementById('username');
    const passwordInput    = document.getElementById('password');
    const userDisplay      = document.getElementById('user-display');
    const bannerUsername   = document.getElementById('banner-username');
    const bannerRole       = document.getElementById('banner-role');
    const personaEmoji     = document.getElementById('persona-emoji');
    const personaName      = document.getElementById('persona-name');
    const chatPersonaName  = document.getElementById('chat-persona-name');
    const personaBanner    = document.getElementById('persona-banner');
    const toolsGrid        = document.getElementById('tools-grid');
    const chatMessages     = document.getElementById('chat-messages');
    const chatForm         = document.getElementById('chat-form');
    const chatInput        = document.getElementById('chat-input');
    const btnSend          = document.getElementById('btn-send');
    const logoutBtn        = document.getElementById('logout-btn');

    // --- Login ---
    loginForm.addEventListener('submit', async function (e) {
        e.preventDefault();
        loginError.textContent = '';

        const user = usernameInput.value.trim();
        const pass = passwordInput.value;

        if (!user || !pass) {
            loginError.textContent = 'Please enter username and password.';
            return;
        }

        authHeader = 'Basic ' + btoa(user + ':' + pass);

        try {
            const resp = await fetch('/api/me', {
                headers: { 'Authorization': authHeader }
            });
            if (!resp.ok) {
                authHeader = null;
                loginError.textContent = 'Invalid credentials. Try again.';
                return;
            }
            userInfo = await resp.json();
            showDashboard();
        } catch (err) {
            authHeader = null;
            loginError.textContent = 'Connection error. Is the server running?';
        }
    });

    // --- Logout ---
    logoutBtn.addEventListener('click', function () {
        authHeader = null;
        userInfo = null;
        chatMessages.innerHTML = '';
        usernameInput.value = '';
        passwordInput.value = '';
        loginError.textContent = '';
        dashboardSection.style.display = 'none';
        loginSection.style.display = 'block';
        usernameInput.focus();
    });

    // --- Dashboard rendering ---
    function showDashboard() {
        loginSection.style.display = 'none';
        dashboardSection.style.display = 'block';

        // User info
        userDisplay.textContent = userInfo.username;
        bannerUsername.textContent = userInfo.username;

        // Persona info
        var pName = userInfo.persona || 'JClaw';
        var pEmoji = userInfo.personaEmoji || '\uD83E\uDD9E';
        personaEmoji.textContent = pEmoji;
        personaName.textContent = pName;
        chatPersonaName.textContent = pName;

        // Apply persona-specific color class to the banner
        personaBanner.className = 'persona-banner persona-' + userInfo.username;

        // Primary role (highest)
        var primaryRole = 'VIEWER';
        if (userInfo.roles.indexOf('ROLE_ADMIN') !== -1) primaryRole = 'ADMIN';
        else if (userInfo.roles.indexOf('ROLE_ANALYST') !== -1) primaryRole = 'ANALYST';

        bannerRole.textContent = primaryRole;
        bannerRole.className = 'role-badge role-' + primaryRole.toLowerCase();

        // Tool cards
        renderTools();

        chatInput.focus();
    }

    function renderTools() {
        toolsGrid.innerHTML = '';
        var availableTools = userInfo.tools || [];

        ALL_TOOLS.forEach(function (tool) {
            var isAvailable = availableTools.indexOf(tool.id) !== -1;
            var card = document.createElement('div');
            card.className = 'tool-card ' + (isAvailable ? 'available' : 'locked');

            card.innerHTML =
                '<span class="tool-icon">' + tool.icon + '</span>' +
                '<div class="tool-name">' + tool.label + '</div>' +
                '<div class="tool-access">' +
                    (isAvailable ? '\u2713 Available' : '\uD83D\uDD12 ' + tool.minRole + '+') +
                '</div>';

            toolsGrid.appendChild(card);
        });
    }

    // --- Chat ---
    chatForm.addEventListener('submit', async function (e) {
        e.preventDefault();
        var msg = chatInput.value.trim();
        if (!msg) return;

        appendBubble(msg, 'user');
        chatInput.value = '';
        btnSend.disabled = true;

        // Show typing indicator
        var typing = document.createElement('div');
        typing.className = 'typing-indicator';
        typing.innerHTML = '<span></span><span></span><span></span>';
        chatMessages.appendChild(typing);
        scrollToBottom();

        try {
            var resp = await fetch('/chat', {
                method: 'POST',
                headers: {
                    'Authorization': authHeader,
                    'Content-Type': 'text/plain'
                },
                body: msg
            });

            // Remove typing indicator
            if (typing.parentNode) typing.parentNode.removeChild(typing);

            if (!resp.ok) {
                var errText = await resp.text();
                appendBubble('Error ' + resp.status + ': ' + (errText || 'Request failed'), 'agent error');
            } else {
                var text = await resp.text();
                appendBubble(text, 'agent');
            }
        } catch (err) {
            if (typing.parentNode) typing.parentNode.removeChild(typing);
            appendBubble('Network error: ' + err.message, 'agent error');
        }

        btnSend.disabled = false;
        chatInput.focus();
    });

    function appendBubble(text, classes) {
        var bubble = document.createElement('div');
        bubble.className = 'chat-bubble ' + classes;
        bubble.textContent = text;
        chatMessages.appendChild(bubble);
        scrollToBottom();
    }

    function scrollToBottom() {
        chatMessages.scrollTop = chatMessages.scrollHeight;
    }
})();
