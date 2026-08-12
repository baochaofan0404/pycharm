const STORAGE_KEY = 'todo-app-tasks-v1';
const THEME_KEY = 'todo-app-theme-v1';

let tasks = loadTasks();
let filter = 'all';
let search = '';

const taskForm = document.querySelector('#taskForm');
const taskInput = document.querySelector('#taskInput');
const taskList = document.querySelector('#taskList');
const emptyState = document.querySelector('#emptyState');
const summary = document.querySelector('#summary');
const searchInput = document.querySelector('#searchInput');
const clearCompleted = document.querySelector('#clearCompleted');
const themeToggle = document.querySelector('#themeToggle');

function loadTasks() {
  try { return JSON.parse(localStorage.getItem(STORAGE_KEY)) || []; }
  catch { return []; }
}

function saveTasks() { localStorage.setItem(STORAGE_KEY, JSON.stringify(tasks)); }

function createTask(text) {
  return { id: crypto.randomUUID ? crypto.randomUUID() : `${Date.now()}-${Math.random()}`, text: text.trim(), completed: false, createdAt: Date.now() };
}

function visibleTasks() {
  const q = search.trim().toLowerCase();
  return tasks.filter(task => {
    const matchesFilter = filter === 'all' || (filter === 'active' && !task.completed) || (filter === 'completed' && task.completed);
    return matchesFilter && (!q || task.text.toLowerCase().includes(q));
  }).sort((a, b) => b.createdAt - a.createdAt);
}

function escapeHtml(text) {
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}

function render() {
  const visible = visibleTasks();
  taskList.innerHTML = visible.map(task => `
    <li class="task-item" data-id="${task.id}">
      <button class="check ${task.completed ? 'done' : ''}" data-action="toggle" aria-label="${task.completed ? '标记为未完成' : '标记为完成'}">${task.completed ? '✓' : ''}</button>
      <span class="task-text ${task.completed ? 'done' : ''}" data-action="edit" title="双击编辑">${escapeHtml(task.text)}</span>
      <button class="delete-button" data-action="delete" aria-label="删除任务" title="删除">✕</button>
    </li>`).join('');

  emptyState.classList.toggle('hidden', visible.length !== 0);
  const remaining = tasks.filter(t => !t.completed).length;
  const total = tasks.length;
  summary.textContent = total ? `${remaining} 个进行中 · 共 ${total} 个任务` : '准备开始今天的计划。';
  document.querySelectorAll('.filter-button').forEach(btn => btn.classList.toggle('active', btn.dataset.filter === filter));
}

taskForm.addEventListener('submit', e => {
  e.preventDefault();
  const text = taskInput.value.trim();
  if (!text) return;
  tasks.push(createTask(text));
  saveTasks(); render(); taskInput.value = ''; taskInput.focus();
});

taskList.addEventListener('click', e => {
  const action = e.target.dataset.action;
  if (!action) return;
  const item = e.target.closest('.task-item');
  const id = item.dataset.id;
  const task = tasks.find(t => t.id === id);
  if (!task) return;
  if (action === 'toggle') task.completed = !task.completed;
  if (action === 'delete') tasks = tasks.filter(t => t.id !== id);
  if (action === 'edit') startEdit(item, task);
  saveTasks(); render();
});

taskList.addEventListener('dblclick', e => {
  const item = e.target.closest('.task-item');
  if (!item) return;
  const task = tasks.find(t => t.id === item.dataset.id);
  if (task) startEdit(item, task);
});

function startEdit(item, task) {
  if (item.querySelector('.edit-input')) return;
  const span = item.querySelector('.task-text');
  const input = document.createElement('input');
  input.className = 'edit-input'; input.value = task.text; input.maxLength = 200;
  span.replaceWith(input); input.focus(); input.select();
  const finish = () => { const value = input.value.trim(); if (value) task.text = value; saveTasks(); render(); };
  input.addEventListener('keydown', e => { if (e.key === 'Enter') finish(); if (e.key === 'Escape') render(); });
  input.addEventListener('blur', finish);
}

document.querySelectorAll('.filter-button').forEach(btn => btn.addEventListener('click', () => { filter = btn.dataset.filter; render(); }));
searchInput.addEventListener('input', e => { search = e.target.value; render(); });
clearCompleted.addEventListener('click', () => { tasks = tasks.filter(t => !t.completed); saveTasks(); render(); });

function applyTheme(theme) {
  document.documentElement.classList.toggle('dark', theme === 'dark');
  themeToggle.textContent = theme === 'dark' ? '☀' : '☾';
}

themeToggle.addEventListener('click', () => {
  const next = document.documentElement.classList.contains('dark') ? 'light' : 'dark';
  localStorage.setItem(THEME_KEY, next); applyTheme(next);
});

applyTheme(localStorage.getItem(THEME_KEY) || (matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'));
render();
