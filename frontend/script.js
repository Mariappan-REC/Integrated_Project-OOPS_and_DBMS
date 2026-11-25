/**
 * CommitShield 2.0 - Frontend Interaction & API Controller
 * Connects vanilla JS UI to the lightweight Java HTTP REST API and MySQL database.
 */

const API_BASE = '/api';

// Application State
const state = {
    tasks: [],
    unfinishedTasks: [],
    dependencies: [],
    history: [],
    workload: null,
    availableHours: 6.0,
    activeView: 'dashboard',
    selectedTaskIdForStatus: null,
    selectedTaskIdForEdit: null,
    selectedTaskIdForDelete: null
};

// DOM Elements
const elements = {
    navItems: document.querySelectorAll('.nav-item'),
    viewSections: document.querySelectorAll('.view-section'),
    pageTitle: document.getElementById('page-title'),
    pageSubtitle: document.getElementById('page-subtitle'),
    btnRefresh: document.getElementById('btn-refresh'),
    btnQuickCreate: document.getElementById('btn-quick-create'),
    backendStatus: document.getElementById('backend-status'),
    toastContainer: document.getElementById('toast-container'),

    // Dashboard Metrics
    metricTotal: document.getElementById('metric-total'),
    metricPending: document.getElementById('metric-pending'),
    metricInProgress: document.getElementById('metric-inprogress'),
    metricCompleted: document.getElementById('metric-completed'),
    metricUnfinishedHours: document.getElementById('metric-unfinished-hours'),
    metricDeficit: document.getElementById('metric-deficit'),
    metricDeficitStatus: document.getElementById('metric-deficit-status'),
    cardDeficitSummary: document.getElementById('card-deficit-summary'),
    priorityMetersContainer: document.getElementById('priority-meters-container'),
    tableUrgentTasksTbody: document.querySelector('#table-urgent-tasks tbody'),

    // All Tasks
    tableAllTasksTbody: document.getElementById('all-tasks-tbody'),
    taskSearchInput: document.getElementById('task-search-input'),
    filterPriority: document.getElementById('filter-priority'),
    filterStatus: document.getElementById('filter-status'),

    // Create Task Form
    createTaskForm: document.getElementById('create-task-form'),
    createTitle: document.getElementById('create-title'),
    createHours: document.getElementById('create-hours'),
    createDeadline: document.getElementById('create-deadline'),
    createPriority: document.getElementById('create-priority'),
    createStatus: document.getElementById('create-status'),

    // Workload Analysis
    inputAvailableHours: document.getElementById('input-available-hours'),
    btnRecalculateWorkload: document.getElementById('btn-recalculate-workload'),
    conflictAlertBanner: document.getElementById('conflict-alert-banner'),
    wlUnfinished: document.getElementById('wl-unfinished'),
    wlAvailable: document.getElementById('wl-available'),
    wlDeficit: document.getElementById('wl-deficit'),
    wlStatus: document.getElementById('wl-status'),
    workloadPriorityMeters: document.getElementById('workload-priority-meters'),
    tableUnfinishedQueueTbody: document.querySelector('#table-unfinished-queue tbody'),

    // Dependencies
    addDependencyForm: document.getElementById('add-dependency-form'),
    depTaskId: document.getElementById('dep-task-id'),
    depPrereqId: document.getElementById('dep-prereq-id'),
    depCountBadge: document.getElementById('dep-count-badge'),
    dependenciesTbody: document.getElementById('dependencies-tbody'),

    // History
    historyCountBadge: document.getElementById('history-count-badge'),
    historyTbody: document.getElementById('history-tbody'),

    // Modals
    statusModal: document.getElementById('status-modal'),
    modalTaskTitle: document.getElementById('modal-task-title'),
    modalTaskId: document.getElementById('modal-task-id'),
    modalStatusSelect: document.getElementById('modal-status-select'),
    btnCloseStatusModal: document.getElementById('btn-close-status-modal'),
    btnCancelStatusModal: document.getElementById('btn-cancel-status-modal'),
    btnConfirmStatusUpdate: document.getElementById('btn-confirm-status-update'),

    editModal: document.getElementById('edit-modal'),
    editTaskId: document.getElementById('edit-task-id'),
    editTaskTitle: document.getElementById('edit-task-title'),
    editTaskHours: document.getElementById('edit-task-hours'),
    btnCloseEditModal: document.getElementById('btn-close-edit-modal'),
    btnCancelEditModal: document.getElementById('btn-cancel-edit-modal'),
    btnConfirmEditTask: document.getElementById('btn-confirm-edit-task'),

    deleteModal: document.getElementById('delete-modal'),
    deleteTaskTitle: document.getElementById('delete-task-title'),
    deleteTaskId: document.getElementById('delete-task-id'),
    btnCloseDeleteModal: document.getElementById('btn-close-delete-modal'),
    btnCancelDeleteModal: document.getElementById('btn-cancel-delete-modal'),
    btnConfirmDelete: document.getElementById('btn-confirm-delete')
};

