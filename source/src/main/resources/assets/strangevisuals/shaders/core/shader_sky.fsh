#version 330

layout(std140) uniform Uniforms {
    vec4 uScreen;
    vec4 uParams;
    vec4 uColorA;
    vec4 uColorB;
};

in vec3 vDirection;
out vec4 fragColor;

const vec3 GROUND = vec3(0.031, 0.035, 0.047);

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 345.45));
    p += dot(p, p + 34.345);
    return fract(p.x * p.y);
}

float vnoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x),
        mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x),
        f.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 5; i++) {
        v += vnoise(p) * a;
        p = p * 2.03 + vec2(7.3, 3.1);
        a *= 0.5;
    }
    return v;
}

vec3 rotateY(vec3 v, float a) {
    float c = cos(a);
    float s = sin(a);
    return vec3(c * v.x - s * v.z, v.y, s * v.x + c * v.z);
}

vec2 dome(vec3 d) {
    return d.xz / (0.35 + max(d.y, 0.0));
}

vec2 sphereUv(vec3 d) {
    return d.xz / (1.0 + clamp(d.y, -0.85, 1.0));
}

float stars(vec3 d, float scale, float thresh, float seed, float t) {
    vec2 g = sphereUv(rotateY(d, t * 0.006)) * scale;
    vec2 i = floor(g);
    vec2 f = fract(g);
    float h = hash(i + seed);
    if (h < thresh) {
        return 0.0;
    }
    vec2 c = vec2(hash(i + seed + 1.7), hash(i + seed + 4.3));
    float dist = length(f - c);
    float twinkle = 0.55 + 0.45 * sin(t * 1.4 + h * 60.0);
    return smoothstep(0.11, 0.0, dist) * twinkle + smoothstep(0.36, 0.0, dist) * 0.16;
}

vec3 veil(vec3 d, float t, float starAmount, float scale) {
    float az = atan(d.z, d.x);
    float alt = d.y;

    vec3 col = GROUND * (0.30 + max(alt, 0.0) * 0.55);
    col += vec3(0.86, 0.90, 1.0) * stars(d, 150.0 * scale, 0.9845, 3.0, t)
        * starAmount * 0.55 * smoothstep(0.0, 0.45, alt);

    vec3 tint = vec3(0.0);
    for (int i = 0; i < 3; i++) {
        float fi = float(i);
        float phase = fi * 2.15;
        float rate = 0.20 + fi * 0.055;
        float foot = 0.16 + fi * 0.105;

        float wave = sin(az * 2.0 + t * rate + phase) * 0.078
                   + sin(az * 3.0 - t * rate * 0.68 + phase * 1.7) * 0.034
                   + sin(az * 1.0 + t * rate * 0.42) * 0.058;
        float band = alt - foot - wave;

        float ray = 0.52 + 0.48 * sin(az * (33.0 + fi * 12.0) + t * (0.55 + fi * 0.22) + phase * 4.0);
        ray *= 0.62 + 0.38 * sin(az * (9.0 + fi * 3.0) - t * 0.28 + phase);

        float body = exp(-max(band, 0.0) * (7.0 - fi * 1.2)) * exp(-max(-band, 0.0) * 40.0);
        float halo = exp(-band * band * 11.0) * 0.22;
        float amount = (body * ray + halo) * (0.60 - fi * 0.125);

        tint += mix(uColorA.rgb, uColorB.rgb, clamp(0.10 + fi * 0.32 + band * 0.85, 0.0, 1.0)) * amount;
    }

    col += tint * 1.25;
    col += uColorA.rgb * 0.05 * exp(-max(alt, 0.0) * 6.5);
    return col;
}

vec3 cells(vec2 x, float t) {
    vec2 n = floor(x);
    vec2 f = fract(x);
    float f1 = 9.0;
    float f2 = 9.0;
    float id = 0.0;
    for (int j = -1; j <= 1; j++) {
        for (int i = -1; i <= 1; i++) {
            vec2 g = vec2(float(i), float(j));
            vec2 o = vec2(hash(n + g), hash(n + g + vec2(31.7, 17.3)));
            vec2 c = 0.5 + 0.40 * sin(t * 0.30 + 6.2831 * o);
            float dist = length(g + c - f);
            if (dist < f1) {
                f2 = f1;
                f1 = dist;
                id = hash(n + g + vec2(5.1, 9.7));
            } else if (dist < f2) {
                f2 = dist;
            }
        }
    }
    return vec3(f1, f2 - f1, id);
}

