/* ═══════════════════════════════════════════════════
   AcadeMap — CGPA Tracker Frontend Logic
   Backend: Spring Boot REST API on localhost:8080/api
═══════════════════════════════════════════════════ */

 const API = 'http://localhost:8080/api';
// const API = 'https://abcd1234.ngrok-free.app/api';

// ── Page Navigation ──────────────────────────────
function showPage(name) {
  document.querySelectorAll('.hero, .page').forEach(el => el.classList.add('hidden'));
  document.querySelectorAll('.nav-btn').forEach(b => b.classList.remove('active'));

  const target = document.getElementById(`page-${name}`);
  if (target) target.classList.remove('hidden');

  const btn = [...document.querySelectorAll('.nav-btn')]
    .find(b => b.textContent.toLowerCase().includes(name === 'dashboard' ? 'dash' : name));
  if (btn) btn.classList.add('active');

  if (name === 'dashboard') loadDashboard();
  if (name === 'tracker') loadStudentDropdown();
}

// ── Toast Notification ───────────────────────────
function showToast(msg, duration = 3000) {
  const t = document.getElementById('toast');
  t.textContent = msg;
  t.classList.add('show');
  setTimeout(() => t.classList.remove('show'), duration);
}

// ── API Helper ───────────────────────────────────
async function apiFetch(path, method = 'GET', body = null) {
  const opts = {
    method,
    headers: { 'Content-Type': 'application/json' }
  };
  if (body) opts.body = JSON.stringify(body);
  const res = await fetch(`${API}${path}`, opts);
  const json = await res.json();
  if (!json.success) throw new Error(json.message || 'Request failed');
  return json.data;
}

// ── Dashboard ────────────────────────────────────
async function loadDashboard() {
  try {
    const students = await apiFetch('/students');
    const statTotal = document.getElementById('stat-total');
    const statAvg   = document.getElementById('stat-avg');
    const lbList    = document.getElementById('lb-list');

    statTotal.textContent = students.length;

    const withCgpa = students.filter(s => s.semestersCompleted > 0);
    if (withCgpa.length > 0) {
      const avg = withCgpa.reduce((sum, s) => sum + s.currentCgpa, 0) / withCgpa.length;
      statAvg.textContent = avg.toFixed(2);
    } else {
      statAvg.textContent = '—';
    }

    if (students.length === 0) {
      lbList.innerHTML = '<div class="lb-empty">No students registered yet. <a href="#" onclick="showPage(\'register\')">Register one →</a></div>';
      return;
    }

    lbList.innerHTML = students.map((s, i) => {
      const rankClass = i === 0 ? 'gold' : i === 1 ? 'silver' : i === 2 ? 'bronze' : '';
      const cgpaClass = statusToCgpaClass(s.overallStatus);
      const hasData   = s.semestersCompleted > 0;
      const cgpa      = hasData ? s.currentCgpa.toFixed(2) : '—';

      return `<div class="lb-row" onclick="openStudent(${s.id})">
        <div class="lb-rank ${rankClass}">${i + 1}</div>
        <div class="lb-info">
          <div class="lb-name">${s.name}</div>
          <div class="lb-meta">${s.registerNumber} · ${s.branch}</div>
        </div>
        <div style="display:flex;align-items:center;gap:10px;">
          <span class="lb-badge ${statusToClass(s.overallStatus)}">${friendlyStatus(s.overallStatus)}</span>
          <span class="lb-cgpa ${cgpaClass}">${cgpa}</span>
        </div>
      </div>`;
    }).join('');

  } catch (err) {
    document.getElementById('lb-list').innerHTML =
      `<div class="lb-empty">⚠️ Cannot connect to backend. Start Spring Boot first.<br><small>${err.message}</small></div>`;
  }
}

function openStudent(id) {
  showPage('tracker');
  setTimeout(async () => {
    const sel = document.getElementById('track-student');
    sel.value = id;
    await loadStudentData(id);
  }, 100);
}

// ── Register Student ─────────────────────────────
async function handleRegister(e) {
  e.preventDefault();
  const btn = document.getElementById('reg-submit');
  const msg = document.getElementById('reg-msg');

  const payload = {
    name:           document.getElementById('reg-name').value.trim(),
    email:          document.getElementById('reg-email').value.trim(),
    registerNumber: document.getElementById('reg-regno').value.trim().toUpperCase(),
    branch:         document.getElementById('reg-branch').value,
    targetCgpa:     parseFloat(document.getElementById('reg-target').value)
  };

  if (!payload.branch) return showMsg(msg, 'Please select your branch', 'error');

  btn.disabled = true;
  btn.querySelector('span').textContent = 'Registering...';
  showMsg(msg, '', '');

  try {
    const student = await apiFetch('/students', 'POST', payload);
    showMsg(msg, `✅ Registered successfully! Student ID: ${student.id}. You can now track in the Tracker tab.`, 'success');
    document.getElementById('register-form').reset();
    document.getElementById('reg-target-slider').value = 9.0;
    document.getElementById('reg-target').value = 9.0;
    showToast('Student registered! 🎉');
    loadDashboard();
  } catch (err) {
    showMsg(msg, `❌ ${err.message}`, 'error');
  } finally {
    btn.disabled = false;
    btn.querySelector('span').textContent = 'Register Student';
  }
}