// =========================================================================
// Initialization & Navigation
// =========================================================================

document.addEventListener('DOMContentLoaded', () => {
    initNavigation();
    initEventListeners();
    setDefaultDeadline();
    fetchAllData();
});

function initNavigation() {
    elements.navItems.forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            const view = item.getAttribute('data-view');
            switchView(view);
        });
    });

    // Also support internal anchor links with data-view
    document.querySelectorAll('[data-view]').forEach(link => {
        if (!link.classList.contains('nav-item')) {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                const view = link.getAttribute('data-view');
                switchView(view);
            });
        }
    });

    if (elements.btnQuickCreate) {
        elements.btnQuickCreate.addEventListener('click', () => {
            switchView('create-task');
        });
    }
}

function switchView(viewName) {
    state.activeView = viewName;

    // Update Sidebar Navigation state
    elements.navItems.forEach(item => {
        if (item.getAttribute('data-view') === viewName) {
            item.classList.add('active');
        } else {
            item.classList.remove('active');
        }
    });

    // Update Visible Section
    elements.viewSections.forEach(section => {
        if (section.id === `view-${viewName}`) {
            section.classList.add('active');
        } else {
            section.classList.remove('active');
        }
    });

    // Update Header
    const titles = {
        'dashboard': { title: 'Executive Dashboard', subtitle: 'Real-time task scheduling and conflict detection' },
        'tasks': { title: 'Task Repository', subtitle: 'Manage persistent tasks stored in MySQL database' },
        'create-task': { title: 'Create Task', subtitle: 'Insert new task record with strict schema validation' },
        'workload': { title: 'Workload & Conflict Analysis', subtitle: 'Calculates capacity deficits and prioritizes unfinished tasks' },
        'dependencies': { title: 'Relational Dependencies', subtitle: 'Manage prerequisite links with composite primary keys' },
        'history': { title: 'Status Audit Trail', subtitle: 'Immutable log of status transitions captured in task_history' }
    };

    if (titles[viewName]) {
        elements.pageTitle.textContent = titles[viewName].title;
        elements.pageSubtitle.textContent = titles[viewName].subtitle;
    }

    // Refresh targeted dropdowns if entering dependencies
    if (viewName === 'dependencies') {
        populateDependencyDropdowns();
    }
}

function setDefaultDeadline() {
    const now = new Date();
    now.setDate(now.getDate() + 7);
    now.setHours(18, 0, 0, 0);

    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');

    if (elements.createDeadline) {
        elements.createDeadline.value = `${year}-${month}-${day}T${hours}:${minutes}`;
    }
}

// =========================================================================
// Event Listeners
// =========================================================================

