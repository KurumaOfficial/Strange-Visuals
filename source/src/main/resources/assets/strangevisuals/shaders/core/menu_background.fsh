#version 330

layout(std140) uniform MenuBackgroundData {
    vec4 resolution; // x: width, y: height, z: bgMode, w: darkening
    vec4 time;       // x: seconds, y: opened, z: shaderMode, w: intensity
    vec4 accent;     // x,y,z: accent rgb, w: opacity
};

in vec2 uv;
out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}

float fbm(vec2 p) {
    float sum = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 5; i++) {
        sum += amp * noise(p);
        p *= 2.03;
        amp *= 0.52;
    }
    return sum;
}

// 0. Cosmic Drift (Flagship fluid nebulae waves)
vec3 renderCosmicDrift(vec2 p, float t, float aspect) {
    vec2 drift = vec2(fbm(p * 1.7 + vec2(0.0, t * 0.035)),
                      fbm(p * 1.7 + vec2(4.7, -t * 0.03)));
    vec2 pull = vec2(fbm(p * 2.1 + drift * 2.2 + vec2(1.7, 9.2) + t * 0.045),
                     fbm(p * 2.1 + drift * 2.2 + vec2(6.3, 2.8) - t * 0.04));
    float body = fbm(p * 1.9 + pull * 2.4);

    float ridge = 1.0 - abs(body - 0.52) * 4.6;
    ridge = pow(clamp(ridge, 0.0, 1.0), 3.4);

    float depth = smoothstep(0.2, 0.85, body);
    float lum = 0.035 + depth * 0.07 + ridge * 0.18;

    float sweep = sin((p.x * 0.7 + p.y * 0.35) * 1.6 - t * 0.16) * 0.5 + 0.5;
    lum += pow(sweep, 3.0) * 0.05;

    vec3 col = vec3(lum) * mix(vec3(0.82, 0.86, 1.0), accent.rgb, 0.45 + ridge * 0.25);
    col += accent.rgb * ridge * 0.07;

    vec2 dustP = vec2(uv.x * aspect, uv.y + t * 0.008) * 220.0;
    float dust = pow(hash(floor(dustP)), 60.0) * (0.5 + 0.5 * sin(t * 1.7 + hash(floor(dustP)) * 40.0));
    col += accent.rgb * dust * 0.5;
    return col;
}

// 1. Starfield (3D Hyperspace parallax travel)
vec3 renderStarfield(vec2 p, float t, float aspect) {
    vec3 col = vec3(0.015, 0.018, 0.025);
    float speed = t * 0.4;

    for (int layer = 1; layer <= 3; layer++) {
        float depth = float(layer) * 0.35;
        vec2 sp = p * (25.0 * depth);
        sp += vec2(speed * (0.2 + depth * 0.5), sin(speed * 0.1) * 0.2);

        vec2 cell = floor(sp);
        float h = hash(cell + float(layer) * 17.3);
        if (h > 0.94) {
            vec2 center = cell + 0.5;
            float dist = length(sp - center);
            float twinkle = 0.6 + 0.4 * sin(t * 3.0 + h * 62.0);
            float star = smoothstep(0.18 * depth, 0.0, dist) * twinkle;
            vec3 starCol = mix(vec3(1.0), accent.rgb, h);
            col += starCol * star * (0.8 + depth * 0.6);
        }
    }

    // Distant cosmic gas glow
    float gas = fbm(p * 1.2 + vec2(t * 0.01, -t * 0.015));
    col += mix(vec3(0.02, 0.03, 0.06), accent.rgb * 0.15, gas);
    return col;
}