vec3 facet(vec3 d, float t, float starAmount, float scale) {
    float alt = d.y;
    vec2 p = dome(d) * (1.9 / scale);

    vec3 col = GROUND * (0.42 + max(alt, 0.0) * 0.5);
    col += vec3(0.82, 0.87, 1.0) * stars(d, 175.0 * scale, 0.9885, 11.0, t) * starAmount * 0.5;

    vec3 a = cells(p + vec2(t * 0.012, 0.0), t);
    vec3 b = cells(p * 2.4 - vec2(t * 0.020, 0.0), t * 1.35);

    float edgeA = 1.0 - smoothstep(0.0, 0.052, a.y);
    float edgeB = 1.0 - smoothstep(0.0, 0.070, b.y);
    float sheen = smoothstep(0.62, 0.0, a.x) * 0.10 + smoothstep(0.5, 0.0, b.x) * 0.045;
    float lift = smoothstep(0.0, 0.55, alt);
    float breath = 0.5 + 0.5 * sin(t * 0.22 + a.z * 6.2831);

    col += mix(uColorA.rgb, uColorB.rgb, a.z) * edgeA * 0.85 * lift;
    col += uColorA.rgb * edgeB * 0.26 * lift;
    col += mix(uColorA.rgb, uColorB.rgb, b.z) * sheen * lift;
    col += mix(uColorA.rgb, uColorB.rgb, a.z) * edgeA * breath * 0.32 * lift;
    col += uColorA.rgb * 0.04 * exp(-max(alt, 0.0) * 7.0);
    return col;
}

vec3 inkwell(vec3 d, float t, float starAmount, float scale) {
    float alt = d.y;
    vec2 p = dome(d) * (1.3 / scale);
    float ts = t * 0.075;

    vec2 q = vec2(fbm(p + ts), fbm(p + vec2(5.2, 1.3) - ts * 0.7));
    vec2 r = vec2(fbm(p + 3.3 * q + vec2(1.7, 9.2) + ts * 0.5),
                  fbm(p + 3.3 * q + vec2(8.3, 2.8) - ts * 0.4));
    float f = fbm(p + 3.5 * r);

    vec3 col = mix(GROUND * 0.85, vec3(0.098, 0.107, 0.135), clamp(f * 1.7, 0.0, 1.0));
    col = mix(col, uColorA.rgb * 0.30, clamp(length(q) * 0.62, 0.0, 1.0));
    col = mix(col, uColorB.rgb * 0.24, clamp(r.x * 0.85, 0.0, 1.0));

    float filament = pow(1.0 - abs(fract(f * 7.0) - 0.5) * 2.0, 20.0);
    float hairline = pow(1.0 - abs(fract(f * 14.0 + 0.5) - 0.5) * 2.0, 34.0);
    col += mix(uColorA.rgb, uColorB.rgb, clamp(r.y, 0.0, 1.0)) * filament * 0.80;
    col += uColorA.rgb * hairline * 0.22;

    col += vec3(0.85, 0.89, 1.0) * stars(d, 160.0 * scale, 0.9905, 29.0, t) * starAmount * 0.42;
    col *= 0.55 + 0.55 * smoothstep(0.0, 0.6, alt);
    col += uColorA.rgb * 0.045 * exp(-max(alt, 0.0) * 6.0);
    return col;
}

vec3 halo(vec3 d, float t, float starAmount, float scale) {
    float alt = d.y;
    vec3 a1 = normalize(vec3(0.38 + sin(t * 0.090) * 0.06, 0.60 + sin(t * 0.070) * 0.05, 0.30));
    vec3 a2 = normalize(vec3(-0.44 + sin(t * 0.083 + 2.1) * 0.05, 0.48 + cos(t * 0.061) * 0.06, -0.26));

    float d1 = length(d - a1);
    float d2 = length(d - a2);
    float w1 = sin(d1 * (52.0 / scale) - t * 0.85);
    float w2 = sin(d2 * (48.0 / scale) + t * 0.66);
    float beat = w1 * w2;

    float envelope = exp(-d1 * 1.5) * 0.75 + exp(-d2 * 1.7) * 0.65;
    float line = smoothstep(0.16, 0.0, abs(beat)) * envelope;
    float bloom = exp(-d1 * d1 * 7.0) * 0.16 + exp(-d2 * d2 * 8.5) * 0.12;
    float haze = fbm(dome(d) * 1.4 + vec2(t * 0.014, 0.0)) * 0.10;

    vec3 col = GROUND * (0.38 + max(alt, 0.0) * 0.5);
    col += mix(uColorA.rgb, uColorB.rgb, smoothstep(-0.5, 0.5, d.x)) * line * 0.85;
    col += uColorA.rgb * bloom;
    col += mix(uColorA.rgb, uColorB.rgb, 0.5) * haze * envelope * 0.5;
    col += vec3(0.84, 0.88, 1.0) * stars(d, 185.0 * scale, 0.9915, 41.0, t) * starAmount * 0.45;
    col += uColorA.rgb * 0.04 * exp(-max(alt, 0.0) * 7.0);
    return col;
}