// ── CGPA Slider Sync ─────────────────────────────
function syncSlider() {
  const val = document.getElementById('reg-target-slider').value;
  document.getElementById('reg-target').value = val;
  updateSliderFill(val);
}
function syncInput() {
  let val = parseFloat(document.getElementById('reg-target').value);
  if (isNaN(val)) return;
  val = Math.min(10, Math.max(0, val));
  document.getElementById('reg-target-slider').value = val;
  updateSliderFill(val);
}
function updateSliderFill(val) {
  const pct = ((val - 6) / 4) * 100;
  const slider = document.getElementById('reg-target-slider');
  slider.style.background = `linear-gradient(to right, var(--amber) 0%, var(--amber) ${pct}%, var(--border2) ${pct}%)`;
}

// ── Tracker — Load Dropdown ──────────────────────
async function loadStudentDropdown() {
  const sel = document.getElementById('track-student');
  try {
    const students = await apiFetch('/students');
    sel.innerHTML = '<option value="">— Choose a student —</option>' +
      students.map(s => `<option value="${s.id}">${s.name} (${s.registerNumber})</option>`).join('');
  } catch {
    sel.innerHTML = '<option value="">⚠️ Backend not connected</option>';
  }
}

async function loadStudent() {
  const id = document.getElementById('track-student').value;
  if (!id) {
    document.getElementById('student-panel').classList.add('hidden');
    return;
  }
  await loadStudentData(id);
}

// ── Load Student Data ────────────────────────────
async function loadStudentData(id) {
  try {
    const s = await apiFetch(`/students/${id}`);
    document.getElementById('student-panel').classList.remove('hidden');
    renderProfile(s);
    renderProgress(s);
    renderGuidance(s);
    renderSemesters(s);
    renderAddForm(s);
  } catch (err) {
    showToast('Error loading student: ' + err.message);
  }
}

function renderProfile(s) {
  const initials = s.name.split(' ').map(w => w[0]).join('').toUpperCase().slice(0, 2);
  document.getElementById('profile-strip').innerHTML = `
    <div class="ps-avatar">${initials}</div>
    <div class="ps-info">
      <div class="ps-name">${s.name}</div>
      <div class="ps-meta">${s.registerNumber} · ${s.branch}</div>
    </div>
    <div class="ps-stats">
      <div class="ps-stat">
        <div class="ps-stat-val amber">${s.targetCgpa.toFixed(2)}</div>
        <div class="ps-stat-lbl">Target CGPA</div>
      </div>
      <div class="ps-stat">
        <div class="ps-stat-val ${cgpaColor(s)}">${s.semestersCompleted > 0 ? s.currentCgpa.toFixed(2) : '—'}</div>
        <div class="ps-stat-lbl">Current CGPA</div>
      </div>
      <div class="ps-stat">
        <div class="ps-stat-val">${s.semestersCompleted} / 8</div>
        <div class="ps-stat-lbl">Semesters Done</div>
      </div>
      <div class="ps-stat">
        <div class="ps-stat-val ${s.semestersRemaining > 0 ? '' : 'green'}">${s.requiredGpaPerSemester > 0 ? s.requiredGpaPerSemester.toFixed(2) : '✓'}</div>
        <div class="ps-stat-lbl">Required/Sem</div>
      </div>
    </div>
  `;
}

function renderProgress(s) {
  const curr = s.semestersCompleted > 0 ? s.currentCgpa : 0;
  const target = s.targetCgpa;
  const fillPct = Math.min(100, (curr / 10) * 100);
  const targetPct = Math.min(100, (target / 10) * 100);

  document.getElementById('prog-label').textContent = 'CGPA Progress (out of 10.0)';
  document.getElementById('prog-value').textContent =
    `${curr.toFixed(2)} / ${target.toFixed(2)} target`;
  document.getElementById('prog-fill').style.width = `${fillPct}%`;
  document.getElementById('prog-target-marker').style.left = `calc(${targetPct}% - 2px)`;
}

function renderGuidance(s) {
  const banner = document.getElementById('guidance-banner');
  const cls = statusToClass(s.overallStatus);
  banner.className = `guidance-banner ${cls}`;
  banner.textContent = s.motivationalMessage;
}

