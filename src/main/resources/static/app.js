const BASE_URL = 'http://localhost:8080';

let stompClient = null;
let currentConversationId = null;
let currentConversation = null;
let currentUserId = null;
let currentUsername = null;
let currentSubscription = null;
let reconnectAttempts = 0;
let maxReconnectAttempts = 5;
let isConnecting = false;
let isConnected = false;
let pendingSubscription = null;
let connectionPromise = null;
let currentTab = 'chats';

// DOM Elements
const authSection = document.getElementById('auth-section');
const appSection = document.getElementById('app-section');
const loginForm = document.getElementById('login-form');
const signupForm = document.getElementById('signup-form');
const logoutBtn = document.getElementById('logout-btn');
const conversationsList = document.getElementById('conversations-list');
const chatMessages = document.getElementById('chat-messages');
const messageInput = document.getElementById('message-input');
const sendBtn = document.getElementById('send-btn');
const connectionStatus = document.getElementById('connection-status');
const reconnectBtn = document.getElementById('reconnect-btn');
const chatHeader = document.getElementById('chat-header');
const chatTitle = document.getElementById('chat-title');
const messageInputArea = document.getElementById('message-input-area');

// Update connection status UI
function updateConnectionStatus(status) {
    if (connectionStatus) {
        if (status === 'connected') {
            connectionStatus.textContent = '✓ Connected';
            connectionStatus.className = 'connected';
            if (reconnectBtn) reconnectBtn.style.display = 'none';
            isConnected = true;
            isConnecting = false;
        } else if (status === 'connecting') {
            connectionStatus.textContent = '⟳ Connecting...';
            connectionStatus.className = 'connecting';
            if (reconnectBtn) reconnectBtn.style.display = 'none';
            isConnected = false;
            isConnecting = true;
        } else {
            connectionStatus.textContent = '✗ Disconnected';
            connectionStatus.className = 'disconnected';
            if (reconnectBtn) reconnectBtn.style.display = 'block';
            isConnected = false;
            isConnecting = false;
        }
    }
}


// Check if user is logged in
function checkAuth() {
    // Get current session user instead of hardcoded ID
    fetch(`${BASE_URL}/api/users/current`, {
        method: 'GET',
        credentials: 'include'
    })
        .then(response => {
            if (response.ok) {
                return response.json();
            } else {
                throw new Error('Not authenticated');
            }
        })
        .then(res => {
            if (res.success && res.data) {
                currentUserId = res.data.id;
                currentUsername = res.data.username;
                console.log('Authenticated as:', currentUsername, 'ID:', currentUserId);
                showApp();
                loadConversations();
            } else {
                showAuth();
            }
        })
        .catch(err => {
            console.error('Auth check failed:', err);
            showAuth();
        });
}


// Show auth section
function showAuth() {
    if (authSection) authSection.style.display = 'block';
    if (appSection) appSection.style.display = 'none';
}

// Show app section
function showApp() {
    if (authSection) authSection.style.display = 'none';
    if (appSection) appSection.style.display = 'flex';
    document.getElementById('current-user').textContent = currentUsername || 'User';
    connectWebSocket();
}