vec3 drift(vec3 d, float t, float starAmount, float scale) {
    float alt = d.y;
    vec3 col = GROUND * (0.55 + max(alt, 0.0) * 0.38);

    col += vec3(0.80, 0.85, 0.98) * stars(d, 72.0 * scale, 0.9700, 1.0, t) * starAmount * 0.42;
    col += vec3(0.92, 0.94, 1.00) * stars(d, 124.0 * scale, 0.9855, 9.0, t) * starAmount * 0.70;
    col += uColorA.rgb * stars(d, 205.0 * scale, 0.9930, 23.0, t) * starAmount * 0.95;

    vec3 axis = normalize(vec3(0.42, 0.60, -0.68));
    float across = dot(rotateY(d, t * 0.006), axis);

    float band = exp(-pow(abs(across) * 7.0, 2.0));

    vec2 p = dome(d) * (1.5 / scale);
    float dust = fbm(p * vec2(1.7, 1.4) + vec2(t * 0.009, 0.0));
    float grit = fbm(p * vec2(3.4, 2.6) - vec2(t * 0.006, 0.0));
    float haze = band * (0.32 + dust * 0.95) * (0.60 + grit * 0.55);

    col += mix(vec3(0.90, 0.93, 1.0), uColorA.rgb, 0.35) * haze * 0.30;
    col *= 1.0 - band * smoothstep(0.48, 0.82, dust) * 0.55;
    col += uColorA.rgb * 0.035 * exp(-max(alt, 0.0) * 8.0);
    return col;
}

float segment(vec2 p, vec2 a, vec2 b) {
    vec2 pa = p - a;
    vec2 ba = b - a;
    float h = clamp(dot(pa, ba) / max(dot(ba, ba), 1e-5), 0.0, 1.0);
    return length(pa - ba * h);
}

vec3 cirrus(vec3 d, float t, float starAmount, float scale) {
    float alt = d.y;
    vec2 p = dome(d) * (1.0 / scale);
    vec2 combed = vec2(p.x * 0.30, p.y * 2.4);

    float drift = fbm(combed + vec2(t * 0.020, 0.0));
    float ridge = 1.0 - abs(fbm(combed * 2.1 + drift * 1.3 + vec2(t * 0.030, 0.0)) * 2.0 - 1.0);
    float veilAmount = smoothstep(0.42, 0.97, ridge * (0.55 + drift * 0.85));
    float wisp = smoothstep(0.60, 1.0, ridge) * 0.5;

    vec3 col = GROUND * (0.38 + max(alt, 0.0) * 0.55);
    col += vec3(0.86, 0.90, 1.0) * stars(d, 165.0 * scale, 0.9880, 53.0, t) * starAmount * 0.55;
    col *= 1.0 - veilAmount * 0.45;
    col += mix(uColorA.rgb, uColorB.rgb, drift * 0.7) * veilAmount * 0.42;
    col += uColorA.rgb * wisp * 0.30;
    col += uColorA.rgb * 0.07 * exp(-max(alt, 0.0) * 4.5) * (0.5 + veilAmount);
    return col;
}