function renderSemesters(s) {
  const grid = document.getElementById('sem-grid');
  const records = s.semesterRecords || [];
  const recordMap = {};
  records.forEach(r => { recordMap[r.semesterNumber] = r; });

  grid.innerHTML = Array.from({ length: 8 }, (_, i) => {
    const sem = i + 1;
    const r = recordMap[sem];
    if (r) {
      const cls = r.progressStatus.toLowerCase().replace('_', '-');
      const badgeMap = {
        'ahead': '↑ Ahead', 'on-track': '✓ On Track',
        'behind': '↓ Behind', 'critical': '⚠ Critical', 'achieved': '★ Achieved'
      };
      return `<div class="sem-card ${cls}" title="${r.guidanceMessage}">
        <div class="sem-num">Semester ${sem}</div>
        <div class="sem-gpa-val">${r.gpa.toFixed(2)}</div>
        <div class="sem-cgpa">CGPA: ${r.cgpaAfterSemester.toFixed(2)}</div>
        <span class="sem-badge lb-badge ${cls}">${badgeMap[cls] || cls}</span>
      </div>`;
    } else {
      return `<div class="sem-card empty">
        <div class="sem-num">Semester ${sem}</div>
        <div class="sem-gpa-val">—</div>
        <div class="sem-cgpa">Not entered</div>
      </div>`;
    }
  }).join('');
}

function renderAddForm(s) {
  const card = document.getElementById('add-sem-card');
  const nextSem = s.semestersCompleted + 1;

  if (s.semestersCompleted >= 8) {
    card.innerHTML = `<div class="guidance-banner achieved">
      🎓 All 8 semesters completed! Final CGPA: <strong>${s.currentCgpa.toFixed(2)}</strong>
      ${s.currentCgpa >= s.targetCgpa ? ' — Target Achieved! 🎉' : ' — Keep learning beyond college!'}
    </div>`;
    return;
  }

  document.getElementById('next-sem-label').textContent =
    `Semester ${nextSem} GPA (0.00 – 10.00)`;
  card.querySelector('h3').textContent = `Add Semester ${nextSem} GPA`;
  document.getElementById('sem-gpa').value = '';

  card.querySelector('.btn-primary').onclick = addSemester;
}

// ── Add Semester GPA ─────────────────────────────
async function addSemester() {
  const id  = document.getElementById('track-student').value;
  const gpa = parseFloat(document.getElementById('sem-gpa').value);
  const msg = document.getElementById('sem-msg');

  if (!id) return;
  if (isNaN(gpa) || gpa < 0 || gpa > 10) {
    return showMsg(msg, 'Please enter a valid GPA between 0.00 and 10.00', 'error');
  }

  const student = await apiFetch(`/students/${id}`);
  const nextSem = student.semestersCompleted + 1;

  const btn = document.querySelector('#add-sem-card .btn-primary');
  btn.disabled = true; btn.textContent = 'Saving...';

  try {
    await apiFetch(`/students/${id}/semesters`, 'POST', {
      semesterNumber: nextSem,
      gpa: gpa
    });
    showMsg(msg, `✅ Semester ${nextSem} GPA recorded!`, 'success');
    showToast(`Semester ${nextSem} saved! 🎯`);
    await loadStudentData(id);
  } catch (err) {
    showMsg(msg, `❌ ${err.message}`, 'error');
  } finally {
    btn.disabled = false; btn.textContent = 'Submit GPA';
  }
}

// ── Helpers ──────────────────────────────────────
function showMsg(el, text, type) {
  el.className = `msg ${type}`;
  el.textContent = text;
  if (text) el.classList.remove('hidden');
  else el.classList.add('hidden');
}

function statusToClass(status) {
  const map = {
    'AHEAD': 'ahead', 'ON_TRACK': 'on-track', 'BEHIND': 'behind',
    'CRITICAL': 'critical', 'ACHIEVED': 'achieved', 'NOT_STARTED': 'not-started'
  };
  return map[status] || 'not-started';
}

function statusToCgpaClass(status) {
  const map = {
    'AHEAD': 'ahead', 'ON_TRACK': 'on-track', 'BEHIND': 'behind',
    'CRITICAL': 'behind', 'ACHIEVED': 'ahead', 'NOT_STARTED': 'no-data'
  };
  return map[status] || 'no-data';
}

function friendlyStatus(status) {
  const map = {
    'AHEAD': 'Ahead', 'ON_TRACK': 'On Track', 'BEHIND': 'Behind',
    'CRITICAL': 'Critical', 'ACHIEVED': 'Achieved ✓', 'NOT_STARTED': 'Not Started'
  };
  return map[status] || status;
}

function cgpaColor(s) {
  if (s.semestersCompleted === 0) return '';
  if (s.currentCgpa >= s.targetCgpa) return 'green';
  if (s.overallStatus === 'BEHIND' || s.overallStatus === 'CRITICAL') return 'red';document.addEventListener("mousemove", (e) => {
  document.querySelectorAll(".form-card, .leaderboard, .profile-strip")
    .forEach(card => {
      const rect = card.getBoundingClientRect();
      const x = e.clientX - rect.left;
      const y = e.clientY - rect.top;

      const rx = (y / rect.height - 0.5) * 6;
      const ry = (x / rect.width - 0.5) * -6;

      card.style.transform =
        `perspective(1000px) rotateX(${rx}deg) rotateY(${ry}deg)`;
    });
});
  return 'amber';
}

// ── Init ─────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
  loadDashboard();
  // Sync slider on load
  updateSliderFill(9.0);

  // Mark dashboard nav active
  document.querySelector('.nav-btn').classList.add('active');
});
