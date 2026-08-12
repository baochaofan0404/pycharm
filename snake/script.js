const canvas = document.getElementById('board');
const ctx = canvas.getContext('2d');
const scoreEl = document.getElementById('score');
const bestEl = document.getElementById('best');
const overlay = document.getElementById('overlay');
const title = document.getElementById('overlay-title');
const text = document.getElementById('overlay-text');
const restart = document.getElementById('restart');

const N = 24, CELL = canvas.width / N;
let snake, food, dir, nextDir, score, timer, running = false, paused = false;
let best = Number(localStorage.getItem('snake-best') || 0);
bestEl.textContent = best;

function reset() {
  snake = [{x:12,y:12},{x:11,y:12},{x:10,y:12}];
  dir = {x:1,y:0}; nextDir = {...dir}; score = 0; scoreEl.textContent = score;
  spawnFood(); draw();
  running = true; paused = false; overlay.style.display = 'none';
  clearInterval(timer); timer = setInterval(tick, 105);
}
function spawnFood() {
  do { food = {x:Math.floor(Math.random()*N), y:Math.floor(Math.random()*N)}; }
  while (snake.some(s => s.x === food.x && s.y === food.y));
}
function setDirection(x,y) {
  if (!running || paused) return;
  if (x !== -dir.x || y !== -dir.y) nextDir = {x,y};
}
function tick() {
  dir = nextDir;
  const head = {x:snake[0].x + dir.x, y:snake[0].y + dir.y};
  const hitWall = head.x < 0 || head.x >= N || head.y < 0 || head.y >= N;
  const hitSelf = snake.some((s,i) => i > 0 && s.x === head.x && s.y === head.y);
  if (hitWall || hitSelf) return gameOver();
  snake.unshift(head);
  if (head.x === food.x && head.y === food.y) {
    score++; scoreEl.textContent = score;
    if (score > best) { best = score; bestEl.textContent = best; localStorage.setItem('snake-best', best); }
    spawnFood();
  } else snake.pop();
  draw();
}
function draw() {
  ctx.fillStyle = '#111827'; ctx.fillRect(0,0,canvas.width,canvas.height);
  ctx.strokeStyle = '#1f2937'; ctx.lineWidth = 1;
  for (let i=1;i<N;i++) { ctx.beginPath(); ctx.moveTo(i*CELL,0); ctx.lineTo(i*CELL,canvas.height); ctx.stroke(); ctx.beginPath(); ctx.moveTo(0,i*CELL); ctx.lineTo(canvas.width,i*CELL); ctx.stroke(); }
  ctx.fillStyle = '#f43f5e'; ctx.beginPath(); ctx.arc((food.x+.5)*CELL,(food.y+.5)*CELL,CELL*.34,0,Math.PI*2); ctx.fill();
  snake.forEach((s,i) => { ctx.fillStyle = i === 0 ? '#86efac' : '#22c55e'; ctx.beginPath(); ctx.roundRect(s.x*CELL+2,s.y*CELL+2,CELL-4,CELL-4,6); ctx.fill(); });
}
function gameOver() {
  running = false; clearInterval(timer); title.textContent = '游戏结束'; text.textContent = `得分：${score} · 最高分：${best}`; restart.textContent = '再来一局'; overlay.style.display = 'grid';
}
function togglePause() {
  if (!running) return;
  paused = !paused; title.textContent = paused ? '已暂停' : '准备继续'; text.textContent = paused ? '按空格继续' : '继续游戏'; restart.textContent = paused ? '继续' : '重新开始'; overlay.style.display = paused ? 'grid' : 'none';
}
restart.addEventListener('click', () => paused ? togglePause() : reset());
window.addEventListener('keydown', e => {
  const k=e.key.toLowerCase();
  if (k==='arrowup'||k==='w') setDirection(0,-1);
  else if (k==='arrowdown'||k==='s') setDirection(0,1);
  else if (k==='arrowleft'||k==='a') setDirection(-1,0);
  else if (k==='arrowright'||k==='d') setDirection(1,0);
  else if (k===' ') { e.preventDefault(); togglePause(); }
  else if (k==='enter' && !running) reset();
});
document.querySelectorAll('[data-dir]').forEach(b => b.addEventListener('click', () => ({up:()=>setDirection(0,-1),down:()=>setDirection(0,1),left:()=>setDirection(-1,0),right:()=>setDirection(1,0)}[b.dataset.dir])()));
reset();