function initEventListeners() {
    // Refresh button
    if (elements.btnRefresh) {
        elements.btnRefresh.addEventListener('click', () => {
            fetchAllData();
            showToast('Refreshed live data from MySQL', 'info');
        });
    }

    // Task Search & Filters
    if (elements.taskSearchInput) {
        elements.taskSearchInput.addEventListener('input', renderAllTasksTable);
    }
    if (elements.filterPriority) {
        elements.filterPriority.addEventListener('change', renderAllTasksTable);
    }
    if (elements.filterStatus) {
        elements.filterStatus.addEventListener('change', renderAllTasksTable);
    }

    // Create Task Form
    if (elements.createTaskForm) {
        elements.createTaskForm.addEventListener('submit', handleCreateTask);
    }

    // Recalculate Workload
    if (elements.btnRecalculateWorkload) {
        elements.btnRecalculateWorkload.addEventListener('click', () => {
            const val = parseFloat(elements.inputAvailableHours.value);
            if (!isNaN(val) && val >= 0) {
                state.availableHours = val;
                fetchWorkload(val);
            } else {
                showToast('Please enter a valid non-negative number for available hours.', 'error');
            }
        });
    }

    // Add Dependency Form
    if (elements.addDependencyForm) {
        elements.addDependencyForm.addEventListener('submit', handleAddDependency);
    }

    // Modals
    if (elements.btnCloseStatusModal) elements.btnCloseStatusModal.addEventListener('click', closeStatusModal);
    if (elements.btnCancelStatusModal) elements.btnCancelStatusModal.addEventListener('click', closeStatusModal);
    if (elements.btnConfirmStatusUpdate) elements.btnConfirmStatusUpdate.addEventListener('click', handleConfirmStatusUpdate);

    if (elements.btnCloseEditModal) elements.btnCloseEditModal.addEventListener('click', closeEditModal);
    if (elements.btnCancelEditModal) elements.btnCancelEditModal.addEventListener('click', closeEditModal);
    if (elements.btnConfirmEditTask) elements.btnConfirmEditTask.addEventListener('click', handleConfirmEditTask);

    if (elements.btnCloseDeleteModal) elements.btnCloseDeleteModal.addEventListener('click', closeDeleteModal);
    if (elements.btnCancelDeleteModal) elements.btnCancelDeleteModal.addEventListener('click', closeDeleteModal);
    if (elements.btnConfirmDelete) elements.btnConfirmDelete.addEventListener('click', handleConfirmDelete);
}

// =========================================================================
// API Data Fetching
// =========================================================================

async function fetchAllData() {
    try {
        await Promise.all([
            fetchTasks(),
            fetchUnfinishedTasks(),
            fetchWorkload(state.availableHours),
            fetchDependencies(),
            fetchHistory()
        ]);
        setBackendStatus(true, 'Connected to MySQL');
    } catch (err) {
        console.error('API Fetch Error:', err);
        setBackendStatus(false, 'Database Connection Issue');
        showToast('Unable to connect to MySQL backend. Ensure DB environment variables are set and server is running.', 'error');
    }
}

async function fetchTasks() {
    const res = await fetch(`${API_BASE}/tasks`);
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    state.tasks = await res.json();
    renderDashboardMetrics();
    renderAllTasksTable();
    populateDependencyDropdowns();
}

async function fetchUnfinishedTasks() {
    const res = await fetch(`${API_BASE}/tasks/unfinished`);
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    state.unfinishedTasks = await res.json();
    renderUrgentTasks();
    renderUnfinishedQueue();
}

async function fetchWorkload(availableHours = 6.0) {
    const res = await fetch(`${API_BASE}/workload?availableHours=${availableHours}`);
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    state.workload = await res.json();
    renderWorkloadView();
    renderDashboardMetrics();
}

async function fetchDependencies() {
    const res = await fetch(`${API_BASE}/dependencies`);
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    state.dependencies = await res.json();
    renderDependenciesTable();
}

async function fetchHistory() {
    const res = await fetch(`${API_BASE}/history`);
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    state.history = await res.json();
    renderHistoryTable();
}