// 2. Aurora Borealis (Ethereal dancing northern lights)
vec3 renderAurora(vec2 p, float t, float aspect) {
    vec3 col = vec3(0.01, 0.015, 0.028);

    // Subtle background stars
    vec2 sp = p * 40.0;
    float sh = hash(floor(sp));
    if (sh > 0.965) {
        float sDist = length(fract(sp) - 0.5);
        col += vec3(0.8) * smoothstep(0.15, 0.0, sDist) * (0.4 + 0.6 * sin(t * 2.0 + sh * 30.0));
    }

    // Aurora ribbons
    for (int i = 0; i < 3; i++) {
        float fi = float(i);
        float wave = sin(p.x * (1.8 + fi * 0.6) + t * (0.35 + fi * 0.15) + fi * 2.1) * 0.15;
        wave += cos(p.x * 3.2 - t * 0.25) * 0.06;
        float distToRibbon = abs(p.y - wave + 0.05 - fi * 0.12);

        float ribbonGlow = smoothstep(0.35, 0.0, distToRibbon);
        ribbonGlow = pow(ribbonGlow, 1.8);

        vec3 auroraTint = i == 0 ? vec3(0.1, 0.95, 0.55) : (i == 1 ? accent.rgb : vec3(0.7, 0.2, 0.9));
        col += auroraTint * ribbonGlow * (0.28 + 0.12 * sin(t * 0.8 + fi));
    }
    return col;
}

// 3. Cyber Grid (Synthwave 3D infinite perspective plane)
vec3 renderCyberGrid(vec2 p, float t, float aspect) {
    vec3 col = vec3(0.01, 0.012, 0.02);

    float horizon = -0.05;
    if (p.y < horizon) {
        float depth = 0.6 / abs(p.y - horizon);
        vec2 gridP = vec2(p.x * depth * 0.8, depth + t * 1.8);

        vec2 gridLines = abs(fract(gridP - 0.5) - 0.5) / fwidth(gridP);
        float lineIntensity = 1.0 - min(min(gridLines.x, gridLines.y), 1.0);

        float fade = smoothstep(25.0, 1.0, depth);
        col += mix(vec3(0.2, 0.4, 0.8), accent.rgb, 0.6) * lineIntensity * fade * 0.45;
    }

    // Horizon glow
    float horizonGlow = exp(-abs(p.y - horizon) * 14.0);
    col += mix(accent.rgb, vec3(0.6, 0.2, 0.9), 0.5) * horizonGlow * 0.35;

    // Digital floating dust motes
    vec2 dp = vec2(p.x * 20.0, p.y * 20.0 + t * 0.8);
    float dh = hash(floor(dp));
    if (dh > 0.95) {
        float dDist = length(fract(dp) - 0.5);
        col += accent.rgb * smoothstep(0.2, 0.0, dDist) * 0.5;
    }
    return col;
}

// 4. Warp Vortex (Spiral dimensional accretion disk)
vec3 renderWarpVortex(vec2 p, float t, float aspect) {
    float r = length(p);
    float a = atan(p.y, p.x);

    float spiral = a + 4.5 * log(max(r, 0.001)) - t * 0.6;
    float arms = sin(spiral * 3.0) * 0.5 + 0.5;
    arms = pow(arms, 2.5);

    float ring = exp(-pow((r - 0.32) * 5.0, 2.0));
    float core = smoothstep(0.08, 0.0, r);

    float lum = (arms * ring * 0.45 + ring * 0.25) * smoothstep(0.05, 0.2, r);
    vec3 col = vec3(lum) * mix(vec3(0.9, 0.7, 1.0), accent.rgb, 0.65);
    col += accent.rgb * ring * 0.2;

    // Dark core with event horizon glow
    col *= (1.0 - core);
    col += vec3(0.015, 0.02, 0.03);
    return col;
}