// WebSocket connection
function connectWebSocket() {
    if (isConnected && stompClient && stompClient.connected) {
        return Promise.resolve();
    }

    if (isConnecting && connectionPromise) {
        return connectionPromise;
    }

    connectionPromise = new Promise((resolve, reject) => {
        try {
            if (stompClient) {
                try {
                    if (typeof stompClient.disconnect === 'function') {
                        stompClient.disconnect();
                    }
                } catch (e) {
                    console.warn('Error disconnecting previous stompClient', e);
                }
                stompClient = null;
            }

            if (typeof SockJS === 'undefined') {
                updateConnectionStatus('disconnected');
                reject(new Error('SockJS not loaded'));
                return;
            }

            const StompLib = window.Stomp || window.StompJs?.Stomp;
            if (!StompLib) {
                updateConnectionStatus('disconnected');
                reject(new Error('Stomp not loaded'));
                return;
            }

            updateConnectionStatus('connecting');

            const socket = new SockJS(`${BASE_URL}/ws`);
            stompClient = StompLib.over(socket);
            stompClient.debug = () => {};

            stompClient.heartbeat.outgoing = 10000;
            stompClient.heartbeat.incoming = 10000;

            const connectionTimeout = setTimeout(() => {
                console.error('Connection timeout');
                updateConnectionStatus('disconnected');
                connectionPromise = null;
                isConnecting = false;
                if (stompClient) {
                    try {
                        stompClient.disconnect();
                    } catch (e) {}
                    stompClient = null;
                }
                reject(new Error('Connection timeout'));
            }, 5000);

            stompClient.connect(
                {},
                (frame) => {
                    clearTimeout(connectionTimeout);
                    console.log('✓ STOMP connected');
                    reconnectAttempts = 0;
                    updateConnectionStatus('connected');

                    if (pendingSubscription) {
                        subscribeToConversation(pendingSubscription);
                        pendingSubscription = null;
                    }

                    resolve();
                },
                (error) => {
                    clearTimeout(connectionTimeout);
                    console.error('STOMP error:', error);
                    updateConnectionStatus('disconnected');
                    connectionPromise = null;

                    if (reconnectAttempts < maxReconnectAttempts) {
                        reconnectAttempts++;
                        const delay = Math.min(1000 * Math.pow(2, reconnectAttempts), 30000);
                        setTimeout(() => {
                            connectWebSocket().then(resolve).catch(reject);
                        }, delay);
                    } else {
                        reject(new Error('Max reconnect attempts reached'));
                    }
                }
            );

        } catch (error) {
            console.error('WebSocket setup failed:', error);
            updateConnectionStatus('disconnected');
            connectionPromise = null;
            reject(error);
        }
    });

    return connectionPromise;
}

// Subscribe to a conversation
function subscribeToConversation(conversationId) {
    if (currentSubscription) {
        try {
            currentSubscription.unsubscribe();
        } catch (e) {}
        currentSubscription = null;
    }

    if (!isConnected || !stompClient || !stompClient.connected) {
        pendingSubscription = conversationId;
        if (!isConnecting) {
            connectWebSocket().catch(err => {
                console.error('Failed to connect:', err);
            });
        }
        return;
    }

    try {
        currentSubscription = stompClient.subscribe(
            `/topic/conversation/${conversationId}`,
            (message) => {
                try {
                    const msg = JSON.parse(message.body);
                    displayMessage(msg);
                } catch (e) {
                    console.error('Error parsing message:', e);
                }
            }
        );
    } catch (e) {
        console.error('Error subscribing:', e);
    }
}