// =========================================================================
// Rendering Functions
// =========================================================================

function renderDashboardMetrics() {
    const total = state.tasks.length;
    const pending = state.tasks.filter(t => t.status === 'PENDING').length;
    const inProgress = state.tasks.filter(t => t.status === 'IN_PROGRESS').length;
    const completed = state.tasks.filter(t => t.status === 'COMPLETED').length;

    if (elements.metricTotal) elements.metricTotal.textContent = total;
    if (elements.metricPending) elements.metricPending.textContent = pending;
    if (elements.metricInProgress) elements.metricInProgress.textContent = inProgress;
    if (elements.metricCompleted) elements.metricCompleted.textContent = completed;

    if (state.workload) {
        if (elements.metricUnfinishedHours) {
            elements.metricUnfinishedHours.textContent = `${state.workload.unfinishedHours.toFixed(2)}h`;
        }
        if (elements.metricDeficit) {
            elements.metricDeficit.textContent = `${state.workload.deficit.toFixed(2)}h`;
        }
        if (elements.metricDeficitStatus) {
            elements.metricDeficitStatus.textContent = state.workload.hasConflict
                ? `Conflict: ${state.workload.deficit.toFixed(2)}h deficit`
                : 'Capacity sufficient';
        }
        if (elements.cardDeficitSummary) {
            if (state.workload.hasConflict) {
                elements.cardDeficitSummary.classList.add('has-conflict');
            } else {
                elements.cardDeficitSummary.classList.remove('has-conflict');
            }
        }

        renderPriorityMeters(elements.priorityMetersContainer, state.workload.priorityWorkload, state.workload.unfinishedHours);
    }
}

function renderPriorityMeters(container, priorityWorkload, totalHours) {
    if (!container || !priorityWorkload) return;

    const priorities = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'];
    let html = '';

    priorities.forEach(p => {
        const hours = priorityWorkload[p] || 0.0;
        const percent = totalHours > 0 ? ((hours / totalHours) * 100).toFixed(1) : 0;
        const fillClass = `fill-${p.toLowerCase()}`;

        html += `
            <div class="meter-item">
                <div class="meter-header">
                    <span class="meter-label">${p}</span>
                    <span class="meter-hours">${hours.toFixed(2)} hrs (${percent}%)</span>
                </div>
                <div class="meter-track">
                    <div class="meter-fill ${fillClass}" style="width: ${percent}%;"></div>
                </div>
            </div>
        `;
    });

    container.innerHTML = html;
}

function renderUrgentTasks() {
    if (!elements.tableUrgentTasksTbody) return;

    if (!state.unfinishedTasks || state.unfinishedTasks.length === 0) {
        elements.tableUrgentTasksTbody.innerHTML = `<tr><td colspan="5" class="table-empty">All tasks are completed! 🎉</td></tr>`;
        return;
    }

    const urgentSubset = state.unfinishedTasks.slice(0, 5);
    elements.tableUrgentTasksTbody.innerHTML = urgentSubset.map(task => `
        <tr>
            <td><code>#${task.id}</code></td>
            <td><strong>${escapeHtml(task.title)}</strong></td>
            <td>${task.estimatedHours.toFixed(2)}h</td>
            <td>${formatDate(task.deadline)}</td>
            <td><span class="badge badge-${task.priority.toLowerCase()}">${task.priority}</span></td>
        </tr>
    `).join('');
}