// 5. Flowing Plasma (Organic liquid metaballs caustics)
vec3 renderPlasma(vec2 p, float t, float aspect) {
    float v1 = sin(p.x * 5.0 + t * 0.8);
    float v2 = sin(p.y * 5.0 - t * 0.7);
    float v3 = sin((p.x + p.y) * 4.0 + t * 0.9);
    float cx = p.x + 0.5 * sin(t * 0.5);
    float cy = p.y + 0.5 * cos(t * 0.4);
    float v4 = sin(sqrt(cx * cx + cy * cy + 1.0) * 6.0 - t * 1.2);

    float plasma = (v1 + v2 + v3 + v4) * 0.25 + 0.5;
    float ripple = sin(plasma * 12.0 + t * 0.5) * 0.5 + 0.5;
    ripple = pow(ripple, 3.0);

    vec3 col = mix(vec3(0.02, 0.03, 0.06), accent.rgb * 0.25, plasma);
    col += mix(accent.rgb, vec3(0.85, 0.95, 1.0), 0.5) * ripple * 0.22;
    return col;
}

// 6. Pulse Nebula (Multi-octave cosmic clouds)
vec3 renderPulseNebula(vec2 p, float t, float aspect) {
    vec2 pos = p * 3.0;
    vec2 q = vec2(fbm(pos + vec2(0.0, t * 0.08)), fbm(pos + vec2(1.0, 1.0) - vec2(t * 0.06, 0.0)));
    vec2 r = vec2(fbm(pos + q + vec2(1.7, 1.2) + vec2(0.0, t * 0.07)), fbm(pos + q + vec2(8.3, 2.8) - vec2(0.0, t * 0.09)));
    float f = fbm(pos + r);

    vec3 color1 = accent.rgb * 1.4;
    vec3 color2 = accent.rgb * 0.45;
    vec3 color3 = vec3(accent.r * 0.4, accent.g * 0.3, accent.b * 1.4);

    vec3 col = mix(color1, color2, clamp(f * f * 4.0, 0.0, 1.0));
    col = mix(col, color1, clamp(length(q), 0.0, 1.0));
    col = mix(col, color3, clamp(length(r.x), 0.0, 1.0));

    col = (f * f * f + 0.6 * f * f + 0.5 * f) * col;
    return clamp(col * 1.8, 0.0, 1.0);
}

// 7. Pulse Starfield (Volumetric 3D flythrough)
vec3 renderPulseStarfield(vec2 p, float t, float aspect) {
    vec3 dir = vec3(p * 0.9, 1.0);
    vec3 from = vec3(1.8 * cos(0.01 * t) + 0.001 * t, 1.8 * sin(0.01 * t) + 0.001 * t, 0.003 * t);

    float s = 0.24;
    vec3 v = vec3(0.0);
    for (int r = 0; r < 5; r++) {
        vec3 p2 = from + s * dir;
        p2 = abs(vec3(0.85) - mod(p2, vec3(1.7)));
        float pa = length(p2);
        float a = 0.0;
        for (int i = 0; i < 7; i++) {
            p2 = abs(p2) / dot(p2, p2) - 0.79;
            a += abs(length(p2) - pa);
            pa = length(p2);
        }
        a *= a * a;
        v += vec3(s, s * s, s * s * s * s) * a * 0.003;
        s += 0.39;
    }
    vec3 col = mix(v, accent.rgb * length(v), 0.6);
    return clamp(col, 0.0, 1.0);
}

// 8. Pulse Cobweb (Fractal lattice geometry)
float cobwebDist(vec3 p) {
    vec3 q = abs(mod(p + vec3(cos(p.z * 0.5), cos(p.x * 0.5), cos(p.y * 0.5)), 2.0) - 1.0);
    float a = q.x + q.y + q.z - min(min(q.x, q.y), q.z) - max(max(q.x, q.y), q.z);
    q = vec3(p.x + p.y, p.y + p.z, p.z + p.x) * 0.70710678;
    q = abs(mod(q, 2.0) - 1.0);
    float b = q.x + q.y + q.z - min(min(q.x, q.y), q.z) - max(max(q.x, q.y), q.z);
    return min(a, b);
}