// Display a message
function displayMessage(msg) {
    if (!chatMessages) return;

    const div = document.createElement('div');
    div.className = `message ${msg.senderId === currentUserId ? 'sent' : 'received'}`;

    let html = '';
    if (msg.senderId !== currentUserId && msg.senderName) {
        html += `<div class="message-sender">${msg.senderName}</div>`;
    }
    html += `<div class="message-content">${escapeHtml(msg.content)}</div>`;

    div.innerHTML = html;
    chatMessages.appendChild(div);
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

// Escape HTML
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// Login
if (loginForm) {
    loginForm.addEventListener('submit', (e) => {
        e.preventDefault();
        const email = document.getElementById('login-email').value;
        const phone = document.getElementById('login-phone').value;
        const password = document.getElementById('login-password').value;

        fetch(`${BASE_URL}/api/users/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({ email, phone, password })
        })
            .then(response => response.json())
            .then(res => {
                if (res.success) {
                    currentUserId = res.data.id;
                    currentUsername = res.data.username;
                    showApp();
                    loadConversations();
                } else {
                    alert('Login failed: ' + res.message);
                }
            })
            .catch(err => alert('Login failed: ' + err.message));
    });
}

// Sign Up
if (signupForm) {
    signupForm.addEventListener('submit', (e) => {
        e.preventDefault();
        const username = document.getElementById('signup-username').value;
        const email = document.getElementById('signup-email').value;
        const phone = document.getElementById('signup-phone').value;
        const password = document.getElementById('signup-password').value;

        fetch(`${BASE_URL}/api/users/signUp`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({ username, email, phone, password })
        })
            .then(response => response.json())
            .then(res => {
                if (res.success) {
                    alert('Sign up successful! Logging you in...');
                    currentUserId = res.data.id;
                    currentUsername = res.data.username;
                    showApp();
                    loadConversations();
                } else {
                    alert('Sign up failed: ' + res.message);
                }
            })
            .catch(err => alert('Sign up failed: ' + err.message));
    });
}

// Logout
if (logoutBtn) {
    logoutBtn.addEventListener('click', () => {
        fetch(`${BASE_URL}/api/users/logout`, {
            method: 'POST',
            credentials: 'include'
        })
            .then(() => {
                currentUserId = null;
                currentUsername = null;
                currentConversationId = null;
                pendingSubscription = null;
                if (stompClient) {
                    stompClient.disconnect();
                    stompClient = null;
                }
                isConnected = false;
                isConnecting = false;
                showAuth();
            })
            .catch(err => alert('Logout failed: ' + err.message));
    });
}

// Manual reconnect
if (reconnectBtn) {
    reconnectBtn.addEventListener('click', () => {
        reconnectAttempts = 0;
        connectionPromise = null;
        isConnecting = false;
        isConnected = false;
        connectWebSocket().catch(err => {
            console.error('Manual reconnect failed:', err);
        });
    });
}

// Tabs
document.querySelectorAll('.tab').forEach(tab => {
    tab.addEventListener('click', () => {
        document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
        tab.classList.add('active');
        currentTab = tab.dataset.tab;
        loadConversations();
    });
});

// Load conversations
function loadConversations() {
    let endpoint = '';
    if (currentTab === 'chats') endpoint = '/api/conversations/chats';
    else if (currentTab === 'contacts') endpoint = '/api/conversations/contacts';
    else if (currentTab === 'groups') endpoint = '/api/conversations/user';

    fetch(`${BASE_URL}${endpoint}`, {
        credentials: 'include'
    })
        .then(response => response.json())
        .then(res => {
            if (res.success && conversationsList) {
                conversationsList.innerHTML = '';
                if (res.data.length === 0) {
                    conversationsList.innerHTML = '<div class="empty-state">No conversations yet</div>';
                    return;
                }
                res.data.forEach((conv) => {
                    const div = document.createElement('div');
                    div.className = 'conversation-item';
                    div.innerHTML = `
                            <div class="conversation-name">${conv.name || 'Unnamed'}</div>
                            <div class="conversation-type">${conv.type} • ${conv.participantCount} members</div>
                        `;
                    div.addEventListener('click', () => selectConversation(conv, div));
                    conversationsList.appendChild(div);
                });
            }
        })
        .catch(err => console.error('Load conversations failed:', err));
}

// Select a conversation
function selectConversation(conv, element) {
    document.querySelectorAll('.conversation-item').forEach(el => {
        el.classList.remove('active');
    });

    if (element) {
        element.classList.add('active');
    }

    currentConversationId = conv.id;
    currentConversation = conv;
    chatTitle.textContent = conv.name || 'Unnamed';
    chatHeader.style.display = 'flex';
    messageInputArea.style.display = 'flex';

    loadMessages(conv.id);
    subscribeToConversation(conv.id);
}

// Load messages
function loadMessages(conversationId) {
    fetch(`${BASE_URL}/api/conversations/${conversationId}/messages`, {
        credentials: 'include'
    })
        .then(response => response.json())
        .then(res => {
            if (res.success && chatMessages) {
                chatMessages.innerHTML = '';
                res.data.forEach((msg) => {
                    displayMessage({
                        senderId: msg.senderId,
                        content: msg.content,
                        senderName: msg.senderId === currentUserId ? 'You' : `User ${msg.senderId}`
                    });
                });
            }
        })
        .catch(err => console.error('Load messages failed:', err));
}

// Send message
if (sendBtn && messageInput) {
    sendBtn.addEventListener('click', sendMessage);
    messageInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            sendMessage();
        }
    });
}

function sendMessage() {
    if (!currentConversationId) {
        alert('Select a conversation first');
        return;
    }
    console.log(currentUserId)
    const content = messageInput.value.trim();
    if (!content) return;

    if (isConnected && stompClient && stompClient.connected) {
        stompClient.send('/app/chat.send', {}, JSON.stringify({
            conversationId: currentConversationId,
            senderId: currentUserId,
            content: content,
            timestamp: Date.now()
        }));
        messageInput.value = '';
    } else {
        alert('WebSocket not connected. Please wait...');
        connectWebSocket().then(() => {
            if (content) {
                sendMessage();
            }
        }).catch(err => {
            alert('Failed to connect. Please try again.');
        });
    }
}

// Create private conversation
document.getElementById('create-private-btn').addEventListener('click', () => {
    const otherUserId = prompt('Enter other user ID:');
    const customName = prompt('Custom name (optional):');

    if (otherUserId) {
        fetch(`${BASE_URL}/api/conversations/private`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({
                otherUserId: parseInt(otherUserId),
                customName: customName || ''
            })
        })
            .then(response => response.json())
            .then(res => {
                if (res.success) {
                    alert('Private chat created!');
                    loadConversations();
                } else {
                    alert('Failed: ' + res.message);
                }
            })
            .catch(err => alert('Error: ' + err.message));
    }
});

// Create group conversation
document.getElementById('create-group-btn').addEventListener('click', () => {
    const name = prompt('Group name:');
    if (name) {
        fetch(`${BASE_URL}/api/conversations/group`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({ name })
        })
            .then(response => response.json())
            .then(res => {
                if (res.success) {
                    alert('Group created!');
                    loadConversations();
                } else {
                    alert('Failed: ' + res.message);
                }
            })
            .catch(err => alert('Error: ' + err.message));
    }
});

// Search functionality
let searchTimeout;
document.getElementById('search-query').addEventListener('input', (e) => {
    clearTimeout(searchTimeout);
    const query = e.target.value.trim();

    if (!query) {
        document.getElementById('search-results').innerHTML = '';
        return;
    }

    searchTimeout = setTimeout(() => {
        searchUsersAndGroups(query);
    }, 500);
});

function searchUsersAndGroups(query) {
    const searchResults = document.getElementById('search-results');
    searchResults.innerHTML = '<div style="padding: 10px; color: #65676b;">Searching...</div>';

    // Search users
    fetch(`${BASE_URL}/api/conversations/search/users?query=${encodeURIComponent(query)}`, {
        credentials: 'include'
    })
        .then(response => response.json())
        .then(res => {
            if (res.success) {
                searchResults.innerHTML = '';
                if (res.data.length > 0) {
                    const userHeader = document.createElement('div');
                    userHeader.style.padding = '10px';
                    userHeader.style.fontWeight = 'bold';
                    userHeader.style.color = '#1877f2';
                    userHeader.textContent = 'Users:';
                    searchResults.appendChild(userHeader);

                    res.data.forEach((user) => {
                        const div = document.createElement('div');
                        div.className = 'search-result';
                        div.innerHTML = `<strong>${user.username}</strong><br><small>${user.email}</small>`;
                        div.addEventListener('click', () => {
                            const customName = prompt(`Start chat with ${user.username}?`, user.username);
                            if (customName !== null) {
                                createPrivateChat(user.id, customName);
                            }
                        });
                        searchResults.appendChild(div);
                    });
                }
            }
        })
        .catch(err => console.error('Search users failed:', err));

    // Search groups
    fetch(`${BASE_URL}/api/conversations/search/groups?query=${encodeURIComponent(query)}`, {
        credentials: 'include'
    })
        .then(response => response.json())
        .then(res => {
            if (res.success && res.data.length > 0) {
                const groupHeader = document.createElement('div');
                groupHeader.style.padding = '10px';
                groupHeader.style.fontWeight = 'bold';
                groupHeader.style.color = '#1877f2';
                groupHeader.textContent = 'Groups:';
                searchResults.appendChild(groupHeader);

                res.data.forEach((group) => {
                    const div = document.createElement('div');
                    div.className = 'search-result';
                    div.innerHTML = `<strong>${group.name}</strong><br><small>${group.participantCount} members</small>`;
                    searchResults.appendChild(div);
                });
            }
        })
        .catch(err => console.error('Search groups failed:', err));
}

function createPrivateChat(otherUserId, customName) {
    fetch(`${BASE_URL}/api/conversations/private`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({
            otherUserId: otherUserId,
            customName: customName || ''
        })
    })
        .then(response => response.json())
        .then(res => {
            if (res.success) {
                document.getElementById('search-query').value = '';
                document.getElementById('search-results').innerHTML = '';
                loadConversations();
            } else {
                alert('Failed: ' + res.message);
            }
        })
        .catch(err => alert('Error: ' + err.message));
}

// View participants
document.getElementById('view-participants-btn').addEventListener('click', () => {
    if (!currentConversationId) return;

    fetch(`${BASE_URL}/api/conversations/${currentConversationId}/participants`, {
        credentials: 'include'
    })
        .then(response => response.json())
        .then(res => {
            if (res.success) {
                const participantsList = document.getElementById('participants-list');
                participantsList.innerHTML = '';

                res.data.forEach(p => {
                    const div = document.createElement('div');
                    div.className = 'participant-item';
                    div.innerHTML = `
                            <div class="participant-info">
                                <div class="participant-name">${p.username || 'User ' + p.userId}</div>
                                <div class="participant-role">${p.role} - ${p.status}</div>
                            </div>
                            ${p.userId !== currentUserId ? `<button onclick="removeParticipant(${p.userId})">Remove</button>` : ''}
                        `;
                    participantsList.appendChild(div);
                });

                showModal('participants-modal');
            }
        })
        .catch(err => console.error('Load participants failed:', err));
});

// Add participant
document.getElementById('add-participant-btn').addEventListener('click', () => {
    if (!currentConversationId) return;
    showModal('add-participant-modal');
});

document.getElementById('confirm-add-participant').addEventListener('click', () => {
    const userId = parseInt(document.getElementById('add-participant-id').value);
    const role = document.getElementById('add-participant-role').value;

    if (!userId) {
        alert('Please enter a user ID');
        return;
    }

    fetch(`${BASE_URL}/api/conversations/${currentConversationId}/participants`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ userId, role })
    })
        .then(response => response.json())
        .then(res => {
            if (res.success) {
                alert('Participant added!');
                closeModal('add-participant-modal');
                document.getElementById('add-participant-id').value = '';
            } else {
                alert('Failed: ' + res.message);
            }
        })
        .catch(err => alert('Error: ' + err.message));
});

// Remove participant
window.removeParticipant = function(targetUserId) {
    if (!confirm('Remove this participant?')) return;

    fetch(`${BASE_URL}/api/conversations/${currentConversationId}/participants/${targetUserId}`, {
        method: 'DELETE',
        credentials: 'include'
    })
        .then(response => response.json())
        .then(res => {
            if (res.success) {
                alert('Participant removed!');
                closeModal('participants-modal');
            } else {
                alert('Failed: ' + res.message);
            }
        })
        .catch(err => alert('Error: ' + err.message));
};

// Leave conversation
document.getElementById('leave-conversation-btn').addEventListener('click', () => {
    if (!currentConversationId) return;
    if (!confirm('Are you sure you want to leave this conversation?')) return;

    fetch(`${BASE_URL}/api/conversations/participant`, {
        method: 'DELETE',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({
            conversationId: currentConversationId,
            targetUserId: currentUserId
        })
    })
        .then(response => response.json())
        .then(res => {
            if (res.success) {
                alert('Left conversation');
                currentConversationId = null;
                currentConversation = null;
                chatHeader.style.display = 'none';
                messageInputArea.style.display = 'none';
                chatMessages.innerHTML = '<div class="empty-state">Select a conversation to start chatting</div>';
                loadConversations();
            } else {
                alert('Failed: ' + res.message);
            }
        })
        .catch(err => alert('Error: ' + err.message));
});

// Settings
document.getElementById('settings-btn').addEventListener('click', () => {
    showModal('settings-modal');
});

document.getElementById('update-username-btn').addEventListener('click', () => {
    const username = document.getElementById('settings-username').value.trim();
    if (!username) return;

    fetch(`${BASE_URL}/api/users/username`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ username })
    })
        .then(response => response.json())
        .then(res => {
            if (res.success) {
                alert('Username updated!');
                currentUsername = username;
                document.getElementById('current-user').textContent = username;
                document.getElementById('settings-username').value = '';
            } else {
                alert('Failed: ' + res.message);
            }
        })
        .catch(err => alert('Error: ' + err.message));
});

document.getElementById('update-email-btn').addEventListener('click', () => {
    const email = document.getElementById('settings-email').value.trim();
    if (!email) return;

    fetch(`${BASE_URL}/api/users/email`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ email })
    })
        .then(response => response.json())
        .then(res => {
            if (res.success) {
                alert('Email updated!');
                document.getElementById('settings-email').value = '';
            } else {
                alert('Failed: ' + res.message);
            }
        })
        .catch(err => alert('Error: ' + err.message));
});

document.getElementById('update-phone-btn').addEventListener('click', () => {
    const phone = document.getElementById('settings-phone').value.trim();
    if (!phone) return;

    fetch(`${BASE_URL}/api/users/phone`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ phone })
    })
        .then(response => response.json())
        .then(res => {
            if (res.success) {
                alert('Phone updated!');
                document.getElementById('settings-phone').value = '';
            } else {
                alert('Failed: ' + res.message);
            }
        })
        .catch(err => alert('Error: ' + err.message));
});

document.getElementById('update-password-btn').addEventListener('click', () => {
    const password = document.getElementById('settings-password').value;
    const confirmedPassword = document.getElementById('settings-password-confirm').value;

    if (!password || !confirmedPassword) {
        alert('Please fill in both password fields');
        return;
    }

    if (password !== confirmedPassword) {
        alert('Passwords do not match');
        return;
    }

    fetch(`${BASE_URL}/api/users/password`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ password, confirmedPassword })
    })
        .then(response => response.json())
        .then(res => {
            if (res.success) {
                alert('Password updated!');
                document.getElementById('settings-password').value = '';
                document.getElementById('settings-password-confirm').value = '';
            } else {
                alert('Failed: ' + res.message);
            }
        })
        .catch(err => alert('Error: ' + err.message));
});

// Modal functions
function showModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.add('show');
    }
}

window.closeModal = function(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.remove('show');
    }
};

// Close modal on outside click
document.querySelectorAll('.modal').forEach(modal => {
    modal.addEventListener('click', (e) => {
        if (e.target === modal) {
            modal.classList.remove('show');
        }
    });
});

// Initialize
document.addEventListener('DOMContentLoaded', function() {
    checkAuth();
});