function renderAllTasksTable() {
    if (!elements.tableAllTasksTbody) return;

    const query = (elements.taskSearchInput ? elements.taskSearchInput.value : '').toLowerCase().trim();
    const priorityFilter = elements.filterPriority ? elements.filterPriority.value : 'ALL';
    const statusFilter = elements.filterStatus ? elements.filterStatus.value : 'ALL';

    const filtered = state.tasks.filter(t => {
        const matchesQuery = !query || t.title.toLowerCase().includes(query) || String(t.id).includes(query);
        const matchesPriority = priorityFilter === 'ALL' || t.priority === priorityFilter;
        const matchesStatus = statusFilter === 'ALL' || t.status === statusFilter;
        return matchesQuery && matchesPriority && matchesStatus;
    });

    if (filtered.length === 0) {
        elements.tableAllTasksTbody.innerHTML = `<tr><td colspan="7" class="table-empty">No matching tasks found.</td></tr>`;
        return;
    }

    elements.tableAllTasksTbody.innerHTML = filtered.map(task => `
        <tr>
            <td><code>#${task.id}</code></td>
            <td><strong>${escapeHtml(task.title)}</strong></td>
            <td>${task.estimatedHours.toFixed(2)} hrs</td>
            <td>${formatDate(task.deadline)}</td>
            <td><span class="badge badge-${task.priority.toLowerCase()}">${task.priority}</span></td>
            <td><span class="badge badge-${task.status.toLowerCase()}">${task.status}</span></td>
            <td>
                <div style="display: flex; gap: 8px;">
                    <button class="btn btn-primary btn-sm" onclick="openEditModal(${task.id}, '${escapeHtml(task.title).replace(/'/g, "\\'")}', ${task.estimatedHours})">
                        ✏️ Edit
                    </button>
                    <button class="btn btn-secondary btn-sm" onclick="openStatusModal(${task.id}, '${escapeHtml(task.title).replace(/'/g, "\\'")}', '${task.status}')">
                        Status
                    </button>
                    <button class="btn btn-danger btn-sm" onclick="openDeleteModal(${task.id}, '${escapeHtml(task.title).replace(/'/g, "\\'")}')">
                        Delete
                    </button>
                </div>
            </td>
        </tr>
    `).join('');
}

function renderWorkloadView() {
    if (!state.workload) return;

    if (elements.wlUnfinished) elements.wlUnfinished.textContent = `${state.workload.unfinishedHours.toFixed(2)}h`;
    if (elements.wlAvailable) elements.wlAvailable.textContent = `${state.workload.availableHours.toFixed(2)}h`;
    if (elements.wlDeficit) elements.wlDeficit.textContent = `${state.workload.deficit.toFixed(2)}h`;
    if (elements.wlStatus) elements.wlStatus.textContent = state.workload.hasConflict ? 'CONFLICT' : 'OPTIMAL';

    if (elements.conflictAlertBanner) {
        if (state.workload.hasConflict) {
            elements.conflictAlertBanner.className = 'conflict-banner danger';
            elements.conflictAlertBanner.innerHTML = `
                <span style="font-size: 1.4rem;">⚠️</span>
                <div>
                    <div><strong>Workload Conflict Detected!</strong></div>
                    <div style="font-size: 0.88rem; font-weight: normal;">
                        Unfinished workload (${state.workload.unfinishedHours.toFixed(2)}h) exceeds available capacity (${state.workload.availableHours.toFixed(2)}h) by <strong>${state.workload.deficit.toFixed(2)} hours</strong>.
                    </div>
                </div>
            `;
        } else {
            elements.conflictAlertBanner.className = 'conflict-banner success';
            elements.conflictAlertBanner.innerHTML = `
                <span style="font-size: 1.4rem;">✅</span>
                <div>
                    <div><strong>No Workload Conflict</strong></div>
                    <div style="font-size: 0.88rem; font-weight: normal;">
                        Available capacity (${state.workload.availableHours.toFixed(2)}h) is sufficient to complete remaining unfinished workload (${state.workload.unfinishedHours.toFixed(2)}h).
                    </div>
                </div>
            `;
        }
    }

    renderPriorityMeters(elements.workloadPriorityMeters, state.workload.priorityWorkload, state.workload.unfinishedHours);
}