vec3 renderPulseCobweb(vec2 p, float t, float aspect) {
    vec2 m = vec2(sin(t * 0.3) * 0.3, cos(t * 0.2) * 0.3);
    vec3 o = vec3(0.0, 0.0, t * 0.5);
    vec3 s = vec3(m, 0.0);
    vec3 b = vec3(0.0);
    vec3 d = vec3(p, 1.0) / 24.0;
    vec3 total = vec3(0.5);

    for (int i = 0; i < 28; ++i) {
        float h = cobwebDist(b + s + o);
        b += h * 8.0 * d;
        total += h;
    }
    total /= 28.0;
    total = total * (1.0 - total * 0.01) * accent.rgb;
    total *= b.z * 0.15;
    return clamp(total * 2.5, 0.0, 1.0);
}

// 9. Pulse Plasma (Multi-source ripple fluid)
vec3 renderPulsePlasma(vec2 p, float t, float aspect) {
    vec2 uvCoord = p * 0.5 + 0.5;
    float v = 0.0;
    v += sin(uvCoord.x * 10.0 + t);
    v += sin((uvCoord.y * 10.0 + t) * 0.5);
    v += sin((uvCoord.x * 10.0 + uvCoord.y * 10.0 + t) * 0.5);

    vec2 c = uvCoord * 10.0 - vec2(5.0);
    v += sin(sqrt(c.x * c.x + c.y * c.y + 1.0) + t);

    vec2 c2 = uvCoord * 10.0 - vec2(5.0 + sin(t * 0.5) * 3.0, 5.0 + cos(t * 0.7) * 3.0);
    v += sin(sqrt(c2.x * c2.x + c2.y * c2.y + 1.0) + t * 1.5);
    float pl = v * 0.2;

    float glow = pow(abs(sin(pl * 3.14159 * 2.0)), 2.0) * 0.5;
    float pulse = 0.8 + sin(t * 2.0 + pl * 5.0) * 0.2;
    vec3 col = mix(accent.rgb * 0.5, vec3(0.95, 0.85, 1.0), abs(sin(pl * 2.5))) * pulse;
    col += accent.rgb * glow;
    return clamp(col, 0.0, 1.0);
}

// 6. Ocean (Deep bioluminescent abyssal currents)
vec3 renderOcean(vec2 p, float t, float aspect) {
    vec2 p2 = p * 4.2;
    float waves = sin((p2.x + t * 0.35) * 2.2 + fbm(p2 + vec2(0.0, t * 0.12)) * 2.5);
    float caustics = smoothstep(0.55, 0.95, 0.5 + 0.5 * waves);
    float fog = fbm(p2 * 0.7 - vec2(t * 0.08, t * 0.05));
    vec3 base = vec3(0.01, 0.04, 0.09);
    vec3 col = mix(base * 0.3, base, fog * 0.85);
    col += accent.rgb * caustics * 0.95;
    return clamp(col, 0.0, 1.0);
}

// 7. Toxin (Bubbling radioactive acidic waves)
vec3 renderToxin(vec2 p, float t, float aspect) {
    vec2 p2 = p * 4.0;
    float fumes = fbm(p2 + vec2(t * 0.18, -t * 0.12));
    float bubbles = smoothstep(0.68, 0.96, fbm(p2 * 1.6 - t * 0.25));
    vec3 base = vec3(0.02, 0.07, 0.015);
    vec3 col = mix(base * 0.2, base, fumes);
    col += accent.rgb * bubbles * 1.15;
    return clamp(col, 0.0, 1.0);
}

// 8. Thunder (Crackling high-voltage atmospheric lightning)
vec3 renderThunder(vec2 p, float t, float aspect) {
    vec2 p2 = p * 4.8;
    float cloud = fbm(p2 - vec2(t * 0.12, t * 0.08));
    float bolt = abs(sin((p2.x + fbm(p2 + t * 0.15) * 2.5) * 2.8 - t * 3.2));
    bolt = smoothstep(0.85, 0.99, bolt);
    vec3 base = vec3(0.02, 0.02, 0.06);
    vec3 col = mix(base * 0.2, base, cloud * 0.9);
    col += mix(vec3(1.0), accent.rgb, 0.4) * bolt * 2.1;
    return clamp(col, 0.0, 1.0);
}