vec3 rift(vec3 d, float t, float starAmount, float scale) {
    float alt = d.y;
    vec3 axis = normalize(vec3(0.52, 0.34, -0.78));
    vec2 p = dome(d);

    float wander = (fbm(p * (2.2 / scale) + vec2(t * 0.020, 0.0)) - 0.5) * 0.22;
    float detail = (fbm(p * (6.5 / scale) - vec2(t * 0.035, 0.0)) - 0.5) * 0.055;
    float dist = abs(dot(d, axis) + wander + detail);

    float core = exp(-dist * dist * 2600.0);
    float lip = exp(-dist * dist * 260.0) * 0.45;
    float bleed = exp(-dist * dist * 22.0) * 0.30;
    float shimmer = 0.75 + 0.25 * sin(dot(p, vec2(9.0, 7.0)) + t * 1.6);

    vec3 col = GROUND * (0.34 + max(alt, 0.0) * 0.5);
    col += vec3(0.84, 0.88, 1.0) * stars(d, 175.0 * scale, 0.9900, 61.0, t) * starAmount * 0.5;
    col += uColorB.rgb * bleed;
    col += mix(uColorA.rgb, uColorB.rgb, 0.35) * lip * shimmer;
    col += mix(uColorA.rgb, vec3(1.0), 0.55) * core * 1.4;
    col += uColorA.rgb * 0.04 * exp(-max(alt, 0.0) * 7.0);
    return col;
}

vec3 lattice(vec3 d, float t, float starAmount, float scale) {
    float alt = d.y;
    vec2 g = sphereUv(rotateY(d, t * 0.010)) * (7.5 * scale);
    vec2 n = floor(g);
    vec2 f = fract(g);

    vec2 home = vec2(hash(n), hash(n + vec2(11.3, 7.7)));
    home = 0.28 + 0.44 * home;

    float web = 0.0;
    float nodes = 0.0;
    for (int j = -1; j <= 1; j++) {
        for (int i = -1; i <= 1; i++) {
            vec2 o = vec2(float(i), float(j));
            vec2 site = o + 0.28 + 0.44 * vec2(hash(n + o), hash(n + o + vec2(11.3, 7.7)));
            nodes += smoothstep(0.052, 0.0, length(f - site));
            if (i == 0 && j == 0) {
                continue;
            }
            if (hash(n + o * 0.5 + vec2(3.3, 8.1)) < 0.42) {
                continue;
            }
            web += smoothstep(0.017, 0.0, segment(f, home, site));
        }
    }

    float pulse = 0.6 + 0.4 * sin(t * 0.5 + hash(n) * 6.2831);
    float lift = smoothstep(-0.05, 0.4, alt);

    vec3 col = GROUND * (0.44 + max(alt, 0.0) * 0.45);
    col += mix(uColorA.rgb, uColorB.rgb, hash(n + 2.5)) * web * 0.34 * lift;
    col += vec3(0.92, 0.95, 1.0) * clamp(nodes, 0.0, 1.0) * pulse * (0.35 + starAmount * 0.85) * lift;
    col += uColorA.rgb * stars(d, 190.0 * scale, 0.9925, 71.0, t) * starAmount * 0.5;
    col += uColorA.rgb * 0.035 * exp(-max(alt, 0.0) * 7.5);
    return col;
}

vec3 tide(vec3 d, float t, float starAmount, float scale) {
    float alt = d.y;
    vec2 p = dome(d) * (1.15 / scale);

    float warp = fbm(p * 0.85 + vec2(t * 0.026, t * 0.012)) * 1.9;
    float fine = fbm(p * 2.3 - vec2(t * 0.018, 0.0)) * 0.7;
    float phase = p.y * 1.7 + warp + fine;

    float bands = sin(phase * 5.5 + t * 0.22);
    float thin = sin(phase * 13.0 - t * 0.16);
    float sheen = 0.5 + 0.5 * sin(phase * 2.3 + t * 0.11);

    float body = pow(0.5 + 0.5 * bands, 2.4);
    float edge = pow(1.0 - abs(thin), 14.0);

    vec3 col = GROUND * (0.40 + max(alt, 0.0) * 0.5);
    col += vec3(0.84, 0.88, 1.0) * stars(d, 170.0 * scale, 0.9910, 83.0, t) * starAmount * 0.42;
    col += mix(uColorA.rgb, uColorB.rgb, sheen) * body * 0.30;
    col += mix(uColorB.rgb, uColorA.rgb, sheen) * edge * 0.55;
    col *= 0.6 + 0.5 * smoothstep(-0.1, 0.5, alt);
    col += uColorA.rgb * 0.045 * exp(-max(alt, 0.0) * 6.0);
    return col;
}