function renderUnfinishedQueue() {
    if (!elements.tableUnfinishedQueueTbody) return;

    if (!state.unfinishedTasks || state.unfinishedTasks.length === 0) {
        elements.tableUnfinishedQueueTbody.innerHTML = `<tr><td colspan="6" class="table-empty">No unfinished tasks in queue.</td></tr>`;
        return;
    }

    elements.tableUnfinishedQueueTbody.innerHTML = state.unfinishedTasks.map(task => `
        <tr>
            <td><span class="badge badge-${task.priority.toLowerCase()}">${task.priority}</span></td>
            <td><code>#${task.id}</code></td>
            <td><strong>${escapeHtml(task.title)}</strong></td>
            <td>${task.estimatedHours.toFixed(2)} hrs</td>
            <td>${formatDate(task.deadline)}</td>
            <td><span class="badge badge-${task.status.toLowerCase()}">${task.status}</span></td>
        </tr>
    `).join('');
}

function renderDependenciesTable() {
    if (!elements.dependenciesTbody) return;

    if (elements.depCountBadge) {
        elements.depCountBadge.textContent = `${state.dependencies.length} Links`;
    }

    if (!state.dependencies || state.dependencies.length === 0) {
        elements.dependenciesTbody.innerHTML = `<tr><td colspan="4" class="table-empty">No task dependencies defined.</td></tr>`;
        return;
    }

    elements.dependenciesTbody.innerHTML = state.dependencies.map(dep => `
        <tr>
            <td>
                <strong>${escapeHtml(dep.taskTitle)}</strong>
                <span style="color: var(--text-muted); font-size: 0.8rem; display: block;">ID: #${dep.taskId}</span>
            </td>
            <td>
                <strong>${escapeHtml(dep.dependsOnTitle)}</strong>
                <span style="color: var(--text-muted); font-size: 0.8rem; display: block;">Prerequisite ID: #${dep.dependsOnTaskId}</span>
            </td>
            <td>${formatDate(dep.createdAt)}</td>
            <td>
                <button class="btn btn-secondary btn-sm" onclick="handleRemoveDependency(${dep.taskId}, ${dep.dependsOnTaskId})">
                    Remove Link
                </button>
            </td>
        </tr>
    `).join('');
}

function renderHistoryTable() {
    if (!elements.historyTbody) return;

    if (elements.historyCountBadge) {
        elements.historyCountBadge.textContent = `${state.history.length} Logs`;
    }

    if (!state.history || state.history.length === 0) {
        elements.historyTbody.innerHTML = `<tr><td colspan="7" class="table-empty">No status transition history recorded yet. Updates from Web or Console will appear here.</td></tr>`;
        return;
    }

    elements.historyTbody.innerHTML = state.history.map(item => `
        <tr>
            <td><code>#${item.historyId}</code></td>
            <td><code>#${item.taskId}</code></td>
            <td><strong>${escapeHtml(item.taskTitle)}</strong></td>
            <td><span class="badge badge-${item.oldStatus.toLowerCase()}">${item.oldStatus}</span></td>
            <td>&rarr;</td>
            <td><span class="badge badge-${item.newStatus.toLowerCase()}">${item.newStatus}</span></td>
            <td>${formatDate(item.changedAt)}</td>
        </tr>
    `).join('');
}

function populateDependencyDropdowns() {
    if (!elements.depTaskId || !elements.depPrereqId) return;

    const options = state.tasks.map(t => `<option value="${t.id}">#${t.id} - ${escapeHtml(t.title)} (${t.status})</option>`).join('');

    elements.depTaskId.innerHTML = '<option value="">-- Select Dependent Task --</option>' + options;
    elements.depPrereqId.innerHTML = '<option value="">-- Select Prerequisite Task --</option>' + options;
}

// =========================================================================
// Form Handlers & Actions
// =========================================================================