// 9. Sunset (Warm clouds, radiant gold horizon)
vec3 renderSunset(vec2 p, float t, float aspect) {
    vec2 uvCoord = p + 0.5;
    float horizon = smoothstep(-0.2, 0.8, 1.0 - uvCoord.y);
    float clouds = fbm(vec2(uvCoord.x * 3.0 + t * 0.08, uvCoord.y * 2.2));
    float gold = smoothstep(0.62, 0.88, clouds + horizon * 0.25);
    vec3 base = vec3(0.12, 0.03, 0.08);
    vec3 warm = mix(base, accent.rgb, horizon);
    return clamp((warm * (0.35 + clouds * 0.65) + accent.rgb * gold * 0.65), 0.0, 1.0);
}

// 10. Void (Deep dimensional vortex singularity)
vec3 renderVoid(vec2 p, float t, float aspect) {
    float angle = atan(p.y, p.x);
    float radius = length(p) * 3.0;
    float swirl = fbm(vec2(angle * 1.4, radius * 1.8 - t * 0.22));
    float rift = smoothstep(0.74, 0.96, sin(angle * 5.0 + t * 0.8 + swirl * 4.5));
    vec3 base = vec3(0.02, 0.01, 0.05);
    vec3 col = mix(base * 0.10, accent.rgb * 0.7, swirl * 0.65);
    col += accent.rgb * rift * 0.65;
    return clamp(col, 0.0, 1.0);
}

// 11. Solaris (Solar corona flares and convection cells)
vec3 renderSolaris(vec2 p, float t, float aspect) {
    float radius = length(p * 2.4);
    float angle = atan(p.y, p.x);
    float core = smoothstep(1.05, 0.10, radius);
    float flare = smoothstep(0.58, 0.96, sin(angle * 6.0 - t * 1.55 + fbm(p * 2.4) * 3.0) * 0.5 + 0.5);
    float convection = smoothstep(0.42, 0.86, fbm(p * 3.1 - t * 0.35));
    vec3 base = vec3(0.08, 0.02, 0.0);
    vec3 col = base * (core * 0.62 + convection * 0.42);
    col += accent.rgb * (flare * (1.0 - core) * 1.3 + core * 0.92);
    return clamp(col, 0.0, 1.0);
}

// 12. Fractal (Sacred geometric recursive iterations)
vec3 renderFractal(vec2 p, float t, float aspect) {
    vec2 z = p * 2.2;
    float accum = 0.0;
    float glow = 0.0;
    float c = cos(0.65 + t * 0.05);
    float s = sin(0.65 + t * 0.05);
    mat2 rot = mat2(c, -s, s, c);
    for (int i = 0; i < 4; i++) {
        z = abs(z) / clamp(dot(z, z), 0.25, 2.8) - vec2(0.78, 0.62);
        z *= rot;
        accum += exp(-3.4 * abs(length(z) - 0.75));
        glow += exp(-8.0 * abs(z.x * z.y));
    }
    vec3 base = vec3(0.04, 0.02, 0.08);
    vec3 col = base * accum * 0.4;
    col += accent.rgb * glow * 0.4;
    col += mix(base, accent.rgb, 0.5) * smoothstep(0.58, 0.95, fbm(z * 2.0 + t * 0.1)) * 0.45;
    return clamp(col, 0.0, 1.0);
}