vec3 nebula(vec3 d, float t, float starAmount, float scale) {
    float alt = d.y;
    vec2 p = dome(d) * (0.42 / scale);

    float base = fbm(p * 0.9 + vec2(t * 0.008, 0.0));
    float ridge = 1.0 - abs(fbm(p * 1.8 + base * 2.2 - vec2(t * 0.005, 0.0)) * 2.0 - 1.0);
    float lanes = fbm(p * 2.4 + vec2(9.1, 4.3) - vec2(t * 0.004, 0.0));

    vec3 core = normalize(vec3(0.34, 0.58, -0.46));
    float toCore = length(d - core);
    float glow = exp(-toCore * toCore * 2.4);

    float density = pow(clamp(ridge * 1.5 + base * 0.5 - 0.62, 0.0, 1.0), 1.25);
    density *= 1.0 - smoothstep(0.42, 0.72, lanes) * 0.85;
    float rim = pow(clamp(ridge - 0.72, 0.0, 1.0), 0.8) * 3.4;

    vec3 col = GROUND * (0.34 + max(alt, 0.0) * 0.34);
    col += vec3(0.88, 0.92, 1.0) * stars(d, 95.0 * scale, 0.9955, 97.0, t) * starAmount * 0.85;
    col += mix(uColorB.rgb * 0.55, uColorA.rgb, base) * density * (0.55 + glow * 1.5);
    col += mix(uColorA.rgb, vec3(1.0), 0.45) * rim * density * (0.35 + glow * 1.3);
    col += uColorA.rgb * glow * 0.16;
    return col;
}

vec3 corona(vec3 d, float t, float starAmount, float scale) {
    float alt = d.y;
    vec2 p = dome(d) * (1.0 / scale);
    float r = length(p);
    float a = atan(p.y, p.x);

    float churn = fbm(p * 0.75 + vec2(t * 0.030, t * 0.018));
    float detail = fbm(p * 1.9 - vec2(t * 0.022, 0.0));

    float rays = 0.42 + 0.58 * sin(a * 13.0 + churn * 6.5 + t * 0.16);
    rays *= 0.55 + 0.45 * sin(a * 5.0 - churn * 3.0 - t * 0.09);

    float sheets = fbm(vec2(cos(a) * r * 1.3, sin(a) * r * 1.3) + vec2(t * 0.02, -t * 0.014));
    float shell = exp(-pow(max(r - 0.55, 0.0) * 1.05, 2.0));
    float hole = smoothstep(0.0, 0.42, r);

    float body = shell * hole * (0.35 + sheets * 1.25) * (0.45 + rays * 0.85);
    float veins = pow(clamp(rays * sheets * 1.6 - 0.45, 0.0, 1.0), 1.4) * shell * hole;

    vec3 col = GROUND * (0.28 + max(alt, 0.0) * 0.4);
    col += vec3(0.86, 0.90, 1.0) * stars(d, 140.0 * scale, 0.9900, 113.0, t) * starAmount * 0.5;
    col += mix(uColorA.rgb, uColorB.rgb, clamp(detail * 1.2, 0.0, 1.0)) * body * 0.62;
    col += mix(uColorA.rgb, vec3(1.0), 0.35) * veins * 0.75;
    col += uColorA.rgb * 0.05 * exp(-max(alt, 0.0) * 5.0);
    return col;
}

void main() {
    vec3 dir = normalize(vDirection);

    float t = uScreen.z;
    float intensity = uParams.x;
    float starAmount = uParams.y;
    int style = int(uParams.z + 0.5);
    float scale = max(uParams.w, 0.05);

    vec3 col;
    if (style == 1) {
        col = facet(dir, t, starAmount, scale);
    } else if (style == 2) {
        col = inkwell(dir, t, starAmount, scale);
    } else if (style == 3) {
        col = halo(dir, t, starAmount, scale);
    } else if (style == 4) {
        col = drift(dir, t, starAmount, scale);
    } else if (style == 5) {
        col = cirrus(dir, t, starAmount, scale);
    } else if (style == 6) {
        col = rift(dir, t, starAmount, scale);
    } else if (style == 7) {
        col = lattice(dir, t, starAmount, scale);
    } else if (style == 8) {
        col = tide(dir, t, starAmount, scale);
    } else if (style == 9) {
        col = nebula(dir, t, starAmount, scale);
    } else if (style == 10) {
        col = corona(dir, t, starAmount, scale);
    } else {
        col = veil(dir, t, starAmount, scale);
    }

    float below = smoothstep(0.035, -0.055, dir.y);
    vec3 floorTone = GROUND * 0.75 + uColorA.rgb * 0.020;
    col = mix(col, floorTone, below);

    fragColor = vec4(col, clamp(intensity, 0.0, 1.0));
}