async function handleCreateTask(e) {
    e.preventDefault();

    const title = elements.createTitle.value.trim();
    const hours = parseFloat(elements.createHours.value);
    const deadlineVal = elements.createDeadline.value;
    const priority = elements.createPriority.value;
    const status = elements.createStatus.value;

    if (!title) {
        showToast('Task title cannot be empty.', 'error');
        return;
    }
    if (isNaN(hours) || hours <= 0) {
        showToast('Estimated hours must be greater than zero.', 'error');
        return;
    }
    if (!deadlineVal) {
        showToast('Please select a deadline.', 'error');
        return;
    }

    // Format datetime as yyyy-MM-dd HH:mm
    const formattedDeadline = deadlineVal.replace('T', ' ');

    const payload = {
        title: title,
        estimatedHours: hours,
        deadline: formattedDeadline,
        priority: priority,
        status: status
    };

    try {
        const res = await fetch(`${API_BASE}/tasks`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        const data = await res.json();
        if (res.ok && data.success) {
            showToast('Task created and saved to MySQL successfully!', 'success');
            elements.createTaskForm.reset();
            setDefaultDeadline();
            await fetchAllData();
            switchView('tasks');
        } else {
            showToast(`Error: ${data.error || 'Failed to create task'}`, 'error');
        }
    } catch (err) {
        showToast(`Network error: ${err.message}`, 'error');
    }
}

async function handleAddDependency(e) {
    e.preventDefault();

    const taskId = parseInt(elements.depTaskId.value);
    const prereqId = parseInt(elements.depPrereqId.value);

    if (!taskId || !prereqId) {
        showToast('Please select both dependent and prerequisite tasks.', 'error');
        return;
    }

    if (taskId === prereqId) {
        showToast('A task cannot depend on itself (prevented by relational integrity).', 'error');
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/dependencies`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ taskId: taskId, dependsOnTaskId: prereqId })
        });

        const data = await res.json();
        if (res.ok && data.success) {
            showToast('Relational dependency linked successfully in MySQL!', 'success');
            elements.addDependencyForm.reset();
            await fetchDependencies();
        } else {
            showToast(`Error: ${data.error || 'Failed to link dependency'}`, 'error');
        }
    } catch (err) {
        showToast(`Network error: ${err.message}`, 'error');
    }
}

async function handleRemoveDependency(taskId, dependsOnTaskId) {
    if (!confirm(`Are you sure you want to remove the dependency between Task #${taskId} and Task #${dependsOnTaskId}?`)) {
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/dependencies?taskId=${taskId}&dependsOnTaskId=${dependsOnTaskId}`, {
            method: 'DELETE'
        });

        const data = await res.json();
        if (res.ok && data.success) {
            showToast('Dependency link removed from MySQL.', 'success');
            await fetchDependencies();
        } else {
            showToast(`Error: ${data.error || 'Failed to remove dependency'}`, 'error');
        }
    } catch (err) {
        showToast(`Network error: ${err.message}`, 'error');
    }
}

// Modal Handlers
window.openStatusModal = function(id, title, currentStatus) {
    state.selectedTaskIdForStatus = id;
    if (elements.modalTaskId) elements.modalTaskId.textContent = id;
    if (elements.modalTaskTitle) elements.modalTaskTitle.textContent = title;
    if (elements.modalStatusSelect) elements.modalStatusSelect.value = currentStatus;
    if (elements.statusModal) elements.statusModal.classList.add('open');
};

function closeStatusModal() {
    state.selectedTaskIdForStatus = null;
    if (elements.statusModal) elements.statusModal.classList.remove('open');
}

async function handleConfirmStatusUpdate() {
    const id = state.selectedTaskIdForStatus;
    const newStatus = elements.modalStatusSelect.value;
    if (!id || !newStatus) return;

    try {
        const res = await fetch(`${API_BASE}/tasks/${id}/status`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ status: newStatus })
        });

        const data = await res.json();
        if (res.ok && data.success) {
            showToast(`Task #${id} status updated to ${newStatus} (history logged)!`, 'success');
            closeStatusModal();
            await fetchAllData();
        } else {
            showToast(`Error: ${data.error || 'Failed to update status'}`, 'error');
        }
    } catch (err) {
        showToast(`Network error: ${err.message}`, 'error');
    }
}