// 13. Monsoon (Tempest rain streaks and thunder sky)
vec3 renderMonsoon(vec2 p, float t, float aspect) {
    vec2 uvCoord = p + 0.5;
    float rain = smoothstep(0.05, 0.0, abs(fract((uvCoord.x + fbm(p * 3.2) * 0.08 + t * 0.18) * 18.0) - 0.5));
    float streak = smoothstep(0.34, 0.0, abs(fract((uvCoord.y * 1.7 - t * 1.4 + fbm(p * 2.0) * 0.16) * 8.0) - 0.5));
    float clouds = fbm(uvCoord * 4.4 - vec2(t * 0.12, t * 0.05));
    float flash = smoothstep(0.92, 0.99, sin((uvCoord.x + uvCoord.y) * 14.0 - t * 2.8) * 0.5 + 0.5);
    vec3 base = vec3(0.02, 0.03, 0.07);
    vec3 col = mix(base * 0.18, base, clouds);
    col += accent.rgb * (rain * streak * 0.95 + flash * 0.55);
    return clamp(col, 0.0, 1.0);
}

// 14. Bloom (Radiant botanical cherry blossom petals)
vec3 renderBloom(vec2 p, float t, float aspect) {
    float radius = length(p * 2.2);
    float angle = atan(p.y, p.x);
    float petals = smoothstep(0.48, 0.94, sin(angle * 6.0 + fbm(p * 2.4) * 2.0 - t * 0.18) * 0.5 + 0.5);
    petals *= smoothstep(0.92, 0.10, radius);
    float pollen = smoothstep(0.72, 0.95, fbm(p * 5.0 + t * 0.12));
    vec3 base = vec3(0.06, 0.015, 0.04);
    vec3 col = base * (petals * 0.48 + pollen * 0.22);
    col += accent.rgb * (petals * 0.96 + pollen * 0.35);
    return clamp(col, 0.0, 1.0);
}

void main() {
    float aspect = resolution.x / max(resolution.y, 1.0);
    vec2 p = vec2((uv.x - 0.5) * aspect, uv.y - 0.5);
    float t = time.x * max(0.2, time.w);
    float opened = clamp(time.y, 0.0, 1.0);
    int mode = int(time.z + 0.5);

    vec3 colour = vec3(0.0);
    switch (mode) {
        case 0:
            colour = renderCosmicDrift(p, t, aspect);
            break;
        case 1:
            colour = renderPulseNebula(p, t, aspect);
            break;
        case 2:
            colour = renderPulseStarfield(p, t, aspect);
            break;
        case 3:
            colour = renderPulseCobweb(p, t, aspect);
            break;
        case 4:
            colour = renderPulsePlasma(p, t, aspect);
            break;
        case 5:
            colour = renderAurora(p, t, aspect);
            break;
        case 6:
            colour = renderOcean(p, t, aspect);
            break;
        case 7:
            colour = renderToxin(p, t, aspect);
            break;
        case 8:
            colour = renderThunder(p, t, aspect);
            break;
        case 9:
            colour = renderSunset(p, t, aspect);
            break;
        case 10:
            colour = renderVoid(p, t, aspect);
            break;
        case 11:
            colour = renderSolaris(p, t, aspect);
            break;
        case 12:
            colour = renderFractal(p, t, aspect);
            break;
        case 13:
            colour = renderMonsoon(p, t, aspect);
            break;
        case 14:
            colour = renderBloom(p, t, aspect);
            break;
        default:
            colour = renderCosmicDrift(p, t, aspect);
            break;
    }

    // Opening sweep transition
    float wipe = exp(-pow((uv.x + uv.y * 0.25 - opened * 1.6) * 5.0, 2.0));
    colour += accent.rgb * wipe * (1.0 - opened) * 0.55;
    colour *= mix(1.5, 1.0, opened);

    // Ambient cinematic vignette
    float vignette = 1.0 - smoothstep(0.28, 1.08, length(vec2(p.x / max(aspect, 0.001), p.y)) * 1.35);
    colour *= mix(0.42, 1.0, vignette);

    // Gentle dither noise to eliminate banding
    colour += (hash(uv * resolution.xy + fract(t) * 100.0) - 0.5) * 0.016;

    // Darkening and opacity controls
    float darkening = clamp(resolution.w, 0.0, 1.0);
    colour *= (1.0 - darkening * 0.85);

    float alpha = clamp(accent.w, 0.0, 1.0);
    fragColor = vec4(colour, alpha);
}
