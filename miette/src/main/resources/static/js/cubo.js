function resolveColor(color) {
    if (!color.startsWith('--')) return color;
    const val = getComputedStyle(document.documentElement).getPropertyValue(color).trim();
    // Strip alpha channel from 8-char hex (#rrggbbaa → #rrggbb)
    return val.length === 9 && val.startsWith('#') ? val.slice(0, 7) : val;
}

export function cuboLoading(el, { color = '--verdigris', size = 64, lineWidth = size < 32 ? 1.2 : size / 40 } = {}) {
    const resolved = resolveColor(color);

    const canvas = document.createElement('canvas');
    canvas.width = size;
    canvas.height = size;
    canvas.style.display = 'block';
    el.appendChild(canvas);
    const ctx = canvas.getContext('2d');

    // 2.5× faster than the bocal animation
    const SPEED_X = 0.03, SPEED_Y = 0.045, SPEED_Z = 0.018;
    const s = 1;
    const baseVerts = [
        [ s,  s,  0], [-s,  s,  0], [ s, -s,  0], [-s, -s,  0],
        [ s,  0,  s], [-s,  0,  s], [ s,  0, -s], [-s,  0, -s],
        [ 0,  s,  s], [ 0, -s,  s], [ 0,  s, -s], [ 0, -s, -s]
    ];
    const edges = [
        [0,4],[0,6],[0,8],[0,10],
        [1,5],[1,7],[1,8],[1,10],
        [2,4],[2,6],[2,9],[2,11],
        [3,5],[3,7],[3,9],[3,11],
        [4,8],[4,9],[5,8],[5,9],
        [6,10],[6,11],[7,10],[7,11]
    ];

    let verts = baseVerts.map(v => [...v]);
    let timeLast = 0;
    let rafId = null;

    function rotX(a) { const c = Math.cos(a), s = Math.sin(a); for (const v of verts) { const y = v[1]*c-v[2]*s, z = v[1]*s+v[2]*c; v[1]=y; v[2]=z; } }
    function rotY(a) { const c = Math.cos(a), s = Math.sin(a); for (const v of verts) { const x = v[0]*c+v[2]*s, z = -v[0]*s+v[2]*c; v[0]=x; v[2]=z; } }
    function rotZ(a) { const c = Math.cos(a), s = Math.sin(a); for (const v of verts) { const x = v[0]*c-v[1]*s, y = v[0]*s+v[1]*c; v[0]=x; v[1]=y; } }

    function projectedVerts() {
        const fov = 4;
        return verts.map(v => {
            const z = v[2] + fov;
            return [v[0] / z, v[1] / z, z];
        });
    }

    function loop(timeNow) {
        const dt = Math.min(timeNow - timeLast, 50) * 0.001;
        timeLast = timeNow;
        rotX(SPEED_X * Math.PI * 2 * dt);
        rotY(SPEED_Y * Math.PI * 2 * dt);
        rotZ(SPEED_Z * Math.PI * 2 * dt);

        const cx = size / 2, cy = size / 2;
        const lw = Math.max(1, lineWidth);
        const proj = projectedVerts();
        const xs = proj.map(p => p[0]);
        const ys = proj.map(p => p[1]);
        const minX = Math.min(...xs), maxX = Math.max(...xs);
        const minY = Math.min(...ys), maxY = Math.max(...ys);
        const w = maxX - minX || 1;
        const h = maxY - minY || 1;
        const fitScale = Math.min(size / w, size / h);
        const offsetX = cx - (minX + maxX) * 0.5 * fitScale;
        const offsetY = cy - (minY + maxY) * 0.5 * fitScale;

        ctx.clearRect(0, 0, size, size);

        const projected = verts.map(v => {
            const z = v[2] + 4;
            return [v[0] / z * fitScale + offsetX, v[1] / z * fitScale + offsetY, z];
        });

        for (const [i, j] of edges) {
            const a = projected[i], b = projected[j];
            var z_avg = (a[2] + b[2]) / 2;
            var alpha = Math.min(0.9, Math.max(0.05, 1 - (z_avg - 1) / 6));
            var edgeWeight = Math.max(1, 3 - z_avg);  // arêtes proches = plus épaisses
            ctx.beginPath();
            ctx.moveTo(a[0], a[1]);
            ctx.lineTo(b[0], b[1]);
            ctx.globalAlpha = alpha;
            ctx.strokeStyle = resolved;
            ctx.lineWidth = lw * edgeWeight;
            ctx.stroke();
        }
        ctx.globalAlpha = 1;
        rafId = requestAnimationFrame(loop);
    }

    rafId = requestAnimationFrame(loop);

    return () => {
        cancelAnimationFrame(rafId);
        canvas.remove();
    };
}

// Vue directive — usage: <span v-if="loading" v-cubo-spinner></span>
// The v-if handles mount/unmount; mounted starts the animation, unmounted stops it.
export const vCuboSpinner = {
    mounted(el, binding) {
        const opts = binding.value ?? {};
        const size = opts.size ?? 16;
        const lineWidth = opts.lineWidth ?? 1.5;
        el.style.display = 'inline-block';
        el.style.width = size + 'px';
        el.style.height = size + 'px';
        el.style.verticalAlign = 'middle';
        el._stopCubo = cuboLoading(el, { size, color: opts.color ?? '--verdigris', lineWidth });
    },
    unmounted(el) {
        el._stopCubo?.();
        delete el._stopCubo;
    }
};