// Edit Task Modal Handlers
window.openEditModal = function(id, title, estimatedHours) {
    state.selectedTaskIdForEdit = id;
    if (elements.editTaskId) elements.editTaskId.value = `#${id}`;
    if (elements.editTaskTitle) elements.editTaskTitle.value = title;
    if (elements.editTaskHours) elements.editTaskHours.value = estimatedHours;
    if (elements.editModal) elements.editModal.classList.add('open');
};

function closeEditModal() {
    state.selectedTaskIdForEdit = null;
    if (elements.editModal) elements.editModal.classList.remove('open');
}

async function handleConfirmEditTask() {
    const id = state.selectedTaskIdForEdit;
    if (!id) return;

    const title = elements.editTaskTitle.value.trim();
    const hours = parseFloat(elements.editTaskHours.value);

    if (!title) {
        showToast('Task title cannot be empty.', 'error');
        return;
    }
    if (title.length > 100) {
        showToast('Task title must not exceed 100 characters.', 'error');
        return;
    }
    if (isNaN(hours) || hours <= 0 || hours > 9999.99) {
        showToast('Estimated hours must be a positive finite number up to 9999.99.', 'error');
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/tasks/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                title: title,
                estimatedHours: hours
            })
        });

        const data = await res.json();
        if (res.ok && data.success) {
            showToast(`Task #${id} details updated successfully in MySQL!`, 'success');
            closeEditModal();
            await fetchAllData();
        } else {
            showToast(`Error: ${data.error || 'Failed to update task'}`, 'error');
        }
    } catch (err) {
        showToast(`Network error: ${err.message}`, 'error');
    }
}

window.openDeleteModal = function(id, title) {
    state.selectedTaskIdForDelete = id;
    if (elements.deleteTaskId) elements.deleteTaskId.textContent = id;
    if (elements.deleteTaskTitle) elements.deleteTaskTitle.textContent = title;
    if (elements.deleteModal) elements.deleteModal.classList.add('open');
};

function closeDeleteModal() {
    state.selectedTaskIdForDelete = null;
    if (elements.deleteModal) elements.deleteModal.classList.remove('open');
}

async function handleConfirmDelete() {
    const id = state.selectedTaskIdForDelete;
    if (!id) return;

    try {
        const res = await fetch(`${API_BASE}/tasks/${id}`, {
            method: 'DELETE'
        });

        const data = await res.json();
        if (res.ok && data.success) {
            showToast(`Task #${id} and associated relations deleted successfully.`, 'success');
            closeDeleteModal();
            await fetchAllData();
        } else {
            showToast(`Error: ${data.error || 'Failed to delete task'}`, 'error');
        }
    } catch (err) {
        showToast(`Network error: ${err.message}`, 'error');
    }
}

// =========================================================================
// Utilities
// =========================================================================

function setBackendStatus(online, message) {
    if (!elements.backendStatus) return;
    elements.backendStatus.textContent = message;
    const indicator = elements.backendStatus.previousElementSibling;
    if (indicator) {
        indicator.className = `status-indicator ${online ? 'online' : 'error'}`;
    }
}

function showToast(message, type = 'info') {
    if (!elements.toastContainer) return;

    const toast = document.createElement('div');
    toast.className = `toast ${type}`;

    const icons = {
        success: '✅',
        error: '❌',
        info: 'ℹ️'
    };

    toast.innerHTML = `<span>${icons[type] || 'ℹ️'}</span> <span>${escapeHtml(message)}</span>`;
    elements.toastContainer.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateY(8px)';
        toast.style.transition = 'all 0.25s ease';
        setTimeout(() => toast.remove(), 250);
    }, 4000);
}

function escapeHtml(str) {
    if (!str) return '';
    return str
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

function formatDate(dateStr) {
    if (!dateStr) return '-';
    return dateStr.replace('T', ' ');
}
